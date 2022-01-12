package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import io.vavr.control.Validation;
import static java.lang.String.format;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import static no.nav.common.utils.StringUtils.nullOrEmpty;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.RestResponse;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import static no.nav.pto.veilarbportefolje.util.ValideringsRegler.validerArbeidsliste;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/arbeidsliste")
public class ArbeidsListeController {
    private final ArbeidslisteService arbeidslisteService;
    private final BrukerService brukerService;
    private final AktorClient aktorClient;
    private final AuthService authService;

    @Autowired
    public ArbeidsListeController(
            ArbeidslisteService arbeidslisteService,
            BrukerService brukerService,
            AktorClient aktorClient, AuthService authService
    ) {
        this.arbeidslisteService = arbeidslisteService;
        this.brukerService = brukerService;
        this.aktorClient = aktorClient;
        this.authService = authService;

    }


    @PostMapping
    public ResponseEntity opprettArbeidsListe(@RequestBody List<ArbeidslisteRequest> arbeidsliste) {
        authService.tilgangTilOppfolging();
        List<String> tilgangErrors = getTilgangErrors(arbeidsliste);
        if (tilgangErrors.size() > 0) {
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

    @GetMapping("{fnr}")
    public Arbeidsliste getArbeidsListe(@PathVariable("fnr") String fnrString) {
        validerOppfolgingOgBruker(fnrString);

        String innloggetVeileder = AuthUtils.getInnloggetVeilederIdent().toString();

        Fnr fnr = Fnr.ofValidFnr(fnrString);
        Try<AktorId> aktoerId = Try.of(()-> aktorClient.hentAktorId(fnr));

        boolean harVeilederTilgang = brukerService.hentNavKontorFraDbLinkTilArena(fnr)
                .map(enhet -> authService.harVeilederTilgangTilEnhet(innloggetVeileder, enhet))
                .orElse(false);

        Arbeidsliste arbeidsliste = aktoerId
                .flatMap((id) -> arbeidslisteService.getArbeidsliste(id, innloggetVeileder))
                .toJavaOptional()
                .orElse(emptyArbeidsliste())
                .setIsOppfolgendeVeileder(aktoerId.map(id ->
                        arbeidslisteService.erVeilederForBruker(fnr, VeilederId.of(innloggetVeileder))).get())
                .setHarVeilederTilgang(harVeilederTilgang);

        return harVeilederTilgang ? arbeidsliste : emptyArbeidsliste().setHarVeilederTilgang(false);
    }

    @PostMapping("{fnr}")
    public Arbeidsliste opprettArbeidsListe(@RequestBody ArbeidslisteRequest body, @PathVariable("fnr") String fnr) {
        validerOppfolgingOgBruker(fnr);
        validerErVeilederForBruker(fnr);
        sjekkTilgangTilEnhet(Fnr.ofValidFnr(fnr));

        String veileder  = AuthUtils.getInnloggetVeilederIdent().toString();

        arbeidslisteService.createArbeidsliste(data(body, Fnr.ofValidFnr(fnr)))
                .onFailure(e -> log.warn("Kunne ikke opprette arbeidsliste: {}", e.getMessage()))
                .getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);

        return arbeidslisteService.getArbeidsliste(Fnr.ofValidFnr(fnr), veileder).get()
                .setHarVeilederTilgang(true)
                .setIsOppfolgendeVeileder(true);
    }

    @PutMapping("{fnr}")
    public Arbeidsliste oppdaterArbeidsListe(@RequestBody ArbeidslisteRequest body, @PathVariable("fnr") String fnrString) {
        validerOppfolgingOgBruker(fnrString);
        Fnr fnr = Fnr.ofValidFnr(fnrString);
        sjekkTilgangTilEnhet(fnr);
        validerArbeidsliste(body, true);

        String veileder  = AuthUtils.getInnloggetVeilederIdent().toString();
        arbeidslisteService
                .updateArbeidsliste(data(body, fnr))
                .onFailure(e -> log.warn("Kunne ikke oppdatere arbeidsliste: {}", e.getMessage()))
                .getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);

        return arbeidslisteService.getArbeidsliste(fnr, veileder).get()
                .setHarVeilederTilgang(true)
                .setIsOppfolgendeVeileder(arbeidslisteService.erVeilederForBruker(
                        fnr,
                        AuthUtils.getInnloggetVeilederIdent()));
    }

    @DeleteMapping("{fnr}")
    public Arbeidsliste deleteArbeidsliste(@PathVariable("fnr") String fnr) {
        validerOppfolgingOgBruker(fnr);
        validerErVeilederForBruker(fnr);
        sjekkTilgangTilEnhet(Fnr.ofValidFnr(fnr));

        final int antallRaderSlettet = arbeidslisteService.slettArbeidsliste(Fnr.ofValidFnr(fnr));
        if (antallRaderSlettet != 1) {
            throw new IllegalStateException("Kunne ikke slette. Fant ikke arbeidsliste for bruker");
        }

        return emptyArbeidsliste().setHarVeilederTilgang(true).setIsOppfolgendeVeileder(true);
    }

    @PostMapping("/delete")
    public RestResponse<String> deleteArbeidslisteListe(@RequestBody java.util.List<ArbeidslisteRequest> arbeidslisteData) {
            authService.tilgangTilOppfolging();

            java.util.List<String> feiledeFnrs = new ArrayList<>();
            java.util.List<String> okFnrs = new ArrayList<>();

            java.util.List<Fnr> fnrs = arbeidslisteData
                    .stream()
                    .map(data -> Fnr.ofValidFnr(data.getFnr()))
                    .collect(Collectors.toList());

            Validation<List<Fnr>, List<Fnr>> validerFnrs = ValideringsRegler.validerFnrs(fnrs);
            Validation<String, List<Fnr>> veilederForBrukere = arbeidslisteService.erVeilederForBrukere(fnrs);
            if (validerFnrs.isInvalid() || veilederForBrukere.isInvalid()) {
                throw new IllegalStateException(format("%s inneholder ett eller flere ugyldige fødselsnummer", validerFnrs.getError()));
            }

            validerFnrs.get().forEach(fnr -> {
                final int antallRaderSlettet = arbeidslisteService.slettArbeidsliste(fnr);

                final AktorId aktoerId = brukerService.hentAktorId(fnr)
                        .orElse(new AktorId("uten aktør-ID"));

                if (antallRaderSlettet != 1) {
                    feiledeFnrs.add(fnr.get());
                    log.warn("Kunne ikke slette arbeidsliste for bruker {} ", aktoerId.get());
                } else {
                    okFnrs.add(fnr.get());
                    log.info("Arbeidsliste for aktoerid {} slettet", aktoerId.get());
                }
            });

            if (feiledeFnrs.size() == fnrs.size()) {
                throw new IllegalStateException();
            }
            return RestResponse.of(okFnrs, feiledeFnrs);
    }

    private void sjekkTilgangTilEnhet(Fnr fnr) {
        String enhet = brukerService.hentNavKontorFraDbLinkTilArena(fnr).orElseThrow(() -> new IllegalArgumentException("Kunne ikke hente enhet for denne brukeren"));
        authService.tilgangTilEnhet(enhet);
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

    private Arbeidsliste emptyArbeidsliste() {
        return new Arbeidsliste(null, null, null, null, null, null);
    }

    private void validerOppfolgingOgBruker(String fnr) {
        authService.tilgangTilOppfolging();
        Validation<String, Fnr> validateFnr = ValideringsRegler.validerFnr(fnr);
        authService.tilgangTilBruker(fnr);
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
