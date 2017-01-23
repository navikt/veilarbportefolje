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
            @QueryParam("ident") String ident,
            @QueryParam("fra") int fra,
            @QueryParam("antall") int antall,
            @QueryParam("sortByLastName") String sortDirection){

        if(sortDirection.equals("ascending")) {
            portefoljeMock.sortByLastName("ascending");
        } else if(sortDirection.equals("descending")) {
            portefoljeMock.sortByLastName("descending");
        }

        int brukerMockSize = portefoljeMock.getBrukere().size();

        if(antall > brukerMockSize - fra) { antall = brukerMockSize - fra; }

        try {
            Boolean brukerHarTilgangTilEnhet = brukertilgangService.harBrukerTilgangTilEnhet(ident, enhet);
            if(brukerHarTilgangTilEnhet) {
                return Response.ok().entity(new PortefoljeOgEnhet(enhet,
                                            new Portefolje().withBrukere(portefoljeMock.getBrukerFrom(fra, antall)),
                                            brukerMockSize,
                                            antall,
                                            fra)
                                            ).build();
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
        private int antallTotalt;
        private int antallReturnert;
        private int fraIndex;

        PortefoljeOgEnhet(String enhet, Portefolje portefolje, int antallTotalt, int antallReturnet, int fraIndex) {
            this.portefolje = portefolje;
            this.enhet =  enhet;
            this.antallReturnert = portefolje.getBrukere().size();
            this.antallTotalt = antallTotalt;
            this.fraIndex = fraIndex;
        }

        public Portefolje getPortefolje() {
            return portefolje;
        }

        public String getEnhet() {
            return enhet;
        }

        public int getAntallTotalt() { return antallTotalt; }

        public int getAntallReturnert() { return  antallReturnert; }

        public int getFraIndex() { return fraIndex; }

    }
}
