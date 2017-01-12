package no.nav.fo.provider.rest;

import no.nav.fo.domene.Portefolje;
import no.nav.fo.service.BrukertilgangService;
import no.nav.virksomhet.tjenester.enhet.v1.HentEnhetListeRessursIkkeFunnet;
import no.nav.virksomhet.tjenester.enhet.v1.HentEnhetListeUgyldigInput;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;

@Path("/hentportefoljeforenhet")
@Produces(APPLICATION_JSON)
public class HentPortefoljeForEnhetController {

    @Inject
    Portefolje portefoljeMock;

    @Inject
    BrukertilgangService brukertilgangService;

    @GET
    @Path("/{enhet}")
    public Response hentPortefolje(
            @PathParam("enhet") String enhet,
            @QueryParam("ident") String ident){


        try {
            Boolean brukerHarTilgangTilEnhet = brukertilgangService.harBrukerTilgangTilEnhet(ident, enhet);
            if(brukerHarTilgangTilEnhet) {
                return Response.ok().entity(new PortefoljeOgEnhet(enhet, portefoljeMock)).build();
            } else {
                return Response.status(FORBIDDEN).build();
            }
        } catch (HentEnhetListeUgyldigInput e) {
            return Response.status(BAD_REQUEST).build();
        } catch(HentEnhetListeRessursIkkeFunnet e) {
            return Response.status(NOT_FOUND).build();
        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
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
