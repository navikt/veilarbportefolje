package no.nav.fo.provider.rest;

import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.domene.Bruker;
import no.nav.fo.domene.FacetResults;
import no.nav.fo.domene.Filtervalg;
import no.nav.fo.domene.Portefolje;
import no.nav.fo.service.BrukertilgangService;
import no.nav.fo.service.PepClient;
import no.nav.fo.service.SolrService;
import no.nav.fo.util.PortefoljeUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_GATEWAY;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.slf4j.LoggerFactory.getLogger;

@Path("/enhet")
@Produces(APPLICATION_JSON)
public class EnhetController {

    private static final Logger logger = getLogger(EnhetController.class);

    @Inject
    BrukertilgangService brukertilgangService;

    @Inject
    SolrService solrService;

    @Inject
    PepClient pepClient;

    @GET
    @Path("/{enhet}/portefolje")
    public Response hentPortefoljeForEnhet(
            @PathParam("enhet") String enhet,
            @QueryParam("fra") int fra,
            @QueryParam("antall") int antall,
            @QueryParam("sortDirection") String sortDirection,
            @QueryParam("sortField") String sortField,
            @BeanParam Filtervalg filtervalg) {

        List<String> enheterIPilot = Arrays.asList(System.getProperty("portefolje.pilot.enhetliste").split(","));

        try {
            if(!enheterIPilot.contains(enhet)) {
                return Response.ok().entity(new Portefolje().setBrukere(new ArrayList<>())).build();
            }
            String ident = SubjectHandler.getSubjectHandler().getUid();
            boolean brukerHarTilgangTilEnhet = brukertilgangService.harBrukerTilgang(ident, enhet);

            if (brukerHarTilgangTilEnhet) {

                List<Bruker> brukere = solrService.hentBrukereForEnhet(enhet, sortDirection, sortField, filtervalg);
                List<Bruker> brukereSublist = PortefoljeUtils.getSublist(brukere, fra, antall);
                List<Bruker> filtrertBrukereSublist = PortefoljeUtils.sensurerBrukere(brukereSublist,ident, pepClient);

                Portefolje portefolje = PortefoljeUtils.buildPortefolje(brukere, filtrertBrukereSublist, enhet, fra);

                return Response.ok().entity(portefolje).build();
            } else {
                return Response.status(FORBIDDEN).build();
            }
        } catch (Exception e) {
            logger.error("Kall mot upstream service feilet", e);
            return Response.status(BAD_GATEWAY).build();
        }
    }

    @GET
    @Path("/{enhet}/portefoljestorrelser")
    public Response hentPortefoljestorrelser(@PathParam("enhet") String enhet) {
        FacetResults facetResult = solrService.hentPortefoljestorrelser(enhet);
        return Response.ok().entity(facetResult).build();
    }
}
