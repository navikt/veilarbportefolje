package no.nav.pto.veilarbportefolje.admin;

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
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
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

    @DeleteMapping("/oppfolgingsbruker")
    public String slettOppfolgingsbruker(@RequestBody String aktoerId) {
        sjekkTilgangTilAdmin();
        oppfolgingAvsluttetService.avsluttOppfolging(AktorId.of(aktoerId));
        return "Slettet oppfølgingsbruker " + aktoerId;
    }

    @PostMapping("/lastInnOppfolging")
    public String lastInnOppfolgingsData() {
        sjekkTilgangTilAdmin();
        oppfolgingService.lastInnDataPaNytt();
        return "Innlastning av oppfolgingsdata har startet";
    }

    @PostMapping("/lastInnOppfolgingForBruker")
    public String lastInnOppfolgingsDataForBruker(@RequestBody String fnr) {
        sjekkTilgangTilAdmin();
        String aktorId = aktorClient.hentAktorId(Fnr.ofValidFnr(fnr)).get();
        oppfolgingService.oppdaterBruker(AktorId.of(aktorId));
        return "Innlastning av oppfolgingsdata har startet";
    }

    @PutMapping("/indeks/bruker/fnr")
    public String indeks(@RequestParam String fnr) {
        sjekkTilgangTilAdmin();
        String aktorId = aktorClient.hentAktorId(Fnr.ofValidFnr(fnr)).get();
        opensearchIndexer.indekser(AktorId.of(aktorId));
        return "Indeksering fullfort";
    }

    @PutMapping("/indeks/bruker")
    public String indeksAktoerId(@RequestParam String aktorId) {
        sjekkTilgangTilAdmin();
        opensearchIndexer.indekser(AktorId.of(aktorId));
        return "Indeksering fullfort";
    }

    @PostMapping("/indeks/AlleBrukere")
    public String indekserAlleBrukere() {
        sjekkTilgangTilAdmin();
        return JobRunner.runAsync("Admin_hovedindeksering", () -> {
                    List<AktorId> brukereUnderOppfolging = oppfolgingRepositoryV2.hentAlleGyldigeBrukereUnderOppfolging();
                    opensearchIndexer.oppdaterAlleBrukereIOpensearch(brukereUnderOppfolging);
                }
        );
    }

    @PostMapping("/indeks/AlleBrukereNyIndex")
    public String indekserAlleBrukereNyIndex() {
        sjekkTilgangTilAdmin();
        return JobRunner.runAsync("Admin_hovedindeksering_ny_index", () -> {
                    List<AktorId> brukereUnderOppfolging = oppfolgingRepositoryV2.hentAlleGyldigeBrukereUnderOppfolging();
                    hovedIndekserer.aliasBasertHovedIndeksering(brukereUnderOppfolging);
                }
        );
    }

    @PutMapping("/ytelser/allUsers")
    public String syncYtelserForAlle() {
        sjekkTilgangTilAdmin();
        List<AktorId> brukereUnderOppfolging = oppfolgingRepositoryV2.hentAlleGyldigeBrukereUnderOppfolging();
        brukereUnderOppfolging.forEach(ytelsesService::oppdaterYtelsesInformasjon);
        return "Ytelser er nå i sync";
    }

    @PutMapping("/ytelser/idag")
    public String syncYtelserForIDag() {
        sjekkTilgangTilAdmin();
        ytelsesService.oppdaterBrukereMedYtelserSomStarterIDag();
        return "Aktiviteter er nå i sync";
    }

    @PostMapping("/opensearch/createIndex")
    public String createIndex() {
        sjekkTilgangTilAdmin();
        String indexName = opensearchAdminService.opprettNyIndeks();
        log.info("Opprettet index: {}", indexName);
        return indexName;
    }

    @GetMapping("/opensearch/getAliases")
    public String getAliases() {
        sjekkTilgangTilAdmin();
        return opensearchAdminService.hentAliaser();
    }

    @PostMapping("/opensearch/deleteIndex")
    public boolean deleteIndex(@RequestParam String indexName) {
        sjekkTilgangTilAdmin();
        log.info("Sletter index: {}", indexName);
        return opensearchAdminService.slettIndex(indexName);
    }

    @PostMapping("/opensearch/assignAliasToIndex")
    public String assignAliasToIndex(@RequestParam String indexName) {
        sjekkTilgangTilAdmin();
        opensearchAdminService.opprettAliasForIndeks(indexName);
        return "Ok";
    }

    @PostMapping("/opensearch/getSettings")
    public String getSettings(@RequestParam String indexName) {
        sjekkTilgangTilAdmin();
        return opensearchAdminService.getSettingsOnIndex(indexName);
    }

    @PostMapping("/opensearch/fixReadOnlyMode")
    public String fixReadOnlyMode() {
        sjekkTilgangTilAdmin();
        return opensearchAdminService.updateFromReadOnlyMode();
    }

    @PostMapping("/opensearch/forceShardAssignment")
    public String forceShardAssignment() {
        sjekkTilgangTilAdmin();
        return opensearchAdminService.forceShardAssignment();
    }

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
                //TODO:: Må vi hensynta barn her?
            } catch (Exception e) {
                secureLog.info("pdl brukerdata: feil under innlastning av pdl data på bruker: {}", bruker, e);
            }
        });
        log.info("pdl brukerdata: ferdig med innlastning");
        return "ferdig";
    }

    @PutMapping("/pdl/lastInnDataFraPdl")
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
}
