package no.nav.fo.provider.rest;

import no.nav.fo.domene.Bruker;
import no.nav.fo.domene.Portefolje;
import no.nav.fo.service.BrukertilgangService;
import no.nav.fo.service.SolrService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_GATEWAY;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

@Path("/enhet")
@Produces(APPLICATION_JSON)
public class PortefoljeController {

    @Inject
    BrukertilgangService brukertilgangService;

    @Inject
    SolrService solrService;

    @GET
    @Path("/{enhet}/portefolje")
    public Response hentPortefolje(
            @PathParam("enhet") String enhet,
            @QueryParam("ident") String ident,
            @QueryParam("fra") int fra,
            @QueryParam("antall") int antall,
            @QueryParam("sortByLastName") String sortDirection) {

        try {
            boolean brukerHarTilgangTilEnhet = brukertilgangService.harBrukerTilgang(ident, enhet);

            if (brukerHarTilgangTilEnhet) {

                List<Bruker> brukere = solrService.hentBrukere(enhet, sortDirection);
                List<Bruker> brukereSublist = brukere.stream().skip(fra).limit(antall).collect(toList());

                Portefolje portefolje = new Portefolje()
                        .setEnhet(enhet)
                        .setBrukere(brukereSublist)
                        .setAntallTotalt(brukere.size())
                        .setAntallReturnert(brukereSublist.size())
                        .setFraIndex(fra);

                return Response.ok().entity(portefolje).build();
            } else {
                return Response.status(FORBIDDEN).build();
            }
        } catch (Exception e) {
            return Response.status(BAD_GATEWAY).build();
        }
    }
}
