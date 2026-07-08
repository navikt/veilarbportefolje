package no.nav.pto.veilarbportefolje.admin;

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.job.JobRunner
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.EnvironmentUtils
import no.nav.pto.veilarbportefolje.aap.AapService
import no.nav.pto.veilarbportefolje.admin.dto.*
import no.nav.pto.veilarbportefolje.auth.AuthUtils.hentApplikasjonFraContex
import no.nav.pto.veilarbportefolje.auth.DownstreamApi
import no.nav.pto.veilarbportefolje.client.AktorClient
import no.nav.pto.veilarbportefolje.ensligforsorger.EnsligeForsorgereService
import no.nav.pto.veilarbportefolje.opensearch.HovedIndekserer
import no.nav.pto.veilarbportefolje.opensearch.OpensearchAdminService
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingClient
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.oppfolging.domene.Veilarbportefoljeinfo
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.PdlService
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.concurrent.atomic.AtomicInteger

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(
    name = "Admin",
    description = "Admin-funksjonalitet som ikke er tilgjengelig for vanlige brukere. Funksjonaliteten er kun tilgjengelig for medlemmer av applikasjonens forvaltningsteam."
)
class AdminController(
    private val aktorClient: AktorClient,
    private val hovedIndekserer: HovedIndekserer,
    private val opensearchIndexer: OpensearchIndexer,
    private val authContextHolder: AuthContextHolder,
    private val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    private val opensearchAdminService: OpensearchAdminService,
    private val pdlService: PdlService,
    private val pdlIdentRepository: PdlIdentRepository,
    private val ensligForsorgerService: EnsligeForsorgereService,
    private val aapService: AapService,
    private val oppfolgingClient: OppfolgingClient
) {
    private val POAO_ADMIN = DownstreamApi(
        if (EnvironmentUtils.isProduction().orElse(false)) "prod-gcp" else "dev-gcp", "poao", "poao-admin"
    ).toString()
    private val log = org.slf4j.LoggerFactory.getLogger(AdminController::class.java)

    // INDEKSERINGSJOBBER
    @Operation(
        summary = "Indekser bruker med fødselsnummer",
        description = "Hent og skriv oppdatert data for bruker, gitt ved fødselsnummer, til søkemotoren (OpenSearch)."
    )
    @PutMapping("/indeks/bruker/fnr")
    fun indeks(@RequestBody adminFnrRequest: AdminFnrRequest): String {
        sjekkTilgangTilAdmin()
        val aktorId = aktorClient.hentAktorId(Fnr.ofValidFnr(adminFnrRequest.fnr.get()))
        opensearchIndexer.indekser(aktorId)
        return "Indeksering fullfort"
    }

    @Operation(
        summary = "Indekser bruker med Aktør-ID",
        description = "Hent og skriv oppdatert data for bruker, gitt ved Aktør-ID, til søkemotoren (OpenSearch)."
    )
    @PutMapping("/indeks/bruker")
    fun indeksAktoerId(@RequestBody adminAktorIdRequest: AdminAktorIdRequest): String {
        sjekkTilgangTilAdmin()
        opensearchIndexer.indekser(adminAktorIdRequest.aktorId)
        return "Indeksering fullført"
    }

    @PostMapping("/indeks/AlleBrukere")
    @Operation(
        summary = "Indekser alle brukere",
        description = "Går gjennom alle brukere i løsningen og oppdaterer data om disse i søkemotoren (OpenSearch)."
    )
    fun indekserAlleBrukere(): String {
        sjekkTilgangTilAdmin()
        return JobRunner.runAsync("Admin_hovedindeksering") {
            val brukereUnderOppfolging = oppfolgingRepositoryV2.hentAlleGyldigeBrukereUnderOppfolging()
            opensearchIndexer.oppdaterAlleBrukereIOpensearch(brukereUnderOppfolging)
        }
    }

    @PostMapping("/indeks/AlleBrukereNyIndex")
    @Operation(
        summary = "Indekser alle brukere på ny index",
        description = "Går gjennom alle brukere i løsningen og oppdaterer data om disse i søkemotoren (OpenSearch) på en ny indeks."
    )
    fun indekserAlleBrukereNyIndex(): String {
        sjekkTilgangTilAdmin()
        return JobRunner.runAsync("Admin_hovedindeksering_ny_index") {
            val brukereUnderOppfolging = oppfolgingRepositoryV2.hentAlleGyldigeBrukereUnderOppfolging()
            hovedIndekserer.aliasBasertHovedIndeksering(brukereUnderOppfolging)
        }
    }

    @PostMapping("/opensearch/createIndex")
    @Operation(summary = "Opprett ny indeks", description = "Oppretter en ny indeks i søkemotoren (OpenSearch).")
    fun createIndex(): String {
        sjekkTilgangTilAdmin()
        val indexName = opensearchAdminService.opprettNyIndeks()
        log.info("Opprettet index: {}", indexName)
        return indexName
    }

    @GetMapping("/opensearch/getAliases")
    @Operation(
        summary = "Hent alle aliaser",
        description = "Henter alle aliaser som eksisterer i søkemotoren (OpenSearch)."
    )
    fun getAliases(): String {
        sjekkTilgangTilAdmin()
        return opensearchAdminService.hentAliaser()
    }

    @PostMapping("/opensearch/deleteIndex")
    @Operation(summary = "Slett indeks", description = "Sletter en indeks i søkemotoren (OpenSearch).")
    fun deleteIndex(@RequestParam indexName: String): Boolean {
        sjekkTilgangTilAdmin()
        log.info("Sletter index: {}", indexName)
        return opensearchAdminService.slettIndex(indexName)
    }

    @PostMapping("/opensearch/assignAliasToIndex")
    @Operation(
        summary = "Tildel alias til indeks",
        description = "Tildeler et alias til en indeks i søkemotoren (OpenSearch)."
    )
    fun assignAliasToIndex(@RequestParam indexName: String): String {
        sjekkTilgangTilAdmin()
        opensearchAdminService.opprettAliasForIndeks(indexName)
        return "Ok"
    }

    // SJEKK OM VI HAR BERØRTE AKTØRIDER I TILFELLER AV MERGE/SPLIT
    @PostMapping("/aktoridSjekk")
    @Operation(
        summary = "Sjekk om vi har en aktørid i bruker_ident tabellen",
        description = "Sjekker om vi har aktørider i bruker_ident våre." +
                "Dette er i tilfeller ved merge/split og for å sjekke om vi er berørt."
    )
    fun sjekkOmViHarAktorId(@RequestBody adminAktorIdRequest: AdminAktorIdRequest): Boolean {
        sjekkTilgangTilAdmin()
        val responsPortefolje = pdlIdentRepository.hentPerson(adminAktorIdRequest.aktorId.get())
        return !responsPortefolje.isNullOrEmpty()
    }

    // DATA FETCHING JOBBER - BATCH
    @PostMapping("/pdl/lastInnDataFraPdl")
    @Operation(
        summary = "Last inn PDL-data",
        description = "Henter og lagrer data fra PDL (identer, personalia og foreldreansvar) for alle brukere i løsningen."
    )
    fun lastInnPDLBrukerData(): String {
        sjekkTilgangTilAdmin()

        val antall = AtomicInteger(0)
        val brukereUnderOppfolging = oppfolgingRepositoryV2.hentAlleGyldigeBrukereUnderOppfolging()

        brukereUnderOppfolging.forEach { bruker ->
            if (antall.getAndAdd(1) % 100 == 0) {
                log.info(
                    "pdl brukerdata: innlasting {}% ferdig",
                    (antall.get().toDouble() / brukereUnderOppfolging.size.toDouble()) * 100.0
                )
            }
            try {
                pdlService.hentOgLagrePdlData(bruker)
            } catch (e: Exception) {
                secureLog.info("pdl brukerdata: feil under innlasting av pdl-data for bruker: {}", bruker, e)
            }
        }

        log.info("pdl brukerdata: ferdig med innlasting")
        return "ferdig"
    }


    @PostMapping("/hentEnsligForsorgerDataBatch")
    @Operation(
        summary = "Henter data om enslig forsorger for alle brukere",
        description = "Sjekker om bruker er enslig forsørger og henter data for alle brukere"
    )
    fun hentEnsligForsorgerBruker(): ResponseEntity<String> {
        sjekkTilgangTilAdmin()

        val brukereUnderOppfolging = oppfolgingRepositoryV2.hentAlleGyldigeBrukereUnderOppfolging()
        val antall = AtomicInteger(0)

        log.info("Startet: Innlasting av Ensligforsørger brukerdata")

        brukereUnderOppfolging.forEach { aktorId ->
            if (antall.getAndAdd(1) % 100 == 0) {
                log.info(
                    "Ensligforsørger brukerdata: innlasting {}% ferdig",
                    (antall.get().toDouble() / brukereUnderOppfolging.size.toDouble()) * 100.0
                )
            }
            try {
                ensligForsorgerService.hentOgLagreEnsligForsorgerDataVedAdminjobb(aktorId)
            } catch (e: Exception) {
                secureLog.error("Feil under innlasting av ensligforsørger-data for aktorId $aktorId")
            }
        }

        log.info("Ferdig: Innlasting av ensligforsørger brukerdata")
        return ResponseEntity.ok("Innlasting av EnsligForsørger-data fullført")
    }

    @PostMapping("/lastInnTildelingsdatoForBrukere")
    @Operation(
        summary = "Oppdater tilordningsdato for alle brukere",
        description = "Går gjennom alle brukere med tildelt veileder i løsningen og oppdaterer tilordningsdato for disse."
    )
    fun lastInnTildelingstidspunktForVeileder(
        @RequestParam(required = false) limit: Int? = null
    ): String {
        sjekkTilgangTilAdmin()
        val alleBrukereUnderOppfolging = oppfolgingRepositoryV2.hentAlleBrukerUnderOppfolgingMedTildeltVeileder()
        val brukereUnderOppfolging =
            if (limit != null) alleBrukereUnderOppfolging.take(limit) else alleBrukereUnderOppfolging
        log.info("Tilordningsdato : prosesserer ${brukereUnderOppfolging.size} av ${alleBrukereUnderOppfolging.size} brukere")
        val antall = AtomicInteger(0)

        brukereUnderOppfolging.forEach { aktorId ->
            if (antall.getAndAdd(1) % 100 == 0) {
                log.info(
                    "Tilordningsdato brukerdata: innlasting {}% ferdig",
                    (antall.get().toDouble() / brukereUnderOppfolging.size.toDouble()) * 100.0
                )
            }
            try {
                val veilarbInfo: Veilarbportefoljeinfo = oppfolgingClient.hentVeilarbData(aktorId)
                secureLog.info("Tilordningsdato : Starter prosessering for nr $antall med aktorId $aktorId")

                if (veilarbInfo.erUnderOppfolging && veilarbInfo.tilordnetTidspunkt != null
                ) {
                    oppfolgingRepositoryV2.settTildeltTidspunkt(
                        aktorId,
                        veilarbInfo.tilordnetTidspunkt
                    )
                    secureLog.info("Tilordningsdato : dato ble oppdatert i databasen for nr $antall med aktorId $aktorId")
                } else {
                    secureLog.warn(
                        "Tilordningsdato : blir ikke lagret fordi aktorId $aktorId har fra clientet at tilordnettidspunkt=${veilarbInfo.tilordnetTidspunkt} " +
                                "og erUnderOppfolging=${veilarbInfo.erUnderOppfolging}"
                    )
                }
            } catch (e: Exception) {
                secureLog.error(
                    "Tilordningsdato : Exception i OppfolgingsJobb tildelingstidspunkt for bruker $aktorId",
                    e
                )
            }
            Thread.sleep(50) // throttle: ~20 req/s mot veilarboppfolging

        }
        return "Innlastning av tilordningsdato for veileder har startet"
    }

    // DATA FETCHING JOBBER - FOR EN ENKELTBRUKER
    @GetMapping("hentData/hentDataForBruker/muligeValg")
    @Operation(
        summary = "Henter mulige valg for datahenting",
        description = "Henter en liste over mulige datatyper som kan hentes."
    )
    fun hentDataForBrukerMuligeValg(): ResponseEntity<List<AdminDataTypeResponse>> {
        sjekkTilgangTilAdmin()
        val muligeValg = AdminDataType.entries.map { type ->
            AdminDataTypeResponse(name = type.name, displayName = type.displayName)
        }
        return ResponseEntity.ok(muligeValg)
    }


    @PostMapping("hentData/hentDataForBruker/forValgte")
    @Operation(
        summary = "Hent data for bruker basert på valg",
        description = "Henter spesifikk type data for en bruker basert på angitte valg."
    )
    fun hentDataForBrukerForAngitteValg(@RequestBody request: AdminDataForBrukerRequest): ResponseEntity<String> {
        sjekkTilgangTilAdmin()
        if (request.valg.isEmpty()) {
            return ResponseEntity.badRequest().body("Ingen valg er angitt for datahenting")
        }

        val feiledeValg = mutableListOf<String>()
        request.valg.forEach { type ->
            try {
                when (type) {
                    AdminDataType.PDL_DATA -> hentPdlData(request.aktorId)
                    AdminDataType.ENSLIG_FORSORGER_DATA -> hentOvergangsstønadData(request.aktorId)
                    AdminDataType.AAP_DATA -> hentAapData(request.aktorId)
                }
            } catch (e: Exception) {
                secureLog.error("Feil ved henting av ${type.name} for aktorId ${request.aktorId}", e)
                feiledeValg.add(type.name)
            }
        }

        return if (feiledeValg.isEmpty()) {
            ResponseEntity.ok("Alle datahentinger fullført uten feil")
        } else {
            ResponseEntity.internalServerError().body("Feil ved: ${feiledeValg.joinToString(", ")}")
        }
    }

    fun hentOpplysningMetadataForOppfolgingsbruker()

    private fun hentOvergangsstønadData(aktorId: AktorId) {
        secureLog.info("Starter datahenting for overgangsstønad for aktorId {}", aktorId)
        ensligForsorgerService.hentOgLagreEnsligForsorgerDataVedAdminjobb(aktorId)
    }

    private fun hentPdlData(aktorId: AktorId) {
        secureLog.info("Starter datahenting for PDL for aktorId {}", aktorId)
        pdlService.hentOgLagrePdlData(aktorId)
    }

    private fun hentAapData(aktorId: AktorId) {
        secureLog.info("Starter datahenting for AAP for aktorId {}", aktorId)
        aapService.hentOgLagreAapForBrukerVedOppfolgingStart(aktorId)
    }

    private fun sjekkTilgangTilAdmin() {
        val erInternBrukerFraAzure = authContextHolder.erInternBruker()
        val erPoaoAdmin = POAO_ADMIN == hentApplikasjonFraContex(authContextHolder)

        if (erPoaoAdmin && erInternBrukerFraAzure) return

        throw ResponseStatusException(HttpStatus.FORBIDDEN)
    }

}
