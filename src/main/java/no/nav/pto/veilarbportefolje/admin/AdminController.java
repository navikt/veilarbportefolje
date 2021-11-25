package no.nav.pto.veilarbportefolje.admin;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.Id;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesService;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.pto.veilarbportefolje.database.BrukerAktiviteterService;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingAvsluttetService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final ElasticServiceV2 elasticServiceV2;
    private final OppfolgingService oppfolgingService;
    private final AuthContextHolder authContextHolder;
    private final ElasticIndexer elasticIndexer;
    private final BrukerAktiviteterService brukerAktiviteterService;
    private final YtelsesService ytelsesService;
    private final BrukerRepository brukerRepository;
    private final OppfolgingRepository oppfolgingRepository;
    private final ArbeidslisteService arbeidslisteService;


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

    @DeleteMapping("/fjernBrukerElastic")
    @SneakyThrows
    public String fjernBrukerFraElastic(@RequestBody String aktoerId) {
        authorizeAdmin();
        elasticServiceV2.slettDokumenter(List.of(AktorId.of(aktoerId)));
        return "Slettet bruker fra elastic " + aktoerId;
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
        elasticIndexer.indekser(AktorId.of(aktorId));
        return "Indeksering fullfort";
    }

    @PostMapping("/indeks/AlleBrukere")
    public String indekserAlleBrukere() {
        authorizeAdmin();
        List<AktorId> brukereUnderOppfolging = oppfolgingRepository.hentAlleGyldigeBrukereUnderOppfolging();
        elasticIndexer.nyHovedIndeksering(brukereUnderOppfolging);
        return "Indeksering fullfort";
    }


    @PutMapping("/brukerAktiviteter")
    public String syncBrukerAktiviteter(@RequestBody String fnr) {
        authorizeAdmin();
        String aktorId = aktorClient.hentAktorId(Fnr.ofValidFnr(fnr)).get();
        brukerAktiviteterService.syncAktivitetOgBrukerData(AktorId.of(aktorId));

        elasticIndexer.indekser(AktorId.of(aktorId));
        return "Aktiviteter er naa i sync";
    }

    @PutMapping("/brukerAktiviteter/allUsers")
    public String syncBrukerAktiviteterForAlle() {
        authorizeAdmin();
        brukerAktiviteterService.syncAktivitetOgBrukerData();
        return "Aktiviteter er nå i sync";
    }

    @PutMapping("/ytelser/allUsers")
    public String syncYtelserForAlle() {
        authorizeAdmin();
        ytelsesService.syncYtelserForAlleBrukere();
        return "Aktiviteter er nå i sync";
    }

    @PutMapping("/arbeidslista/migrer")
    public String migrerArbeidslista() {
        authorizeAdmin();
        arbeidslisteService.migrerArbeidslistaTilPostgres();
        return "Arbeidslista er nå migrert";
    }

    private void authorizeAdmin() {
        final String ident = authContextHolder.getNavIdent().map(Id::toString).orElseThrow();
        if (!environmentProperties.getAdmins().contains(ident)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
