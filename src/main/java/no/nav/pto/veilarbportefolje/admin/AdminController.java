package no.nav.pto.veilarbportefolje.admin;

import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.oppfolging.NyForVeilederService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingAvsluttetService;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final List<String> admins;
    private final RegistreringService registreringService;
    private final AktorregisterClient aktorregisterClient;
    private final NyForVeilederService nyForVeilederService;
    private final AktivitetService aktivitetService;
    private final OppfolgingAvsluttetService oppfolgingAvsluttetService;

    public AdminController(EnvironmentProperties environmentProperties, RegistreringService registreringService, AktorregisterClient aktorregisterClient, NyForVeilederService nyForVeilederService, AktivitetService aktivitetService, OppfolgingAvsluttetService oppfolgingAvsluttetService) {
        this.admins = environmentProperties.getAdmins();
        this.registreringService = registreringService;
        this.aktorregisterClient = aktorregisterClient;
        this.nyForVeilederService = nyForVeilederService;
        this.aktivitetService = aktivitetService;
        this.oppfolgingAvsluttetService = oppfolgingAvsluttetService;
    }

    @PostMapping("/aktoerId")
    public String aktoerId(@RequestBody String fnr) {
        authorizeAdmin();
        return aktorregisterClient.hentAktorId(fnr);
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

    @DeleteMapping("/oppfolgingsbruker")
    public String slettOppfolgingsbruker(@RequestBody String aktoerId) {
        authorizeAdmin();
        oppfolgingAvsluttetService.avsluttOppfolging(AktoerId.of(aktoerId));
        return "Slettet oppfølgingsbruker " + aktoerId;
    }

    private void authorizeAdmin() {
        final String ident = SubjectHandler.getIdent().orElseThrow();
        if (!admins.contains(ident)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
