package no.nav.pto.veilarbportefolje.admin;

import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.pto.veilarbportefolje.oppfolging.NyForVeilederService;
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

    public AdminController(EnvironmentProperties environmentProperties, RegistreringService registreringService, AktorregisterClient aktorregisterClient, NyForVeilederService nyForVeilederService) {
        this.admins = environmentProperties.getAdmins();
        this.registreringService = registreringService;
        this.aktorregisterClient = aktorregisterClient;
        this.nyForVeilederService = nyForVeilederService;
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
        return "Rewind av ny ForVeileder har startet";
    }

    private void authorizeAdmin() {
        final String ident = SubjectHandler.getIdent().orElseThrow();
        if (!admins.contains(ident)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
