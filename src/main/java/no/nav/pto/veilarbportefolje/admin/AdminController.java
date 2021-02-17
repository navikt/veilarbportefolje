package no.nav.pto.veilarbportefolje.admin;

import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.Id;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.oppfolging.NyForVeilederService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingAvsluttetService;
import no.nav.pto.veilarbportefolje.pdldata.PdlDataService;
import no.nav.pto.veilarbportefolje.pdldata.PdlRepository;
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
    private final AktorClient aktorClient;
    private final NyForVeilederService nyForVeilederService;
    private final AktivitetService aktivitetService;
    private final OppfolgingAvsluttetService oppfolgingAvsluttetService;
    private final PdlDataService pdlDataService;

    public AdminController(EnvironmentProperties environmentProperties, RegistreringService registreringService, AktorClient aktorClient,
                           NyForVeilederService nyForVeilederService, AktivitetService aktivitetService,
                           OppfolgingAvsluttetService oppfolgingAvsluttetService, PdlDataService pdlDataService) {
        this.admins = environmentProperties.getAdmins();
        this.registreringService = registreringService;
        this.aktorClient = aktorClient;
        this.nyForVeilederService = nyForVeilederService;
        this.aktivitetService = aktivitetService;
        this.oppfolgingAvsluttetService = oppfolgingAvsluttetService;
        this.pdlDataService = pdlDataService;
    }

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

    @DeleteMapping("/oppfolgingsbruker")
    public String slettOppfolgingsbruker(@RequestBody String aktoerId) {
        authorizeAdmin();
        oppfolgingAvsluttetService.avsluttOppfolging(AktorId.of(aktoerId));
        return "Slettet oppfølgingsbruker " + aktoerId;
    }

    @PostMapping("/fodselsDatatilNyTabell")
    public String flyttFodselsDagDataTilNyTabell() {
        authorizeAdmin();
        pdlDataService.lastInnDataFraDbLinkTilPdlDataTabell();
        return "Overføring har startet...";
    }


    private void authorizeAdmin() {
        final String ident = AuthContextHolder.getNavIdent().map(Id::toString).orElseThrow();
        if (!admins.contains(ident)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
