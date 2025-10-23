package no.nav.pto.veilarbportefolje.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.types.identer.AktorId
import no.nav.common.utils.EnvironmentUtils
import no.nav.pto.veilarbportefolje.auth.AuthUtils
import no.nav.pto.veilarbportefolje.auth.DownstreamApi
import no.nav.pto.veilarbportefolje.ensligforsorger.EnsligeForsorgereService
import no.nav.pto.veilarbportefolje.persononinfo.PdlService
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException


@Slf4j
@RestController
@RequestMapping("/api/admin/hentData")
@RequiredArgsConstructor
@Tag(
    name = "Admin for datahenting",
    description = "Admin-funksjonalitet for å hente data i batchjobber og for enkeltbrukere som ikke er tilgjengelig for andre utenfor forvaltere."
)

class AdminControllerDataHenting(
    private val authContextHolder: AuthContextHolder,
    private val pdlService: PdlService,
    private val ensligForsorgerService: EnsligeForsorgereService
) {
    private val POAO_ADMIN = DownstreamApi(
        if (EnvironmentUtils.isProduction().orElse(false)) "prod-gcp" else "dev-gcp",
        "poao",
        "poao-admin"
    ).toString()

    @GetMapping("hentDataForBruker/muligeValg")
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

    @PostMapping("hentDataForBruker/forValgte")
    @Operation(
        summary = "Hent data for bruker basert på valg",
        description = "Henter spesifikk type data for en bruker basert på angitte valg."
    )
    fun hentDataForBrukerForAngitteValg(@RequestBody request: HentDataForBrukerRequest): ResponseEntity<String> {
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

    private fun hentOvergangsstønadData(aktorId: AktorId) {
        secureLog.info("Starter datahenting for overgangsstønad for aktorId {}", aktorId)
        ensligForsorgerService.hentOgLagreEnsligForsorgerDataVedAdminjobb(aktorId)
    }

    private fun hentPdlData(aktorId: AktorId) {
        secureLog.info("Starter datahenting for PDL for aktorId {}", aktorId)
        pdlService.hentOgLagrePdlData(aktorId)
    }

    private fun sjekkTilgangTilAdmin() {
        val erInternBrukerFraAzure = authContextHolder.erInternBruker()
        val erPoaoAdmin = POAO_ADMIN == AuthUtils.hentApplikasjonFraContex(authContextHolder)

        if (erPoaoAdmin && erInternBrukerFraAzure) {
            return
        }
        throw ResponseStatusException(HttpStatus.FORBIDDEN)
    }
}
