package no.nav.fo.provider.rest;

import io.swagger.annotations.Api;
import no.nav.fo.domene.Fnr;
import no.nav.fo.exception.RestNotFoundException;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteData;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteRequest;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.fo.service.BrukertilgangService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.time.Instant;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static no.nav.fo.provider.rest.RestUtils.createResponse;

@Api(value = "arbeidsliste")
@Path("/arbeidsliste/{fnr}")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class ArbeidsListeRessurs {

    @Inject
    private ArbeidslisteService arbeidslisteService;

    @Inject
    private BrukertilgangService brukertilgangService;

    @GET
    public Response getArbeidsListe(@PathParam("fnr") String fnr) {
        return createResponse(() -> {
            ValideringsRegler.sjekkFnr(fnr);
            sjekkTilgangTilEnhet(new Fnr(fnr));

            return arbeidslisteService
                    .getArbeidsliste(new ArbeidslisteData().setFnr(new Fnr(fnr)))
                    .getOrElseThrow(() -> new RestNotFoundException("Kunne ikke opprette. Fant ikke arbeidsliste for bruker"));
        });
    }


    @PUT
    public Response putArbeidsListe(ArbeidslisteRequest body, @PathParam("fnr") String fnr) {
        return createResponse(() -> {
            ValideringsRegler.sjekkFnr(fnr);
            TilgangsRegler.erVeilederForBruker(arbeidslisteService, new Fnr(fnr));

            return arbeidslisteService
                    .createArbeidsliste(data(body, new Fnr(fnr)))
                    .map(x -> "Arbeidsliste opprettet.")
                    .getOrElseThrow(() -> new RuntimeException("Kunne ikke opprette arbeidsliste"));
        }, CREATED);
    }

    @POST
    public Response postArbeidsListe(ArbeidslisteRequest body, @PathParam("fnr") String fnr) {
        return createResponse(() -> {
            ValideringsRegler.sjekkFnr(fnr);
            sjekkTilgangTilEnhet(new Fnr(fnr));

            return arbeidslisteService
                    .updateArbeidsliste(data(body, new Fnr(fnr)))
                    .map(x -> "Arbeidsliste oppdatert.")
                    .getOrElseThrow(() -> new RuntimeException("Kunne ikke oppdatere ny arbeidsliste"));
        });
    }

    @DELETE
    public Response deleteArbeidsliste(@PathParam("fnr") String fnr) {
        return createResponse(() -> {
            ValideringsRegler.sjekkFnr(fnr);
            TilgangsRegler.erVeilederForBruker(arbeidslisteService, new Fnr(fnr));

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
        return new ArbeidslisteData()
                .setFnr(fnr)
                .setVeilederId(body.getVeilederId())
                .setKommentar(body.getKommentar())
                .setFrist(Timestamp.from(Instant.parse(body.getFrist())));
    }
}
