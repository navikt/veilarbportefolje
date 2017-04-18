package no.nav.fo.provider.rest;

import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.domene.Bruker;
import no.nav.fo.domene.Filtervalg;
import no.nav.fo.domene.Portefolje;
import no.nav.fo.domene.StatusTall;
import no.nav.fo.service.BrukertilgangService;
import no.nav.fo.service.PepClientInterface;
import no.nav.fo.service.SolrService;
import no.nav.fo.util.PortefoljeUtils;
import no.nav.fo.util.TokenUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_GATEWAY;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.slf4j.LoggerFactory.getLogger;

@Path("/veileder")
@Produces(APPLICATION_JSON)
public class VeilederController {

    private static final Logger logger = getLogger(VeilederController.class);

    @Inject
    BrukertilgangService brukertilgangService;

    @Inject
    SolrService solrService;

    @Inject
    PepClientInterface pepClient;

    @POST
    @Path("/{veilederident}/portefolje")
    public Response hentPortefoljeForVeileder(
            @PathParam("veilederident") String veilederIdent,
            @QueryParam("enhet") String enhet,
            @QueryParam("fra") int fra,
            @QueryParam("antall") int antall,
            @QueryParam("sortDirection") String sortDirection,
            @QueryParam("sortField") String sortField,
            Filtervalg filtervalg) {

        try {
            String ident = SubjectHandler.getSubjectHandler().getUid();
            String token = TokenUtils.getTokenBody(SubjectHandler.getSubjectHandler().getSubject());

            boolean brukerHarTilgangTilEnhet = brukertilgangService.harBrukerTilgang(ident, enhet);
            boolean userIsInModigOppfolging = pepClient.isSubjectMemberOfModiaOppfolging(ident);

            if (brukerHarTilgangTilEnhet && userIsInModigOppfolging) {

                List<Bruker> brukere = solrService.hentBrukereForVeileder(veilederIdent, enhet, sortDirection, sortField, filtervalg);
                List<Bruker> brukereSublist = PortefoljeUtils.getSublist(brukere, fra, antall);
                List<Bruker> sensurerteBrukereSublist = PortefoljeUtils.sensurerBrukere(brukereSublist,token, pepClient);

                Portefolje portefolje = PortefoljeUtils.buildPortefolje(brukere, sensurerteBrukereSublist, enhet, fra);

                return Response.ok().entity(portefolje).build();
            } else {
                return Response.status(UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            logger.error("Kall mot upstream service feilet", e);
            return Response.status(BAD_GATEWAY).build();
        }
    }

    @GET
    @Path("/{veilederident}/statustall")
    public Response hentStatusTall(@PathParam("veilederident") String veilederIdent, @QueryParam("enhet") String enhet) {
        List<String> enheterIPilot = Arrays.asList(System.getProperty("portefolje.pilot.enhetliste").split(","));

        if (!enheterIPilot.contains(enhet)) {
            return Response.ok().entity(new StatusTall().setTotalt(0).setInaktiveBrukere(0)).build();
        }

        StatusTall statusTall = solrService.hentStatusTallForVeileder(enhet, veilederIdent);
        return Response.ok().entity(statusTall).build();
    }
}
