package no.nav.pto.veilarbportefolje.admin;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.Id;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.cv.CvService;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.oppfolging.NyForVeilederService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingAvsluttetService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingService;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final EnvironmentProperties environmentProperties;
    private final RegistreringService registreringService;
    private final AktorClient aktorClient;
    private final NyForVeilederService nyForVeilederService;
    private final AktivitetService aktivitetService;
    private final OppfolgingAvsluttetService oppfolgingAvsluttetService;
    private final VedtakService vedtakService;
    private final ElasticServiceV2 elasticServiceV2;
    private final OppfolgingService oppfolgingService;
    private final CvService cvService;

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

    @PostMapping("/settSamtykkeCV")
    public String settharDeltCV(@RequestBody String aktoerId) {
        authorizeAdmin();
        cvService.setCVSamtykke(AktorId.of(aktoerId));
        return "Feltet har HAR_DELT_CV oppdatert for aktørId: " + aktoerId;
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
        return "Slettet oppfølgingsbruker " + aktoerId;
    }


    @PostMapping("/lastInnOppfolging")
    public String lastInnOppfolgingsData() {
        authorizeAdmin();
        oppfolgingService.lastInnDataPaNytt();
        return "Rewind av vedtak har startet";
    }

    private void authorizeAdmin() {
        final String ident = AuthContextHolder.getNavIdent().map(Id::toString).orElseThrow();
        if (!environmentProperties.getAdmins().contains(ident)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
