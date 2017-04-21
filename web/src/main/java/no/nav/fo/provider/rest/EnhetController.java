package no.nav.fo.provider.rest;

import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.domene.*;
import no.nav.fo.service.BrukertilgangService;
import no.nav.fo.service.PepClientInterface;
import no.nav.fo.service.SolrService;
import no.nav.fo.util.PortefoljeUtils;
import no.nav.fo.util.TokenUtils;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_GATEWAY;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
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
    PepClientInterface pepClient;

    @POST
    @Path("/{enhet}/portefolje")
    public Response hentPortefoljeForEnhet(
            @PathParam("enhet") String enhet,
            @QueryParam("fra") int fra,
            @QueryParam("antall") int antall,
            @QueryParam("sortDirection") String sortDirection,
            @QueryParam("sortField") String sortField,
            Filtervalg filtervalg) {

        List<String> enheterIPilot = Arrays.asList(System.getProperty("portefolje.pilot.enhetliste").split(","));

        try {
            if(!enheterIPilot.contains(enhet)) {
                return Response.ok().entity(new Portefolje().setBrukere(new ArrayList<>())).build();
            }


            String ident = SubjectHandler.getSubjectHandler().getUid();
            String identHash = DigestUtils.md5Hex(ident).toUpperCase();

            String token = TokenUtils.getTokenBody(SubjectHandler.getSubjectHandler().getSubject());
            boolean brukerHarTilgangTilEnhet = brukertilgangService.harBrukerTilgang(ident, enhet);
            boolean userIsInModigOppfolging = pepClient.isSubjectMemberOfModiaOppfolging(ident);


            if (brukerHarTilgangTilEnhet && userIsInModigOppfolging) {
                List<Bruker> brukere = solrService.hentBrukereForEnhet(enhet, sortDirection, sortField, filtervalg);
                List<Bruker> brukereSublist = PortefoljeUtils.getSublist(brukere, fra, antall);
                List<Bruker> sensurerteBrukereSublist = PortefoljeUtils.sensurerBrukere(brukereSublist,token, pepClient);

                Portefolje portefolje = PortefoljeUtils.buildPortefolje(brukere, sensurerteBrukereSublist, enhet, fra);

                Event event = MetricsFactory.createEvent("enhetsportefolje.lastet");
                event.addFieldToReport("identhash", identHash);
                event.report();

                return Response.ok().entity(portefolje).build();
            } else {
                return Response.status(UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            logger.warn("Kall mot upstream service feilet", e);
            return Response.status(BAD_GATEWAY).build();
        }
    }

    @GET
    @Path("/{enhet}/portefoljestorrelser")
    public Response hentPortefoljestorrelser(@PathParam("enhet") String enhet) {
        FacetResults facetResult = solrService.hentPortefoljestorrelser(enhet);
        return Response.ok().entity(facetResult).build();
    }

    @GET
    @Path("/{enhet}/statustall")
    public Response hentStatusTall(@PathParam("enhet") String enhet) {
        List<String> enheterIPilot = Arrays.asList(System.getProperty("portefolje.pilot.enhetliste").split(","));

        if(!enheterIPilot.contains(enhet)) {
            return Response.ok().entity(new StatusTall().setTotalt(0).setInaktiveBrukere(0).setNyeBrukere(0)).build();
        }

        StatusTall statusTall = solrService.hentStatusTallForPortefolje(enhet);
        return Response.ok().entity(statusTall).build();
    }
}
