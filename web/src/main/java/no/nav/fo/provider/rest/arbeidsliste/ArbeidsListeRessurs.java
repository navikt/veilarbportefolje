package no.nav.fo.provider.rest.arbeidsliste;

import io.swagger.annotations.Api;
import no.nav.fo.domene.Arbeidsliste;
import no.nav.fo.domene.Fnr;
import no.nav.fo.provider.rest.arbeidsliste.exception.ArbeidslisteIkkeFunnetException;
import no.nav.fo.provider.rest.arbeidsliste.exception.ArbeidslisteIkkeOpprettetException;
import no.nav.fo.service.ArbeidslisteService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;

@Api(value = "arbeidsliste")
@Path("/arbeidsliste/{fnr}")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class ArbeidsListeRessurs {

    @Inject
    private ArbeidslisteService arbeidslisteService;

    @GET
    public Response getArbeidsListe(@PathParam("fnr") String fnr) {
        Arbeidsliste arbeidsliste = arbeidslisteService
                .getArbeidsliste(fnr)
                .orElseThrow(ArbeidslisteIkkeFunnetException::new);

        return Response.ok().entity(arbeidsliste).build();
    }

    @PUT
    public Response putArbeidsListe(ArbeidslisteRequest body, @PathParam("fnr") String fnr) {

        ArbeidslisteUpdate updateData =
                new ArbeidslisteUpdate(new Fnr(fnr))
                        .setVeilederId(body.veilederId)
                        .setKommentar(body.kommentar)
                        .setFrist(body.frist);

        arbeidslisteService
                .createArbeidsliste(updateData)
                .orElseThrow(ArbeidslisteIkkeOpprettetException::new);

        return Response.status(CREATED).build();
    }

    @POST
    public Response postArbeidsListe(ArbeidslisteRequest body, @PathParam("fnr") String fnr) {
        return Response.ok().build();
    }

    @DELETE
    public Response deleteArbeidsliste(@PathParam("fnr") String fnr) {
        return Response.ok().build();
    }

}
