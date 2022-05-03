package no.nav.pto.veilarbportefolje.admin;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.Id;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesService;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.dialog.DialogService;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchAdminService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingAvsluttetService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingService;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.persononinfo.PdlService;
import no.nav.pto.veilarbportefolje.postgres.AktoerDataOpensearchMapper;
import no.nav.pto.veilarbportefolje.postgres.PostgresOpensearchMapper;
import no.nav.pto.veilarbportefolje.postgres.utils.PostgresAktorIdEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final EnvironmentProperties environmentProperties;
    private final AktorClient aktorClient;
    private final OppfolgingAvsluttetService oppfolgingAvsluttetService;
    private final OpensearchIndexer opensearchIndexer;
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final OppfolgingService oppfolgingService;
    private final AuthContextHolder authContextHolder;
    private final YtelsesService ytelsesService;
    private final OppfolgingRepository oppfolgingRepository;
    private final OpensearchAdminService opensearchAdminService;
    private final PostgresOpensearchMapper postgresOpensearchMapper;
    private final AktoerDataOpensearchMapper aktoerDataOpensearchMapper;
    private final BrukerRepository brukerRepository;
    private final DialogService dialogService;
    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3;
    private final PdlService pdlService;

    @PostMapping("/aktoerId")
    public String aktoerId(@RequestBody String fnr) {
        authorizeAdmin();
        return aktorClient.hentAktorId(Fnr.ofValidFnr(fnr)).get();
    }

    @DeleteMapping("/oppfolgingsbruker")
    public String slettOppfolgingsbruker(@RequestBody String aktoerId) {
        authorizeAdmin();
        oppfolgingAvsluttetService.avsluttOppfolging(AktorId.of(aktoerId));
        return "Slettet oppfølgingsbruker " + aktoerId;
    }

    @DeleteMapping("/fjernBrukerOpensearch")
    @SneakyThrows
    public String fjernBrukerFraOpensearch(@RequestBody String aktoerId) {
        authorizeAdmin();
        opensearchIndexerV2.slettDokumenter(List.of(AktorId.of(aktoerId)));
        return "Slettet bruker fra opensearch " + aktoerId;
    }


    @PostMapping("/lastInnOppfolging")
    public String lastInnOppfolgingsData() {
        authorizeAdmin();
        oppfolgingService.lastInnDataPaNytt();
        return "Innlastning av oppfolgingsdata har startet";
    }

    @PostMapping("/lastInnOppfolgingForBruker")
    public String lastInnOppfolgingsDataForBruker(@RequestBody String fnr) {
        authorizeAdmin();
        String aktorId = aktorClient.hentAktorId(Fnr.ofValidFnr(fnr)).get();
        oppfolgingService.oppdaterBruker(AktorId.of(aktorId));
        return "Innlastning av oppfolgingsdata har startet";
    }

    @PutMapping("/indeks/bruker")
    public String indeks(@RequestBody String fnr) {

        authorizeAdmin();
        String aktorId = aktorClient.hentAktorId(Fnr.ofValidFnr(fnr)).get();
        opensearchIndexer.indekser(AktorId.of(aktorId));
        return "Indeksering fullfort";
    }

    @PostMapping("/indeks/AlleBrukere")
    public String indekserAlleBrukere() {
        authorizeAdmin();
        List<AktorId> brukereUnderOppfolging = oppfolgingRepository.hentAlleGyldigeBrukereUnderOppfolging();
        opensearchIndexer.oppdaterAlleBrukereIOpensearch(brukereUnderOppfolging);
        return "Indeksering fullfort";
    }

    @PutMapping("/ytelser/allUsers")
    public String syncYtelserForAlle() {
        authorizeAdmin();
        List<AktorId> brukereUnderOppfolging = oppfolgingRepository.hentAlleGyldigeBrukereUnderOppfolging();
        brukereUnderOppfolging.forEach(ytelsesService::oppdaterYtelsesInformasjon);
        return "Ytelser er nå i sync";
    }

    @PutMapping("/ytelser/idag")
    public String syncYtelserForIDag() {
        authorizeAdmin();
        ytelsesService.oppdaterBrukereMedYtelserSomStarterIDag();
        return "Aktiviteter er nå i sync";
    }

    @PostMapping("/opensearch/createIndex")
    public String createIndex() {
        authorizeAdmin();
        String indexName = opensearchAdminService.opprettNyIndeks();
        log.info("Opprettet index: {}", indexName);
        return indexName;
    }

    @GetMapping("/opensearch/getAliases")
    public String getAliases() {
        authorizeAdmin();
        return opensearchAdminService.hentAliaser();
    }

    @PostMapping("/opensearch/deleteIndex")
    public boolean deleteIndex(@RequestBody String indexName) {
        authorizeAdmin();
        log.info("Sletter index: {}", indexName);
        return opensearchAdminService.slettIndex(indexName);
    }

    @PostMapping("/opensearch/assignAliasToIndex")
    public String assignAliasToIndex(@RequestBody String indexName) {
        authorizeAdmin();
        opensearchAdminService.opprettAliasForIndeks(indexName);
        return "Ok";
    }

    @PostMapping("/opensearch/getSettings")
    public String getSettings(@RequestBody String indexName) {
        authorizeAdmin();
        return opensearchAdminService.getSettingsOnIndex(indexName);
    }

    @PostMapping("/opensearch/fixReadOnlyMode")
    public String fixReadOnlyMode() {
        authorizeAdmin();
        return opensearchAdminService.updateFromReadOnlyMode();
    }

    @PostMapping("/opensearch/forceShardAssignment")
    public String forceShardAssignment() {
        authorizeAdmin();
        return opensearchAdminService.forceShardAssignment();
    }

    @PostMapping("/pdl/hentAntallIkkeMappet")
    public String hentIkkeMappeteIdenter() {
        authorizeAdmin();
        return "Det er : " + pdlService.hentIdenterSomIkkeErMappet() + " brukere som ikke er mappet";
    }

    @PostMapping("/pdl/lastInnIdenter")
    public String lastInnManglendeIdenter() {
        authorizeAdmin();
        pdlService.mapManglendeIdenter();
        return "Ferdig!";
    }

    @PostMapping("/migrer/dialog")
    public String migrerDialg() {
        authorizeAdmin();
        return dialogService.migrerDialogData();
    }

    @PostMapping("/migrer/arena")
    public String migrerOppfolgingsbruker() {
        authorizeAdmin();
        return oppfolgingsbrukerRepositoryV3.migrerOppfolgingsbrukere();
    }

    @PostMapping("/test/postgresIndeksering")
    public void testHentUnderOppfolging() {
        authorizeAdmin();
        List<AktorId> brukereUnderOppfolging = oppfolgingRepository.hentAlleGyldigeBrukereUnderOppfolging();
        opensearchIndexer.dryrunAvPostgresTilOpensearchMapping(brukereUnderOppfolging);
        log.info("ferdig med dryrun");
    }

    @PutMapping("/test/hentFraOracleOgPostgres")
    public String testHentIndeksertPostgresOgOracleBruker(@RequestBody String aktoerIdString) {
        authorizeAdmin();
        AktorId aktoerId = AktorId.of(aktoerIdString);
        OppfolgingsBruker fraOracle = brukerRepository.hentBrukerFraView(aktoerId).get();

        OppfolgingsBruker fraPostgres = brukerRepository.hentBrukerFraView(aktoerId).get();
        postgresOpensearchMapper.flettInnPostgresData(List.of(fraPostgres), true);

        PostgresAktorIdEntity aktorIdData = aktoerDataOpensearchMapper.hentAktoerData(List.of(aktoerId)).get(aktoerId);

        return "{ \"oracle\":" + JsonUtils.toJson(fraOracle) + ", \"postgres\":" + JsonUtils.toJson(aktorIdData) + " }";
    }

    private void authorizeAdmin() {
        final String ident = authContextHolder.getNavIdent().map(Id::toString).orElseThrow();
        if (!environmentProperties.getAdmins().contains(ident)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
