package no.nav.pto.veilarbportefolje.arbeidsliste.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Validation;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteDTO;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.arbeidsliste.SlettArbeidslisteException;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.domene.RestResponse;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static no.nav.common.utils.StringUtils.nullOrEmpty;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;
import static no.nav.pto.veilarbportefolje.util.ValideringsRegler.validerArbeidsliste;

@Slf4j
@RestController
@RequestMapping("/api/arbeidsliste")
@Tag(name = "Arbeidsliste", description = "Arbeidsliste-funksjonalitet")
public class ArbeidsListeController {
    private final ArbeidslisteService arbeidslisteService;
    private final BrukerServiceV2 brukerService;
    private final AuthService authService;

    @Autowired
    public ArbeidsListeController(
            ArbeidslisteService arbeidslisteService,
            BrukerServiceV2 brukerService,
            AuthService authService
    ) {
        this.arbeidslisteService = arbeidslisteService;
        this.brukerService = brukerService;
        this.authService = authService;

    }


    @PostMapping
    @Operation(summary = "Opprett arbeidslister", description = "Oppretter arbeidslister for et sett med brukere.")
    public ResponseEntity opprettArbeidsListe(@RequestBody List<ArbeidslisteRequest> arbeidsliste) {
        authService.innloggetVeilederHarTilgangTilOppfolging();
        List<String> tilgangErrors = getTilgangErrors(arbeidsliste);
        if (!tilgangErrors.isEmpty()) {
            return RestResponse.of(tilgangErrors).forbidden();
        }

        RestResponse<String> response = new RestResponse<>(new ArrayList<>(), new ArrayList<>());

        arbeidsliste.forEach(request -> {
            RestResponse<AktorId> opprettArbeidsliste = opprettArbeidsliste(request);
            if (opprettArbeidsliste.containsError()) {
                response.addError(request.getFnr());
            } else {
                response.addData(request.getFnr());
            }
        });

        return response.data.isEmpty() ? response.badRequest() : response.created();
    }

    /**
     * @deprecated Skal fjernes høsten 2024 når arbeidslistene slettes.
     */
    @GetMapping("{fnr}")
    @Deprecated(forRemoval = true)
    @Operation(summary = "Hent arbeidsliste", description = "Henter arbeidsliste for en gitt bruker.")
    public Arbeidsliste getArbeidsListe(@PathVariable("fnr") String fnrString) {
        validerOppfolgingOgBruker(fnrString);

        Fnr validertFnr = Fnr.ofValidFnr(fnrString);
        String innloggetVeileder = AuthUtils.getInnloggetVeilederIdent().toString();

        boolean harVeilederTilgang = brukerService.hentNavKontor(validertFnr)
                .map(enhet -> authService.harVeilederTilgangTilEnhet(innloggetVeileder, enhet.getValue()))
                .orElse(false);

        Arbeidsliste arbeidsliste =
                arbeidslisteService.getArbeidsliste(validertFnr)
                        .toJavaOptional()
                        .orElse(emptyArbeidsliste())
                        .setIsOppfolgendeVeileder(
                                arbeidslisteService.erVeilederForBruker(validertFnr, VeilederId.of(innloggetVeileder)))
                        .setHarVeilederTilgang(harVeilederTilgang);

        return harVeilederTilgang ? arbeidsliste : emptyArbeidsliste().setHarVeilederTilgang(false);
    }

    /**
     * @deprecated Skal fjernes høsten 2024 når arbeidslistene slettes.
     */
    @PostMapping("{fnr}")
    @Deprecated(forRemoval = true)
    @Operation(summary = "Opprett arbeidsliste", description = "Oppretter en arbeidsliste for en gitt bruker.")
    public Arbeidsliste opprettArbeidsListe(@RequestBody ArbeidslisteRequest body, @PathVariable("fnr") String fnr) {
        validerOppfolgingOgBruker(fnr);
        sjekkTilgangTilEnhet(Fnr.ofValidFnr(fnr));

        arbeidslisteService.createArbeidsliste(data(body, Fnr.ofValidFnr(fnr)))
                .onFailure(e -> secureLog.warn("Kunne ikke opprette arbeidsliste: {}", e.getMessage()))
                .getOrElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        return arbeidslisteService.getArbeidsliste(Fnr.ofValidFnr(fnr)).get()
                .setHarVeilederTilgang(true)
                .setIsOppfolgendeVeileder(true);
    }

    /**
     * @deprecated Skal fjernes høsten 2024 når arbeidslistene slettes.
     */
    @PutMapping("{fnr}")
    @Deprecated(forRemoval = true)
    @Operation(summary = "Oppdater arbeidsliste", description = "Oppdaterer en arbeidsliste med nye felter for en gitt bruker.")
    public Arbeidsliste oppdaterArbeidsListe(@RequestBody ArbeidslisteRequest body, @PathVariable("fnr") String fnrString) {
        validerOppfolgingOgBruker(fnrString);
        Fnr fnr = Fnr.ofValidFnr(fnrString);
        sjekkTilgangTilEnhet(fnr);
        validerArbeidsliste(body, true);

        arbeidslisteService
                .updateArbeidsliste(data(body, fnr))
                .onFailure(e -> secureLog.warn("Kunne ikke oppdatere arbeidsliste: {}", e.getMessage()))
                .getOrElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        if (arbeidslisteService.getArbeidsliste(fnr).get() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Kunne ikke oppdatere. Fant ikke arbeidsliste for bruker");
        }

        return arbeidslisteService.getArbeidsliste(fnr).get()
                .setHarVeilederTilgang(true)
                .setIsOppfolgendeVeileder(arbeidslisteService.erVeilederForBruker(
                        fnr,
                        AuthUtils.getInnloggetVeilederIdent()));
    }

    /**
     * @deprecated Skal fjernes høsten 2024 når arbeidslistene slettes.
     */
    @DeleteMapping("{fnr}")
    @Deprecated(forRemoval = true)
    @Operation(summary = "Slett arbeidsliste", description = "Sletter en arbeidsliste for en gitt bruker.")
    public Arbeidsliste deleteArbeidsliste(@PathVariable("fnr") String fnr) {
        validerOppfolgingOgBruker(fnr);
        sjekkTilgangTilEnhet(Fnr.ofValidFnr(fnr));

        try {
            arbeidslisteService.slettArbeidsliste(Fnr.ofValidFnr(fnr), true);
        } catch (SlettArbeidslisteException e) {
            VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();
            NavKontor enhet = brukerService.hentNavKontor(Fnr.ofValidFnr(fnr)).orElse(null);
            secureLog.warn(String.format("Kunne ikke slette arbeidsliste for fnr:%s, av veileder: %s, på enhet: %s", fnr, veilederId.toString(), enhet), e);

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Kunne ikke slette. Fant ikke arbeidsliste for bruker");
        }

        return emptyArbeidsliste().setHarVeilederTilgang(true).setIsOppfolgendeVeileder(true);
    }

    @PostMapping("/delete")
    @Operation(summary = "Slett arbeidslister", description = "Sletter arbeidslister for et sett med brukere.")
    public RestResponse<String> deleteArbeidslisteListe(@RequestBody java.util.List<ArbeidslisteRequest> arbeidslisteData) {
        authService.innloggetVeilederHarTilgangTilOppfolging();

        java.util.List<String> feiledeFnrs = new ArrayList<>();
        java.util.List<String> okFnrs = new ArrayList<>();

        java.util.List<Fnr> fnrs = arbeidslisteData
                .stream()
                .map(data -> Fnr.ofValidFnr(data.getFnr()))
                .collect(Collectors.toList());

        Validation<List<Fnr>, List<Fnr>> validerteFnrs = ValideringsRegler.validerFnrs(fnrs);
        if (validerteFnrs.isInvalid()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, format("%s inneholder ett eller flere ugyldige fødselsnummer", validerteFnrs.getError()));
        }

        validerteFnrs.get().forEach(fnr -> {
            final AktorId aktoerId = brukerService.hentAktorId(fnr)
                    .orElse(new AktorId("uten aktør-ID"));

            try {
                arbeidslisteService.slettArbeidsliste(fnr, true);
                okFnrs.add(fnr.get());
                secureLog.info("Arbeidsliste for aktoerid {} slettet", aktoerId.get());
            } catch (SlettArbeidslisteException e) {
                feiledeFnrs.add(fnr.get());
                secureLog.warn("Kunne ikke slette arbeidsliste for bruker {} ", aktoerId.get());
            }
        });

        if (feiledeFnrs.size() == fnrs.size()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return RestResponse.of(okFnrs, feiledeFnrs);
    }

    private void sjekkTilgangTilEnhet(Fnr fnr) {
        NavKontor enhet = brukerService.hentNavKontor(fnr).orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Kunne ikke hente enhet for denne brukeren"));
        authService.innloggetVeilederHarTilgangTilEnhet(enhet.getValue());
    }

    private ArbeidslisteDTO data(ArbeidslisteRequest body, Fnr fnr) {
        Timestamp frist = nullOrEmpty(body.getFrist()) ? null : Timestamp.from(Instant.parse(body.getFrist()));
        return new ArbeidslisteDTO(fnr)
                .setVeilederId(AuthUtils.getInnloggetVeilederIdent())
                .setOverskrift(body.getOverskrift())
                .setKommentar(body.getKommentar())
                .setKategori(Arbeidsliste.Kategori.valueOf(body.getKategori()))
                .setFrist(frist);
    }

    private List<String> getTilgangErrors(List<ArbeidslisteRequest> arbeidsliste) {
        return arbeidsliste
                .stream()
                .map(bruker -> arbeidslisteService.erVeilederForBruker(bruker.getFnr()))
                .filter(Validation::isInvalid)
                .map(Validation::getError)
                .collect(Collectors.toList());
    }

    private RestResponse<AktorId> opprettArbeidsliste(ArbeidslisteRequest arbeidslisteRequest) {
        return validerArbeidsliste(arbeidslisteRequest, false)
                .map(arbeidslisteService::createArbeidsliste)
                .fold(
                        validationErr -> RestResponse.of(validationErr.toJavaList()),
                        result -> {
                            if (result.isFailure()) {
                                return RestResponse.of(result.getCause().getMessage());
                            }
                            return RestResponse.of(result.get().getAktorId());

                        }
                );
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
}
