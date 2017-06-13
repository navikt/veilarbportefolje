package no.nav.fo.provider.rest.arbeidsliste;

import io.swagger.annotations.Api;
import no.nav.fo.domene.Arbeidsliste;
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
public class ArbeidsListeController {

    @Inject
    private ArbeidslisteService arbeidslisteService;

    @GET
    public Response getArbeidsListe(@PathParam("fnr") String fnr) {
        Arbeidsliste arbeidsliste = arbeidslisteService
                .getArbeidsliste(fnr)
                .orElseThrow(ArbeidslisteNotFoundException::new);

        return Response.ok().entity(arbeidsliste).build();
    }

    @PUT
    public Response putArbeidsListe(ArbeidslisteRequest body, @PathParam("fnr") String fnr) {
        arbeidslisteService
                .createArbeidsliste(fnr, body.kommentar, body.frist)
                .orElseThrow(WebApplicationException::new);

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
