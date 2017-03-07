package no.nav.fo.provider.rest;

import no.nav.fo.domene.Bruker;
import no.nav.fo.domene.Portefolje;
import no.nav.fo.security.jwt.context.SubjectHandler;
import no.nav.fo.service.BrukertilgangService;
import no.nav.fo.service.SolrService;
import no.nav.fo.util.PortefoljeUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_GATEWAY;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.slf4j.LoggerFactory.getLogger;

@Path("/veileder")
@Produces(APPLICATION_JSON)
public class VeilederController {

    private static final Logger logger = getLogger(VeilederController.class);

    @Inject
    BrukertilgangService brukertilgangService;

    @Inject
    SolrService solrService;

    @GET
    @Path("/{veilederident}/portefolje")
    public Response hentPortefoljeForVeileder(
            @PathParam("veilederident") String veilederIdent,
            @QueryParam("enhet") String enhet,
            @QueryParam("fra") int fra,
            @QueryParam("antall") int antall,
            @QueryParam("sortByLastName") String sortDirection) {

        try {
            String ident = SubjectHandler.getSubjectHandler().getUid();
            boolean brukerHarTilgangTilEnhet = brukertilgangService.harBrukerTilgang(ident, enhet);

            if (brukerHarTilgangTilEnhet) {

                List<Bruker> brukere = solrService.hentBrukereForVeileder(veilederIdent, enhet, sortDirection);
                List<Bruker> brukereSublist = PortefoljeUtils.getSublist(brukere, fra, antall);

                Portefolje portefolje = PortefoljeUtils.buildPortefolje(brukere, brukereSublist, enhet, fra);

                return Response.ok().entity(portefolje).build();
            } else {
                return Response.status(FORBIDDEN).build();
            }
        } catch (Exception e) {
            logger.error("Kall mot upstream service feilet", e);
            return Response.status(BAD_GATEWAY).build();
        }
    }
}
