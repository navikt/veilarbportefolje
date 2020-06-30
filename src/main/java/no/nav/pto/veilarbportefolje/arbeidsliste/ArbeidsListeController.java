package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import io.vavr.control.Validation;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.ValideringsRegler;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.RestResponse;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;
import static no.nav.common.utils.StringUtils.nullOrEmpty;
import static no.nav.pto.veilarbportefolje.util.RestUtils.createResponse;
import static no.nav.pto.veilarbportefolje.auth.ValideringsRegler.validerArbeidsliste;

@Slf4j
@RestController
@RequestMapping("/api/arbeidsliste")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class ArbeidsListeController {
    private final ArbeidslisteService arbeidslisteService;
    private final AktoerService aktoerService;
    private final AuthService authService;

    @Autowired
    public ArbeidsListeController (
            ArbeidslisteService arbeidslisteService,
            AktoerService aktoerService,
            AuthService authService
    ) {
        this.arbeidslisteService = arbeidslisteService;
        this.aktoerService = aktoerService;
        this.authService = authService;

    }


    @PostMapping
    public Response opprettArbeidsListe(List<ArbeidslisteRequest> arbeidsliste) {
        authService.tilgangTilOppfolging();
        List<String> tilgangErrors = getTilgangErrors(arbeidsliste);
        if (tilgangErrors.size() > 0) {
            return RestResponse.of(tilgangErrors).forbidden();
        }

        RestResponse<String> response = new RestResponse<>(new ArrayList<>(), new ArrayList<>());

        arbeidsliste.forEach(request -> {
            RestResponse<AktoerId> opprettArbeidsliste = opprettArbeidsliste(request);
            if (opprettArbeidsliste.containsError()) {
                response.addError(request.getFnr());
            } else {
                response.addData(request.getFnr());
            }
        });

        return response.data.isEmpty() ? response.badRequest() : response.created();
    }

    @GetMapping("{fnr}/")
    public Arbeidsliste getArbeidsListe(@PathVariable("fnr") String fnrString) {
        validerOppfolgingOgBruker(fnrString);

        String innloggetVeileder = authService.getInnloggetVeilederIdent().getVeilederId();

        Fnr fnr = new Fnr(fnrString);
        Try<AktoerId> aktoerId = aktoerService.hentAktoeridFraFnr(fnr);

        boolean harVeilederTilgang = arbeidslisteService.hentEnhet(fnr)
                .map(enhet -> authService.harVeilederTilgangTilEnhet(innloggetVeileder, enhet))
                .getOrElse(false);

        Arbeidsliste arbeidsliste = aktoerId
                .flatMap(arbeidslisteService::getArbeidsliste)
                .toJavaOptional()
                .orElse(emptyArbeidsliste())
                .setIsOppfolgendeVeileder(aktoerId.map(id ->
                        arbeidslisteService.erVeilederForBruker(id, VeilederId.of(innloggetVeileder))).get())
                .setHarVeilederTilgang(harVeilederTilgang);

        return harVeilederTilgang ? arbeidsliste : emptyArbeidsliste().setHarVeilederTilgang(false);
    }

    @PostMapping("{fnr}/")
    public Response opprettArbeidsListe(ArbeidslisteRequest body, @PathVariable("fnr") String fnr) {
        validerOppfolgingOgBruker(fnr);
        validerErVeilederForBruker(fnr);
        sjekkTilgangTilEnhet(new Fnr(fnr));

        arbeidslisteService.createArbeidsliste(data(body, new Fnr(fnr)))
                .onFailure(e -> log.warn("Kunne ikke opprette arbeidsliste: {}", e.getMessage()))
                .getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);

        Arbeidsliste arbeidsliste = arbeidslisteService.getArbeidsliste(new Fnr(fnr)).get()
                .setHarVeilederTilgang(true)
                .setIsOppfolgendeVeileder(true);
        return Response.status(CREATED).entity(arbeidsliste).build();
    }

    @PutMapping("{fnr}/")
    public Arbeidsliste oppdaterArbeidsListe(ArbeidslisteRequest body, @PathVariable("fnr") String fnrString) {
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
                        authService.getInnloggetVeilederIdent()));
    }

    @DeleteMapping("{fnr}/")
    public Arbeidsliste deleteArbeidsliste(@PathVariable("fnr") String fnr) {
        validerOppfolgingOgBruker(fnr);
        validerErVeilederForBruker(fnr);
        sjekkTilgangTilEnhet(new Fnr(fnr));

        return arbeidslisteService
                .deleteArbeidsliste(new Fnr(fnr))
                .map((a) -> emptyArbeidsliste().setHarVeilederTilgang(true).setIsOppfolgendeVeileder(true))
                .getOrElseThrow(() -> new WebApplicationException("Kunne ikke slette. Fant ikke arbeidsliste for bruker", BAD_REQUEST));
    }

    @PostMapping("/delete")
    public Response deleteArbeidslisteListe(java.util.List<ArbeidslisteRequest> arbeidslisteData) {
        return createResponse(() -> {
            authService.tilgangTilOppfolging();

            java.util.List<String> feiledeFnrs = new ArrayList<>();
            java.util.List<String> okFnrs = new ArrayList<>();

            java.util.List<Fnr> fnrs = arbeidslisteData
                    .stream()
                    .map(data -> new Fnr(data.getFnr()))
                    .collect(Collectors.toList());

            Validation<List<Fnr>, List<Fnr>> validerFnrs = ValideringsRegler.validerFnrs(fnrs);
            Validation<String, List<Fnr>> veilederForBrukere = authService.erVeilederForBrukere(arbeidslisteService, fnrs);
            if (validerFnrs.isInvalid() || veilederForBrukere.isInvalid()) {
                throw new BadRequestException(format("%s inneholder ett eller flere ugyldige fødselsnummer", validerFnrs.getError()));
            }

            validerFnrs.get()
                    .forEach((fnr) -> arbeidslisteService
                            .deleteArbeidsliste(fnr)
                            .onSuccess((aktoerid) -> {
                                okFnrs.add(fnr.toString());
                                log.info("Arbeidsliste for aktoerid {} slettet", aktoerid);
                            })
                            .onFailure((error) -> {
                                feiledeFnrs.add(fnr.toString());
                                log.warn("Kunne ikke slette arbeidsliste", error);
                            })
                    );

            if (feiledeFnrs.size() == fnrs.size()) {
                throw new InternalServerErrorException();
            }
            return RestResponse.of(okFnrs, feiledeFnrs);
        });
    }

    private void sjekkTilgangTilEnhet(Fnr fnr) {
        String enhet = arbeidslisteService.hentEnhet(fnr).getOrElseThrow(x -> new WebApplicationException("Kunne ikke hente enhet for denne brukeren", BAD_GATEWAY));
        authService.tilgangTilEnhet(enhet);
    }

    private ArbeidslisteDTO data(ArbeidslisteRequest body, Fnr fnr) {
        Timestamp frist = nullOrEmpty(body.getFrist()) ? null : Timestamp.from(Instant.parse(body.getFrist()));
        return new ArbeidslisteDTO(fnr)
                .setVeilederId(authService.getInnloggetVeilederIdent())
                .setOverskrift(body.getOverskrift())
                .setKommentar(body.getKommentar())
                .setKategori(Arbeidsliste.Kategori.valueOf(body.getKategori()))
                .setFrist(frist);
    }

    private List<String> getTilgangErrors(List<ArbeidslisteRequest> arbeidsliste) {
        return arbeidsliste
                .stream()
                .map(bruker -> authService.erVeilederForBruker(arbeidslisteService, bruker.getFnr()))
                .filter(Validation::isInvalid)
                .map(Validation::getError)
                .collect(Collectors.toList());
    }

    private RestResponse<AktoerId> opprettArbeidsliste(ArbeidslisteRequest arbeidslisteRequest) {
        return validerArbeidsliste(arbeidslisteRequest, false)
                .map(arbeidslisteService::createArbeidsliste)
                .fold(
                        validationErr -> RestResponse.of(validationErr.toJavaList()),
                        result -> {
                            if (result.isFailure()) {
                                return RestResponse.of(result.getCause().getMessage());
                            }
                            return RestResponse.of(result.get());

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
            throw new BadRequestException(validateFnr.getError());
        }
    }

    private void validerErVeilederForBruker(String fnr) {
        Validation<String, Fnr> validateVeileder = authService.erVeilederForBruker(arbeidslisteService, fnr);
        if (validateVeileder.isInvalid()) {
            throw new ForbiddenException(validateVeileder.getError());
        }
    }
}
