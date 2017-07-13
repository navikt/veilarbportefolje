package no.nav.fo.provider.rest;

import io.swagger.annotations.Api;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import no.nav.fo.domene.*;
import no.nav.fo.exception.RestNotFoundException;
import no.nav.fo.exception.RestTilgangException;
import no.nav.fo.exception.RestValideringException;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteData;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteRequest;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.fo.service.BrukertilgangService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;
import static no.nav.fo.provider.rest.RestUtils.createResponse;
import static no.nav.fo.provider.rest.ValideringsRegler.validerArbeidsliste;

@Api(value = "arbeidsliste")
@Path("/arbeidsliste/")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class ArbeidsListeRessurs {

    private static Logger LOG = LoggerFactory.getLogger(ArbeidsListeRessurs.class);

    @Inject
    private ArbeidslisteService arbeidslisteService;

    @Inject
    private BrukertilgangService brukertilgangService;

    @PUT
    public Response putArbeidsListe(java.util.List<ArbeidslisteRequest> arbeidsliste) {
        Response.ResponseBuilder response = Response.status(201);

        List<String> tilgangErrors =
                List.ofAll(arbeidsliste)
                        .map(bruker -> TilgangsRegler.erVeilederForBruker(arbeidslisteService, bruker.getFnr()))
                        .filter(Validation::isInvalid)
                        .map(Validation::getError);

        if (tilgangErrors.isEmpty()) {
            RestResponse<AktoerId> responseBody =
                    List.ofAll(arbeidsliste)
                            .map(
                                    elem ->
                                            validerArbeidsliste(elem)
                                                    .map(arbeidslisteService::createArbeidsliste)
                                                    .fold(validationErrors(response), data(response))
                            )
                            .reduce(RestResponse::merge);

            return response.entity(responseBody).build();
        }
        RestResponse<Arbeidsliste> body = new RestResponse<>(tilgangErrors.toJavaList(), emptyList());
        return response.status(FORBIDDEN).entity(body).build();
    }

    @GET
    @Path("{fnr}/")
    public Response getArbeidsListe(@PathParam("fnr") String fnr) {
        return createResponse(() -> {

            Validation<String, Fnr> validateFnr = ValideringsRegler.validerFnr(fnr);
            if (validateFnr.isInvalid()) {
                throw new RestValideringException(validateFnr.getError());
            }

            sjekkTilgangTilEnhet(new Fnr(fnr));

            return arbeidslisteService
                    .getArbeidsliste(new ArbeidslisteData(new Fnr(fnr)))
                    .onFailure(e -> LOG.warn("Kunne ikke hente arbeidsliste: {}", e.getMessage()))
                    .getOrElseThrow(() -> new RestNotFoundException("Kunne ikke finne arbeidsliste for bruker"));
        });
    }

    @PUT
    @Path("{fnr}/")
    public Response putArbeidsListe(ArbeidslisteRequest body, @PathParam("fnr") String fnr) {
        return createResponse(() -> {

            Validation<String, Fnr> validateFnr = ValideringsRegler.validerFnr(fnr);
            if (validateFnr.isInvalid()) {
                throw new RestValideringException(validateFnr.getError());
            }

            Validation<String, Fnr> validateVeileder = TilgangsRegler.erVeilederForBruker(arbeidslisteService, fnr);
            if (validateVeileder.isInvalid()) {
                throw new RestTilgangException(validateVeileder.getError());
            }

            return arbeidslisteService
                    .createArbeidsliste(data(body, new Fnr(fnr)))
                    .map(x -> "Arbeidsliste opprettet.")
                    .onFailure(e -> LOG.warn("Kunne ikke opprette arbeidsliste: {}", e.getMessage()))
                    .getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);
        }, CREATED);
    }

    @POST
    @Path("{fnr}/")
    public Response postArbeidsListe(ArbeidslisteRequest body, @PathParam("fnr") String fnr) {
        return createResponse(() -> {

            Validation<String, Fnr> validateFnr = ValideringsRegler.validerFnr(fnr);
            if (validateFnr.isInvalid()) {
                throw new RestValideringException(validateFnr.getError());
            }

            sjekkTilgangTilEnhet(new Fnr(fnr));

            return arbeidslisteService
                    .updateArbeidsliste(data(body, new Fnr(fnr)))
                    .map(x -> "Arbeidsliste oppdatert.")
                    .onFailure(e -> LOG.warn("Kunne ikke oppdatere arbeidsliste: {}", e.getMessage()))
                    .getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);
        });
    }

    @DELETE
    @Path("{fnr}/")
    public Response deleteArbeidsliste(@PathParam("fnr") String fnr) {
        return createResponse(() -> {

            Validation<String, Fnr> validateFnr = ValideringsRegler.validerFnr(fnr);
            if (validateFnr.isInvalid()) {
                throw new RestValideringException(validateFnr.getError());
            }

            Validation<String, Fnr> validateVeileder = TilgangsRegler.erVeilederForBruker(arbeidslisteService, fnr);
            if (validateVeileder.isInvalid()) {
                throw new RestTilgangException(validateVeileder.getError());
            }

            return arbeidslisteService
                    .deleteArbeidsliste(new Fnr(fnr))
                    .map(x -> "Arbeidsliste slettet")
                    .getOrElseThrow(() -> new RestNotFoundException("Kunne ikke slette. Fant ikke arbeidsliste for bruker"));
        });
    }

    private void sjekkTilgangTilEnhet(Fnr fnr) {
        String enhet = arbeidslisteService.hentEnhet(fnr);
        TilgangsRegler.tilgangTilEnhet(brukertilgangService, enhet);
    }

    private ArbeidslisteData data(ArbeidslisteRequest body, Fnr fnr) {
        return new ArbeidslisteData(fnr)
                .setVeilederId(new VeilederId(body.getVeilederId()))
                .setKommentar(body.getKommentar())
                .setFrist(Timestamp.from(Instant.parse(body.getFrist())));
    }

    private Function<Try<AktoerId>, RestResponse<AktoerId>> data(Response.ResponseBuilder response) {
        return data -> {
            if (data.isFailure()) {
                response.status(NOT_FOUND);
                return new RestResponse<>(singletonList(data.getCause().getMessage()), emptyList());
            }
            return RestResponse.of(data.get());
        };
    }

    private Function<Seq<String>, RestResponse<AktoerId>> validationErrors(Response.ResponseBuilder response) {
        return validationErr -> {
            response.status(BAD_REQUEST);
            return new RestResponse<>(validationErr.toJavaList(), emptyList());
        };
    }
}
