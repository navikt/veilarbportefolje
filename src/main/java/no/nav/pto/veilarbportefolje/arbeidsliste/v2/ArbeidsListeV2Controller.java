package no.nav.pto.veilarbportefolje.arbeidsliste.v2;

import io.vavr.control.Validation;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteDTO;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.arbeidsliste.SlettArbeidslisteException;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;

import static no.nav.common.utils.StringUtils.nullOrEmpty;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;
import static no.nav.pto.veilarbportefolje.util.ValideringsRegler.validerArbeidslisteV2;

@Slf4j
@RestController
@RequestMapping("/api/v2")
public class ArbeidsListeV2Controller {
    private final ArbeidslisteService arbeidslisteService;
    private final BrukerServiceV2 brukerService;
    private final AuthService authService;

    @Autowired
    public ArbeidsListeV2Controller(
            ArbeidslisteService arbeidslisteService,
            BrukerServiceV2 brukerService,
            AuthService authService
    ) {
        this.arbeidslisteService = arbeidslisteService;
        this.brukerService = brukerService;
        this.authService = authService;
    }

    @PostMapping("/hent-arbeidsliste")
    public Arbeidsliste getArbeidsListe(@RequestBody ArbeidslisteForBrukerRequest arbeidslisteForBrukerRequest) {
        validerOppfolgingOgBruker(arbeidslisteForBrukerRequest.fnr().get());

        Fnr validertFnr = Fnr.ofValidFnr(arbeidslisteForBrukerRequest.fnr().get());
        String innloggetVeileder = AuthUtils.getInnloggetVeilederIdent().toString();


        boolean harVeilederTilgang = brukerService.hentNavKontor(validertFnr)
                .map(enhet -> authService.harVeilederTilgangTilEnhet(innloggetVeileder, enhet.getValue()))
                .orElse(false);

        Arbeidsliste arbeidsliste = arbeidslisteService.getArbeidsliste(validertFnr)
                .toJavaOptional()
                .orElse(emptyArbeidsliste())
                .setIsOppfolgendeVeileder(
                        arbeidslisteService.erVeilederForBruker(validertFnr, VeilederId.of(innloggetVeileder))
                )
                .setHarVeilederTilgang(harVeilederTilgang);

        return harVeilederTilgang ? arbeidsliste : emptyArbeidsliste().setHarVeilederTilgang(false);
    }

    @PostMapping("/arbeidsliste")
    public Arbeidsliste opprettArbeidsListe(@RequestBody ArbeidslisteV2Request body) {
        validerOppfolgingOgBruker(body.fnr().get());
        validerErVeilederForBruker(body.fnr().get());
        Fnr gyldigFnr = Fnr.ofValidFnr(body.fnr().get());
        sjekkTilgangTilEnhet(gyldigFnr);

        arbeidslisteService.createArbeidsliste(data(body, gyldigFnr))
                .onFailure(e -> secureLog.warn("Kunne ikke opprette arberidsliste: {}", e.getMessage()))
                .getOrElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        return arbeidslisteService.getArbeidsliste(gyldigFnr).get()
                .setHarVeilederTilgang(true)
                .setIsOppfolgendeVeileder(true);
    }

    @PutMapping("/arbeidsliste")
    public Arbeidsliste oppdaterArbeidsListe(@RequestBody ArbeidslisteV2Request body) {
        validerOppfolgingOgBruker(body.fnr().get());
        Fnr fnr = Fnr.ofValidFnr(body.fnr().get());
        sjekkTilgangTilEnhet(fnr);
        validerArbeidslisteV2(body, true);

        arbeidslisteService
                .updateArbeidsliste(data(body, fnr))
                .onFailure(e -> secureLog.warn("Kunne ikke oppdatere arbeidsliste: {}", e.getMessage()))
                .getOrElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        if (arbeidslisteService.getArbeidsliste(fnr).get() == null) {
            VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();
            NavKontor enhet = brukerService.hentNavKontor(fnr).orElse(null);
            secureLog.warn("Arbeidsliste kunne ikke oppdateres, var null, fnr: {}, veileder: {}, på enhet: {}", body.fnr().get(), veilederId, enhet);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Kunne ikke oppdatere. Fant ikke arbeidsliste for bruker");
        }

        return arbeidslisteService.getArbeidsliste(fnr).get()
                .setHarVeilederTilgang(true)
                .setIsOppfolgendeVeileder(arbeidslisteService.erVeilederForBruker(
                        fnr,
                        AuthUtils.getInnloggetVeilederIdent()));
    }

    @DeleteMapping("/arbeidsliste")
    public Arbeidsliste deleteArbeidsliste(@RequestBody ArbeidslisteForBrukerRequest arbeidslisteForBrukerRequest) {
        validerOppfolgingOgBruker(arbeidslisteForBrukerRequest.fnr().get());
        validerErVeilederForBruker(arbeidslisteForBrukerRequest.fnr().get());
        Fnr gyldigFnr = Fnr.ofValidFnr(arbeidslisteForBrukerRequest.fnr().get());
        sjekkTilgangTilEnhet(gyldigFnr);

        try {
            arbeidslisteService.slettArbeidsliste(gyldigFnr);
        } catch (SlettArbeidslisteException e) {
            VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();
            NavKontor enhet = brukerService.hentNavKontor(gyldigFnr).orElse(null);
            secureLog.warn("Kunne ikke slette arbeidsliste for fnr: {}, av veileder: {}, på enhet: {}", gyldigFnr.get(), veilederId.toString(), enhet);

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Kunne ikke slette. Fant ikke arbeidsliste for bruker");
        }

        return emptyArbeidsliste().setHarVeilederTilgang(true).setIsOppfolgendeVeileder(true);
    }

    private void sjekkTilgangTilEnhet(Fnr fnr) {
        NavKontor enhet = brukerService.hentNavKontor(fnr).orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Kunne ikke hente enhet for denne brukeren"));
        authService.innloggetVeilederHarTilgangTilEnhet(enhet.getValue());
    }

    private ArbeidslisteDTO data(ArbeidslisteV2Request body, Fnr fnr) {
        Timestamp frist = nullOrEmpty(body.frist()) ? null : Timestamp.from(Instant.parse(body.frist()));
        return new ArbeidslisteDTO(fnr)
                .setVeilederId(AuthUtils.getInnloggetVeilederIdent())
                .setOverskrift(body.overskrift())
                .setKommentar(body.kommentar())
                .setKategori(Arbeidsliste.Kategori.valueOf(body.kategori()))
                .setFrist(frist);
    }


    public static Arbeidsliste emptyArbeidsliste() {
        return new Arbeidsliste(null, null, null, null, null, null);
    }

    private void validerOppfolgingOgBruker(String fnr) {
        authService.innloggetVeilederHarTilgangTilOppfolging();
        Validation<String, Fnr> validateFnr = ValideringsRegler.validerFnr(fnr);
        authService.innloggetVeilederHarTilgangTilBruker(fnr);
        if (validateFnr.isInvalid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private void validerErVeilederForBruker(String fnr) {
        Validation<String, Fnr> validateVeileder = arbeidslisteService.erVeilederForBruker(fnr);
        if (validateVeileder.isInvalid()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
