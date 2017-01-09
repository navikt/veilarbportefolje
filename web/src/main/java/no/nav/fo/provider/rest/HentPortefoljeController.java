package no.nav.fo.provider.rest;

import no.nav.fo.domene.Portefolje;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/hentportefolje")
@Produces(APPLICATION_JSON)
public class HentPortefoljeController {

    @Inject
    Portefolje portefoljeMock;


    @GET
    @Path("/{enhet}")
    public Response hentPortefolje(@PathParam("enhet") String enhet) {

    return Response.ok().entity(new PortefoljeOgEnhet(enhet, portefoljeMock)).build();
    }

    private class PortefoljeOgEnhet {
        private Portefolje portefolje;
        private String enhet;

        PortefoljeOgEnhet(String enhet, Portefolje portefolje) {
            this.portefolje = portefolje;
            this.enhet =  enhet;
        }

        public Portefolje getPortefolje() {
            return portefolje;
        }

        public String getEnhet() {
            return enhet;
        }

    }


}
