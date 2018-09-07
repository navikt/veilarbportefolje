package no.nav.fo.provider.rest;

import io.swagger.annotations.Api;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.domene.*;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteData;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteRequest;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.fo.service.PepClient;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;
import static no.nav.apiapp.util.StringUtils.nullOrEmpty;
import static no.nav.fo.provider.rest.RestUtils.createResponse;
import static no.nav.fo.provider.rest.ValideringsRegler.validerArbeidsliste;

@Slf4j
@Api(value = "arbeidsliste")
@Path("/arbeidsliste/")
@Component
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class ArbeidsListeRessurs {

    @Inject
    private ArbeidslisteService arbeidslisteService;

    @Inject
    private AktoerService aktoerService;

    @Inject
    private PepClient pepClient;

    @POST
    public Response opprettArbeidsListe(java.util.List<ArbeidslisteRequest> arbeidsliste) {
        TilgangsRegler.tilgangTilOppfolging(pepClient);
        List<String> tilgangErrors = getTilgangErrors(arbeidsliste);
        if (tilgangErrors.length() > 0) {
            return RestResponse.of(tilgangErrors.toJavaList()).forbidden();
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

    @GET
    @Path("{fnr}/")
    public Arbeidsliste getArbeidsListe(@PathParam("fnr") String fnrString) {
        validerOppfolgingOgBruker(fnrString);

        String innloggetVeileder = SubjectHandler.getSubjectHandler().getUid();

        Fnr fnr = new Fnr(fnrString);
        Try<AktoerId> aktoerId = aktoerService.hentAktoeridFraFnr(fnr);

        boolean harVeilederTilgang = arbeidslisteService.hentEnhet(fnr)
                .map(enhet -> pepClient.tilgangTilEnhet(innloggetVeileder, enhet))
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

    @POST
    @Path("{fnr}/")
    public Response opprettArbeidsListe(ArbeidslisteRequest body, @PathParam("fnr") String fnr) {
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

    @PUT
    @Path("{fnr}/")
    public Arbeidsliste oppdaterArbeidsListe(ArbeidslisteRequest body, @PathParam("fnr") String fnrString) {
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
                        VeilederId.of(SubjectHandler.getSubjectHandler().getUid())));
    }

    @DELETE
    @Path("{fnr}/")
    public Arbeidsliste deleteArbeidsliste(@PathParam("fnr") String fnr) {
        validerOppfolgingOgBruker(fnr);
        validerErVeilederForBruker(fnr);
        sjekkTilgangTilEnhet(new Fnr(fnr));

        return arbeidslisteService
                .deleteArbeidsliste(new Fnr(fnr))
                .map((a) -> emptyArbeidsliste().setHarVeilederTilgang(true).setIsOppfolgendeVeileder(true))
                .getOrElseThrow(() -> new WebApplicationException("Kunne ikke slette. Fant ikke arbeidsliste for bruker", BAD_REQUEST));
    }

    @POST
    @Path("/delete")
    public Response deleteArbeidslisteListe(java.util.List<ArbeidslisteRequest> arbeidslisteData) {
        return createResponse(() -> {
            TilgangsRegler.tilgangTilOppfolging(pepClient);

            java.util.List<String> feiledeFnrs = new ArrayList<>();
            java.util.List<String> okFnrs = new ArrayList<>();

            java.util.List<Fnr> fnrs = arbeidslisteData
                    .stream()
                    .map(data -> new Fnr(data.getFnr()))
                    .collect(Collectors.toList());

            Validation<java.util.List<Fnr>, java.util.List<Fnr>> validerFnrs = ValideringsRegler.validerFnrs(fnrs);
            Validation<String, java.util.List<Fnr>> veilederForBrukere = TilgangsRegler.erVeilederForBrukere(arbeidslisteService, fnrs);
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
        TilgangsRegler.tilgangTilEnhet(pepClient, enhet);
    }

    private ArbeidslisteData data(ArbeidslisteRequest body, Fnr fnr) {
        Timestamp frist = nullOrEmpty(body.getFrist()) ? null : Timestamp.from(Instant.parse(body.getFrist()));
        return new ArbeidslisteData(fnr)
                .setVeilederId(VeilederId.of(SubjectHandler.getSubjectHandler().getUid()))
                .setOverskrift(body.getOverskrift())
                .setKommentar(body.getKommentar())
                .setFrist(frist);
    }

    private List<String> getTilgangErrors(java.util.List<ArbeidslisteRequest> arbeidsliste) {
        return List.ofAll(arbeidsliste)
                .map(bruker -> TilgangsRegler.erVeilederForBruker(arbeidslisteService, bruker.getFnr()))
                .filter(Validation::isInvalid)
                .map(Validation::getError);
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
        return new Arbeidsliste(null, null, null, null, null);
    }

    private void validerOppfolgingOgBruker(String fnr) {
        TilgangsRegler.tilgangTilOppfolging(pepClient);
        Validation<String, Fnr> validateFnr = ValideringsRegler.validerFnr(fnr);
        TilgangsRegler.tilgangTilBruker(pepClient, fnr);
        if (validateFnr.isInvalid()) {
            throw new BadRequestException(validateFnr.getError());
        }
    }

    private void validerErVeilederForBruker(String fnr) {
        Validation<String, Fnr> validateVeileder = TilgangsRegler.erVeilederForBruker(arbeidslisteService, fnr);
        if (validateVeileder.isInvalid()) {
            throw new ForbiddenException(validateVeileder.getError());
        }
    }
}
