package no.nav.pto.veilarbportefolje.admin;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.Id;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesService;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.pto.veilarbportefolje.cv.CVService;
import no.nav.pto.veilarbportefolje.database.BrukerAktiviteterService;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.oppfolging.*;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final EnvironmentProperties environmentProperties;
    private final RegistreringService registreringService;
    private final AktorClient aktorClient;
    private final NyForVeilederService nyForVeilederService;
    private final VeilederTilordnetService veilederTilordnetService;
    private final AktivitetService aktivitetService;
    private final OppfolgingAvsluttetService oppfolgingAvsluttetService;
    private final VedtakService vedtakService;
    private final ElasticServiceV2 elasticServiceV2;
    private final OppfolgingService oppfolgingService;
    private final AuthContextHolder authContextHolder;
    private final CVService cvService;
    private final KafkaConfigCommon kafkaConfigCommon;
    private final ElasticIndexer elasticIndexer;
    private final BrukerAktiviteterService brukerAktiviteterService;
    private final YtelsesService ytelsesService;
    private final BrukerRepository brukerRepository;
    private final OppfolgingRepository oppfolgingRepository;


    @PostMapping("/aktoerId")
    public String aktoerId(@RequestBody String fnr) {
        authorizeAdmin();
        return aktorClient.hentAktorId(Fnr.ofValidFnr(fnr)).get();
    }

    @PostMapping("/rewind/registrering")
    public String rewindReg() {
        authorizeAdmin();
        registreringService.setRewind(true);
        return "Rewind av registrering har startet";
    }

    @PostMapping("/rewind/nyForVeileder")
    public String rewindNyVeileder() {
        authorizeAdmin();
        nyForVeilederService.setRewind(true);
        return "Rewind av nyVeileder har startet";
    }

    @PostMapping("/rewind/aktivtet")
    public String rewindAktivteter() {
        authorizeAdmin();
        aktivitetService.setRewind(true);
        return "Rewind av aktivteter har startet";
    }

    @PostMapping("/rewind/vedtak")
    public String rewindVedtak() {
        authorizeAdmin();
        vedtakService.setRewind(true);
        return "Rewind av vedtak har startet";
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
        Optional<OppfolgingsBruker> oppfolgingsBruker = brukerRepository.hentBrukerFraView(Fnr.of(fnr));
        if (oppfolgingsBruker.isPresent()) {
            oppfolgingService.oppdaterBruker(oppfolgingsBruker.get());
            return "Innlastning av oppfolgingsdata har startet";
        }
        return "Bruker eksistere ikke";
    }


    @PostMapping("/rewind/cv-eksisterer")
    public String rewindCVEksistere() {
        authorizeAdmin();
        cvService.setRewind(true);
        return "Rewind av cv har startet";
    }

    @PostMapping("/rewind/tilordnet-veileder")
    public String rewindTilordnetVeileder() {
        authorizeAdmin();
        veilederTilordnetService.setRewind(true);
        return "Rewind av tilordnet veileder har startet";
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
        List<AktorId> brukereUnderOppfolging = oppfolgingRepository.hentAlleBrukereUnderOppfolging();
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


    private void authorizeAdmin() {
        final String ident = authContextHolder.getNavIdent().map(Id::toString).orElseThrow();
        if (!environmentProperties.getAdmins().contains(ident)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
