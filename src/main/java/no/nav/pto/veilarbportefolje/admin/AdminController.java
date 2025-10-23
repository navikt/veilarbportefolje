package no.nav.pto.veilarbportefolje.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.job.JobRunner;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesService;
import no.nav.pto.veilarbportefolje.auth.DownstreamApi;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.ensligforsorger.EnsligeForsorgereService;
import no.nav.pto.veilarbportefolje.opensearch.HovedIndekserer;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchAdminService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingAvsluttetService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.persononinfo.PdlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static no.nav.pto.veilarbportefolje.auth.AuthUtils.hentApplikasjonFraContex;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-funksjonalitet som ikke er tilgjengelig for vanlige brukere. Funksjonaliteten er kun tilgjengelig for medlemmer av applikasjonens forvaltningsteam.")
public class AdminController {
    private final String POAO_ADMIN = new DownstreamApi(EnvironmentUtils.isProduction().orElse(false) ?
            "prod-gcp" : "dev-gcp", "poao", "poao-admin").toString();
    private final AktorClient aktorClient;
    private final OppfolgingAvsluttetService oppfolgingAvsluttetService;
    private final HovedIndekserer hovedIndekserer;
    private final OpensearchIndexer opensearchIndexer;
    private final AuthContextHolder authContextHolder;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final OpensearchAdminService opensearchAdminService;
    private final PdlService pdlService;
    private final EnsligeForsorgereService ensligForsorgerService;

    // denne brukes heller ikke fra pto-admin
    @DeleteMapping("/oppfolgingsbruker")
    @Operation(summary = "Fjern bruker", description = "Sletter en bruker og fjerner tilhørende informasjon om brukeren. Brukeren vil ikke lenger eksistere i porteføljene.")
    public String slettOppfolgingsbruker(@RequestBody AdminAktorIdRequest request) {
        sjekkTilgangTilAdmin();
        oppfolgingAvsluttetService.avsluttOppfolging(AktorId.of(request.aktorId().get()));
        return "Oppfølgingsbruker ble slettet";
    }

    @Operation(summary = "Indekser bruker med fødselsnummer", description = "Hent og skriv oppdatert data for bruker, gitt ved fødselsnummer, til søkemotoren (OpenSearch).")
    @PutMapping("/indeks/bruker/fnr")
    public String indeks(@RequestBody AdminFnrRequest adminFnrRequest) {
        sjekkTilgangTilAdmin();
        String aktorId = aktorClient.hentAktorId(Fnr.ofValidFnr(adminFnrRequest.fnr().get())).get();
        opensearchIndexer.indekser(AktorId.of(aktorId));
        return "Indeksering fullfort";
    }

    @Operation(summary = "Indekser bruker med Aktør-ID", description = "Hent og skriv oppdatert data for bruker, gitt ved Aktør-ID, til søkemotoren (OpenSearch).")
    @PutMapping("/indeks/bruker")
    public String indeksAktoerId(@RequestBody AdminAktorIdRequest adminAktorIdRequest) {
        sjekkTilgangTilAdmin();
        opensearchIndexer.indekser(adminAktorIdRequest.aktorId());
        return "Indeksering fullfort";
    }

    @PostMapping("/indeks/AlleBrukere")
    @Operation(summary = "Indekser alle brukere", description = "Går gjennom alle brukere i løsningen og oppdaterer data om disse i søkemotoren (OpenSearch).")
    public String indekserAlleBrukere() {
        sjekkTilgangTilAdmin();
        return JobRunner.runAsync("Admin_hovedindeksering", () -> {
                    List<AktorId> brukereUnderOppfolging = oppfolgingRepositoryV2.hentAlleGyldigeBrukereUnderOppfolging();
                    opensearchIndexer.oppdaterAlleBrukereIOpensearch(brukereUnderOppfolging);
                }
        );
    }

    @PostMapping("/indeks/AlleBrukereNyIndex")
    @Operation(summary = "Indekser alle brukere på ny index", description = "Går gjennom alle brukere i løsningen og oppdaterer data om disse i søkemotoren (OpenSearch) på en ny indeks.")
    public String indekserAlleBrukereNyIndex() {
        sjekkTilgangTilAdmin();
        return JobRunner.runAsync("Admin_hovedindeksering_ny_index", () -> {
                    List<AktorId> brukereUnderOppfolging = oppfolgingRepositoryV2.hentAlleGyldigeBrukereUnderOppfolging();
                    hovedIndekserer.aliasBasertHovedIndeksering(brukereUnderOppfolging);
                }
        );
    }

    @PostMapping("/opensearch/createIndex")
    @Operation(summary = "Opprett ny indeks", description = "Oppretter en ny indeks i søkemotoren (OpenSearch).")
    public String createIndex() {
        sjekkTilgangTilAdmin();
        String indexName = opensearchAdminService.opprettNyIndeks();
        log.info("Opprettet index: {}", indexName);
        return indexName;
    }

    @GetMapping("/opensearch/getAliases")
    @Operation(summary = "Hent alle aliaser", description = "Henter alle aliaser som eksisterer i søkemotoren (OpenSearch).")
    public String getAliases() {
        sjekkTilgangTilAdmin();
        return opensearchAdminService.hentAliaser();
    }

    @PostMapping("/opensearch/deleteIndex")
    @Operation(summary = "Slett indeks", description = "Sletter en indeks i søkemotoren (OpenSearch).")
    public boolean deleteIndex(@RequestParam String indexName) {
        sjekkTilgangTilAdmin();
        log.info("Sletter index: {}", indexName);
        return opensearchAdminService.slettIndex(indexName);
    }

    @PostMapping("/opensearch/assignAliasToIndex")
    @Operation(summary = "Tildel alias til indeks", description = "Tildeler et alias til en indeks i søkemotoren (OpenSearch).")
    public String assignAliasToIndex(@RequestParam String indexName) {
        sjekkTilgangTilAdmin();
        opensearchAdminService.opprettAliasForIndeks(indexName);
        return "Ok";
    }


    // er også en type batch jobb, kan vurderes å generalisere med resten.
    @PostMapping("/pdl/lastInnDataFraPdl")
    @Operation(summary = "Last inn PDL-data", description = "Henter og lagrer data fra PDL (identer, personalia og foreldreansvar) for alle brukere i løsningen.")
    public String lastInnPDLBrukerData() {
        sjekkTilgangTilAdmin();
        AtomicInteger antall = new AtomicInteger(0);
        List<AktorId> brukereUnderOppfolging = oppfolgingRepositoryV2.hentAlleGyldigeBrukereUnderOppfolging();
        brukereUnderOppfolging.forEach(bruker -> {
            if (antall.getAndAdd(1) % 100 == 0) {
                log.info("pdl brukerdata: inlastning {}% ferdig", ((double) antall.get() / (double) brukereUnderOppfolging.size()) * 100.0);
            }
            try {
                pdlService.hentOgLagrePdlData(bruker);
            } catch (Exception e) {
                secureLog.info("pdl brukerdata: feil under innlastning av pdl data på bruker: {}", bruker, e);
            }
        });
        log.info("pdl brukerdata: ferdig med innlastning");
        return "ferdig";
    }

    @PutMapping("/pdl/lastInnDataFraPdl")
    @Deprecated(forRemoval = true)
    public String lastInnPDLBrukerData(@RequestParam String fnr) {
        sjekkTilgangTilAdmin();
        String aktorId = aktorClient.hentAktorId(Fnr.ofValidFnr(fnr)).get();
        try {
            pdlService.hentOgLagrePdlData(AktorId.of(aktorId));
        } catch (Exception e) {
            secureLog.info("pdl brukerdata: feil under innlastning av pdl data på bruker: {}", aktorId, e);
        }
        log.info("pdl brukerdata: ferdig med innlastning");
        return "ferdig";
    }

    @PostMapping("/hentEnsligForsorgerData")
    @Operation(summary = "Henter data om enslig forsorger", description = "Sjekker om bruker er enslig forsorger og henter data om det")
    public ResponseEntity<String> hentEnsligForsorgerBruker(@RequestBody AdminAktorIdRequest adminAktorIdRequest) {
        sjekkTilgangTilAdmin();
        try {
            ensligForsorgerService.hentOgLagreEnsligForsorgerDataVedAdminjobb(adminAktorIdRequest.aktorId());
            return ResponseEntity.ok("Innlastning av Ensligforsørger brukerdata er ferdig");
        } catch (Exception e) {
            secureLog.info("Ensligforsørger brukerdata: feil under innlastning av data på bruker: {}", adminAktorIdRequest.aktorId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Feil under innlasting av Ensligforsørger-data");
        }
    }

    @PostMapping("/hentEnsligForsorgerDataBatch")
    @Operation(summary = "Henter data om enslig forsorger for alle brukere", description = "Sjekker om bruker er enslig forsørger og henter data for alle brukere")
    public ResponseEntity<String> hentEnsligForsorgerBruker() {
        sjekkTilgangTilAdmin();
        List<AktorId> brukereUnderOppfolging = oppfolgingRepositoryV2.hentAlleGyldigeBrukereUnderOppfolging();

        log.info("Startet: Innlastning av Ensligforsørger brukerdata");
        brukereUnderOppfolging.forEach(aktorId -> {
            try {
                ensligForsorgerService.hentOgLagreEnsligForsorgerDataVedAdminjobb(aktorId);
            } catch (Exception e) {
                secureLog.error("Feil under innlasting av ensligforsørger-data for aktorId {}", aktorId);
            }
        });

        log.info("Ferdig: Innlastning av ensligforsørger brukerdata");
        return ResponseEntity.ok("Innlasting av EnsligForsørger-data fullført");
    }

    private void sjekkTilgangTilAdmin() {
        boolean erInternBrukerFraAzure = authContextHolder.erInternBruker();
        boolean erPoaoAdmin = POAO_ADMIN.equals(hentApplikasjonFraContex(authContextHolder));

        if (erPoaoAdmin && erInternBrukerFraAzure) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

}
