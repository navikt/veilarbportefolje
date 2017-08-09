package no.nav.fo.provider.rest;

import io.swagger.annotations.Api;
import io.vavr.collection.List;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.*;
import no.nav.fo.exception.*;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteData;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteRequest;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.fo.service.BrukertilgangService;
import no.nav.fo.service.PepClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
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

    @Inject
    private BrukerRepository brukerRepository;

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

        arbeidsliste.forEach( request -> {
            RestResponse<AktoerId> opprettArbeidsliste = opprettArbeidsliste(request);
            if(opprettArbeidsliste.containsError()) {
                response.addError(request.getFnr());
            } else {
                response.addData(request.getFnr());
            }
        });

        return response.data.isEmpty() ? response.badRequest() : response.created();
    }

    @GET
    @Path("{fnr}/")
    public Arbeidsliste getArbeidsListe(@PathParam("fnr") String fnr) {
        TilgangsRegler.tilgangTilOppfolging(pepClient);
        Validation<String, Fnr> validateFnr = ValideringsRegler.validerFnr(fnr);
        if (validateFnr.isInvalid()) {
            throw new RestValideringException(validateFnr.getError());
        }

        String innloggetVeileder = SubjectHandler.getSubjectHandler().getUid();

        Fnr newFnr = new Fnr(fnr);
        Try<AktoerId> aktoerId = aktoerService.hentAktoeridFraFnr(newFnr);

        boolean erOppfolgendeVeileder = aktoerId.flatMap(brukerRepository::retrieveVeileder)
                .map(Object::toString)
                .map(v -> Objects.equals(innloggetVeileder, v))
                .getOrElse(false);

        boolean harVeilederTilgang = arbeidslisteService.hentEnhet(newFnr)
                .map(enhet -> brukertilgangService.harBrukerTilgang(innloggetVeileder, enhet))
                .getOrElse(false);

        if (harVeilederTilgang) {
            return aktoerId
                    .flatMap(arbeidslisteService::getArbeidsliste)
                    .map(arbeidsliste -> arbeidsliste.setIsOppfolgendeVeileder(erOppfolgendeVeileder).setHarVeilederTilgang(true))
                    .onFailure(e -> LOG.warn("Kunne ikke hente arbeidsliste: {}", e.getMessage()))
                    .getOrElseThrow(() -> new RestNoContentException("Kunne ikke finne arbeidsliste for bruker"));
        } else {
            return new Arbeidsliste(null, null, null, null).setHarVeilederTilgang(false);
        }
    }

    @POST
    @Path("{fnr}/")
    public Response opprettArbeidsListe(ArbeidslisteRequest body, @PathParam("fnr") String fnr) {
        return createResponse(() -> {
            TilgangsRegler.tilgangTilOppfolging(pepClient);
            Validation<String, Fnr> validateFnr = ValideringsRegler.validerFnr(fnr);
            if (validateFnr.isInvalid()) {
                throw new RestValideringException(validateFnr.getError());
            }

            Validation<String, Fnr> validateVeileder = TilgangsRegler.erVeilederForBruker(arbeidslisteService, fnr);
            if (validateVeileder.isInvalid()) {
                throw new RestTilgangException(validateVeileder.getError());
            }

            sjekkTilgangTilEnhet(new Fnr(fnr));

            arbeidslisteService.createArbeidsliste(data(body, new Fnr(fnr)))
                    .onFailure(e -> LOG.warn("Kunne ikke opprette arbeidsliste: {}", e.getMessage()))
                    .getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);

            return arbeidslisteService.getArbeidsliste(new Fnr(fnr)).get();
        }, CREATED);
    }

    @PUT
    @Path("{fnr}/")
    public Response oppdaterArbeidsListe(ArbeidslisteRequest body, @PathParam("fnr") String fnr) {
        return createResponse(() -> {
            TilgangsRegler.tilgangTilOppfolging(pepClient);
            Validation<String, Fnr> validateFnr = ValideringsRegler.validerFnr(fnr);
            if (validateFnr.isInvalid()) {
                throw new RestValideringException(validateFnr.getError());
            }

            sjekkTilgangTilEnhet(new Fnr(fnr));

            arbeidslisteService
                    .updateArbeidsliste(data(body, new Fnr(fnr)))
                    .onFailure(e -> LOG.warn("Kunne ikke oppdatere arbeidsliste: {}", e.getMessage()))
                    .getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);

            return arbeidslisteService.getArbeidsliste(new Fnr(fnr)).get();
        });
    }

    @DELETE
    @Path("{fnr}/")
    public Response deleteArbeidsliste(@PathParam("fnr") String fnr) {
        return createResponse(() -> {
            TilgangsRegler.tilgangTilOppfolging(pepClient);
            Validation<String, Fnr> validateFnr = ValideringsRegler.validerFnr(fnr);
            if (validateFnr.isInvalid()) {
                throw new RestValideringException(validateFnr.getError());
            }

        Validation<String, Fnr> validateVeileder = TilgangsRegler.erVeilederForBruker(arbeidslisteService, fnr);
        if (validateVeileder.isInvalid()) {
            throw new RestTilgangException(validateVeileder.getError());
        }

        sjekkTilgangTilEnhet(new Fnr(fnr));

            return arbeidslisteService
                    .deleteArbeidsliste(new Fnr(fnr))
                    .getOrElseThrow(() -> new WebApplicationException("Kunne ikke slette. Fant ikke arbeidsliste for bruker", BAD_REQUEST));
        });
    }

    @POST
    @Path("/delete")
    public Response deleteArbeidsliseListe(java.util.List<ArbeidslisteRequest> arbeidslisteData) {
        return createResponse(() -> {
            TilgangsRegler.tilgangTilOppfolging(pepClient);
            java.util.List<String> feiledeFnrs = new ArrayList<>();
            java.util.List<String> okFnrs = new ArrayList<>();

            java.util.List<Fnr> fnrs = arbeidslisteData
                    .stream()
                    .map(data -> new Fnr(data.getFnr()))
                    .collect(Collectors.toList());

            Validation<java.util.List<Fnr>, java.util.List<Fnr>> validerFnrs = ValideringsRegler.validerFnrs(fnrs);
            if (validerFnrs.isInvalid()) {
                throw new RestValideringException(format("%s inneholder ett eller flere ugyldige fødselsnummer", validerFnrs.getError()));
            }

            validerFnrs.get()
                    .forEach( (fnr) -> arbeidslisteService
                            .deleteArbeidsliste(fnr)
                            .onSuccess( (aktoerid) -> {
                                okFnrs.add(fnr.toString());
                                LOG.info("Arbeidsliste for aktoerid {} slettet", aktoerid);
                            })
                            .onFailure( (error) -> {
                                feiledeFnrs.add(fnr.toString());
                                LOG.warn("Kunne ikke slette arbeidsliste for fnr {}", fnr.toString(), error);
                            })
                            );

            if(feiledeFnrs.size() == fnrs.size()) {
                throw new InternalServerErrorException();
            }
            return RestResponse.of(okFnrs, feiledeFnrs);
        });
    }

    private void sjekkTilgangTilEnhet(Fnr fnr) {
        String enhet = arbeidslisteService.hentEnhet(fnr).getOrElseThrow(x -> new RestBadGateWayException("Kunne ikke hente enhet for denne brukeren"));
        TilgangsRegler.tilgangTilEnhet(brukertilgangService, enhet);
    }

    private ArbeidslisteData data(ArbeidslisteRequest body, Fnr fnr) {
        return new ArbeidslisteData(fnr)
                .setVeilederId(new VeilederId(SubjectHandler.getSubjectHandler().getUid()))
                .setKommentar(body.getKommentar())
                .setFrist(Timestamp.from(Instant.parse(body.getFrist())));
    }

    private List<String> getTilgangErrors(java.util.List<ArbeidslisteRequest> arbeidsliste) {
        return List.ofAll(arbeidsliste)
                .map(bruker -> TilgangsRegler.erVeilederForBruker(arbeidslisteService, bruker.getFnr()))
                .filter(Validation::isInvalid)
                .map(Validation::getError);
    }

    private RestResponse<AktoerId> opprettArbeidsliste(ArbeidslisteRequest bruker) {
        return validerArbeidsliste(bruker)
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
}
