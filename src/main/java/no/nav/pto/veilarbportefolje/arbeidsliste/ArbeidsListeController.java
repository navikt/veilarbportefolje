package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import io.vavr.control.Validation;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.pdl.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.RestResponse;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.service.BrukerService;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static no.nav.common.utils.StringUtils.nullOrEmpty;
import static no.nav.pto.veilarbportefolje.util.ValideringsRegler.validerArbeidsliste;

@Slf4j
@RestController
@RequestMapping("/api/arbeidsliste")
public class ArbeidsListeController {
    private final ArbeidslisteService arbeidslisteService;
    private final BrukerService brukerService;
    private final AktorOppslagClient aktorOppslagClient;
    private final AuthService authService;

    @Autowired
    public ArbeidsListeController(
            ArbeidslisteService arbeidslisteService,
            BrukerService brukerService,
            AktorOppslagClient aktorOppslagClient, AuthService authService
    ) {
        this.arbeidslisteService = arbeidslisteService;
        this.brukerService = brukerService;
        this.aktorOppslagClient = aktorOppslagClient;
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

        Fnr fnr = new Fnr(fnrString);
        Try<AktorId> aktoerId = Try.of(()-> aktorOppslagClient.hentAktorId(fnr));

        boolean harVeilederTilgang = brukerService.hentNavKontorFraDbLinkTilArena(fnr)
                .map(enhet -> authService.harVeilederTilgangTilEnhet(innloggetVeileder, enhet))
                .orElse(false);

        Arbeidsliste arbeidsliste = aktoerId
                .flatMap(arbeidslisteService::getArbeidsliste)
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
        sjekkTilgangTilEnhet(new Fnr(fnr));

        arbeidslisteService.createArbeidsliste(data(body, new Fnr(fnr)))
                .onFailure(e -> log.warn("Kunne ikke opprette arbeidsliste: {}", e.getMessage()))
                .getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);

        Arbeidsliste arbeidsliste = arbeidslisteService.getArbeidsliste(new Fnr(fnr)).get()
                .setHarVeilederTilgang(true)
                .setIsOppfolgendeVeileder(true);

        return arbeidsliste;
    }

    @PutMapping("{fnr}")
    public Arbeidsliste oppdaterArbeidsListe(@RequestBody ArbeidslisteRequest body, @PathVariable("fnr") String fnrString) {
        validerOppfolgingOgBruker(fnrString);
        Fnr fnr = new Fnr(fnrString);
        sjekkTilgangTilEnhet(fnr);
        validerArbeidsliste(body, true);

        arbeidslisteService
                .updateArbeidsliste(data(body, fnr))
                .onFailure(e -> log.warn("Kunne ikke oppdatere arbeidsliste: {}", e.getMessage()))
                .getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);

        return arbeidslisteService.getArbeidsliste(fnr).get()
                .setHarVeilederTilgang(true)
                .setIsOppfolgendeVeileder(arbeidslisteService.erVeilederForBruker(
                        fnr,
                        AuthUtils.getInnloggetVeilederIdent()));
    }

    @DeleteMapping("{fnr}")
    public Arbeidsliste deleteArbeidsliste(@PathVariable("fnr") String fnr) {
        validerOppfolgingOgBruker(fnr);
        validerErVeilederForBruker(fnr);
        sjekkTilgangTilEnhet(new Fnr(fnr));

        final int antallRaderSlettet = arbeidslisteService.slettArbeidsliste(new Fnr(fnr));
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
                    .map(data -> new Fnr(data.getFnr()))
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
                    feiledeFnrs.add(fnr.toString());
                    log.warn("Kunne ikke slette arbeidsliste for bruker {} ", aktoerId.toString());
                } else {
                    okFnrs.add(fnr.toString());
                    log.info("Arbeidsliste for aktoerid {} slettet", aktoerId.toString());
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
