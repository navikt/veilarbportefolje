package no.nav.pto.veilarbportefolje.admin.v1;

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
import no.nav.pto.veilarbportefolje.opensearch.HovedIndekserer;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchAdminService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingAvsluttetService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingService;
import no.nav.pto.veilarbportefolje.persononinfo.PdlService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static no.nav.pto.veilarbportefolje.auth.AuthUtils.erSystemkallFraAzureAd;
import static no.nav.pto.veilarbportefolje.auth.AuthUtils.hentApplikasjonFraContex;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchConfig.BRUKERINDEKS_ALIAS;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-funksjonalitet som ikke er tilgjengelig for vanlige brukere. Funksjonaliteten er kun tilgjengelig for medlemmer av applikasjonens forvaltningsteam.")
public class AdminController {
    private final String PTO_ADMIN = new DownstreamApi(EnvironmentUtils.isProduction().orElse(false) ?
            "prod-fss" : "dev-fss", "pto", "pto-admin").toString();
    private final AktorClient aktorClient;
    private final OppfolgingAvsluttetService oppfolgingAvsluttetService;
    private final HovedIndekserer hovedIndekserer;
    private final OpensearchIndexer opensearchIndexer;
    private final OppfolgingService oppfolgingService;
    private final AuthContextHolder authContextHolder;
    private final YtelsesService ytelsesService;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final OpensearchAdminService opensearchAdminService;
    private final PdlService pdlService;

    @Operation(summary = "Fjern bruker", description = "Sletter en bruker og fjerner tilhørende informasjon om brukeren. Brukeren vil ikke lenger eksistere i porteføljene.")
    @DeleteMapping("/oppfolgingsbruker")
    public String slettOppfolgingsbruker(@RequestBody String aktoerId) {
        sjekkTilgangTilAdmin();
        oppfolgingAvsluttetService.avsluttOppfolging(AktorId.of(aktoerId));
        return "Oppfølgingsbruker ble slettet";
    }

    @Operation(summary = "Oppdater data for alle brukere", description = "Går gjennom alle brukere i løsningen og oppdaterer data om brukere under oppfølging. Brukere som eventuelt ikke er under oppfølging slettes.")
    @PostMapping("/lastInnOppfolging")
    public String lastInnOppfolgingsData() {
        sjekkTilgangTilAdmin();
        oppfolgingService.lastInnDataPaNytt();
        return "Innlastning av oppfolgingsdata har startet";
    }

    @Operation(summary = "Oppdater data for bruker", description = "Oppdaterer data for en gitt bruker. Dersom brukeren eventuelt ikke er under oppfølging slettes den.")
    @PostMapping("/lastInnOppfolgingForBruker")
    public String lastInnOppfolgingsDataForBruker(@RequestBody String fnr) {
        sjekkTilgangTilAdmin();
        String aktorId = aktorClient.hentAktorId(Fnr.ofValidFnr(fnr)).get();
        oppfolgingService.oppdaterBruker(AktorId.of(aktorId));
        return "Innlastning av oppfolgingsdata har startet";
    }

    @PutMapping("/indeks/bruker/fnr")
    @Deprecated(forRemoval = true)
    public String indeks(@RequestParam String fnr) {
        sjekkTilgangTilAdmin();
        String aktorId = aktorClient.hentAktorId(Fnr.ofValidFnr(fnr)).get();
        opensearchIndexer.indekser(AktorId.of(aktorId));
        return "Indeksering fullfort";
    }

    @PutMapping("/indeks/bruker")
    @Deprecated(forRemoval = true)
    public String indeksAktoerId(@RequestParam String aktorId) {
        sjekkTilgangTilAdmin();
        opensearchIndexer.indekser(AktorId.of(aktorId));
        return "Indeksering fullfort";
    }

    @Operation(summary = "Indekser alle brukere", description = "Går gjennom alle brukere i løsningen og oppdaterer data om disse i søkemotoren (OpenSearch).")
    @PostMapping("/indeks/AlleBrukere")
    public String indekserAlleBrukere() {
        sjekkTilgangTilAdmin();
        return JobRunner.runAsync("Admin_hovedindeksering", () -> {
                    List<AktorId> brukereUnderOppfolging = oppfolgingRepositoryV2.hentAlleGyldigeBrukereUnderOppfolging();
                    opensearchIndexer.oppdaterAlleBrukereIOpensearch(brukereUnderOppfolging);
                }
        );
    }

    @Operation(summary = "Indekser alle brukere på ny index", description = "Går gjennom alle brukere i løsningen og oppdaterer data om disse i søkemotoren (OpenSearch) på en ny index.")
    @PostMapping("/indeks/AlleBrukereNyIndex")
    public String indekserAlleBrukereNyIndex() {
        sjekkTilgangTilAdmin();
        return JobRunner.runAsync("Admin_hovedindeksering_ny_index", () -> {
                    List<AktorId> brukereUnderOppfolging = oppfolgingRepositoryV2.hentAlleGyldigeBrukereUnderOppfolging();
                    hovedIndekserer.aliasBasertHovedIndeksering(brukereUnderOppfolging);
                }
        );
    }

    @Operation(summary = "Synkroniser ytelser for alle brukere", description = "Går gjennom alle brukere i løsningen og oppdaterer data om ytelser for disse.")
    @PutMapping("/ytelser/allUsers")
    public String syncYtelserForAlle() {
        sjekkTilgangTilAdmin();
        List<AktorId> brukereUnderOppfolging = oppfolgingRepositoryV2.hentAlleGyldigeBrukereUnderOppfolging();
        brukereUnderOppfolging.forEach(ytelsesService::oppdaterYtelsesInformasjon);
        return "Ytelser er nå i sync";
    }

    @Operation(summary = "Synkroniser ytelser for alle brukere som har ytelser som starter i dag", description = "Går gjennom alle brukere i løsningen og oppdaterer data om ytelser for disse som starter i dag.")
    @PutMapping("/ytelser/idag")
    public String syncYtelserForIDag() {
        sjekkTilgangTilAdmin();
        ytelsesService.oppdaterBrukereMedYtelserSomStarterIDag();
        return "Aktiviteter er nå i sync";
    }

    @Operation(summary = "Opprett ny index", description = "Oppretter en ny index i søkemotoren (OpenSearch).")
    @PostMapping("/opensearch/createIndex")
    public String createIndex() {
        sjekkTilgangTilAdmin();
        String indexName = opensearchAdminService.opprettNyIndeks();
        log.info("Opprettet index: {}", indexName);
        return indexName;
    }

    @Operation(summary = "Hent alle aliaser", description = "Henter alle aliases som er opprettet i søkemotoren (OpenSearch).")
    @GetMapping("/opensearch/getAliases")
    public String getAliases() {
        sjekkTilgangTilAdmin();
        return opensearchAdminService.hentAliaser();
    }

    @Operation(summary = "Slett index", description = "Sletter en index i søkemotoren (OpenSearch).")
    @PostMapping("/opensearch/deleteIndex")
    public boolean deleteIndex(@RequestParam String indexName) {
        sjekkTilgangTilAdmin();
        log.info("Sletter index: {}", indexName);
        return opensearchAdminService.slettIndex(indexName);
    }

    @Operation(summary = "Tildel alias til index", description = "Tildeler et alias til en index i søkemotoren (OpenSearch).")
    @PostMapping("/opensearch/assignAliasToIndex")
    public String assignAliasToIndex(@RequestParam String indexName) {
        sjekkTilgangTilAdmin();
        opensearchAdminService.opprettAliasForIndeks(indexName);
        return "Ok";
    }

    @Operation(summary = "Hent innstillinger for index", description = "Henter innstillinger for en index i søkemotoren (OpenSearch).")
    @PostMapping("/opensearch/getSettings")
    public String getSettings(@RequestParam String indexName) {
        sjekkTilgangTilAdmin();
        validerIndexName(indexName);
        return opensearchAdminService.getSettingsOnIndex(indexName);
    }

    @Operation(summary = "Fiks read only mode", description = "Fjerner read only mode på en index i søkemotoren (OpenSearch).")
    @PostMapping("/opensearch/fixReadOnlyMode")
    public String fixReadOnlyMode() {
        sjekkTilgangTilAdmin();
        return opensearchAdminService.updateFromReadOnlyMode();
    }

    @Operation(summary = "Tving shard assignment", description = "Tvinger shard assignment på en index i søkemotoren (OpenSearch).")
    @PostMapping("/opensearch/forceShardAssignment")
    public String forceShardAssignment() {
        sjekkTilgangTilAdmin();
        return opensearchAdminService.forceShardAssignment();
    }

    @Operation(summary = "Last inn PDL data", description = "Henter og lagrer data fra PDL (identer, personalia og foreldreansvar) for alle brukere i løsningen.")
    @PostMapping("/pdl/lastInnDataFraPdl")
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


    @Operation(summary = "Test indeksering av brukere", description = "Går gjennom alle brukere i løsningen og gjør en dry-run av mapping til datamodell som benyttes av søkemotoren (OpenSearch).")
    @PostMapping("/test/postgresIndeksering")
    public void testHentUnderOppfolging() {
        sjekkTilgangTilAdmin();
        List<AktorId> brukereUnderOppfolging = oppfolgingRepositoryV2.hentAlleGyldigeBrukereUnderOppfolging();
        opensearchIndexer.dryrunAvPostgresTilOpensearchMapping(brukereUnderOppfolging);
        log.info("ferdig med dryrun");
    }

    private void sjekkTilgangTilAdmin() {
        boolean erSystemBrukerFraAzure = erSystemkallFraAzureAd(authContextHolder);
        boolean erPtoAdmin = PTO_ADMIN.equals(hentApplikasjonFraContex(authContextHolder));

        if (erPtoAdmin && erSystemBrukerFraAzure) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    private void validerIndexName(String indexName) {
        if (!BRUKERINDEKS_ALIAS.equals(indexName)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
