package no.nav.fo.provider.rest;

import no.nav.fo.domene.Bruker;
import no.nav.fo.domene.Portefolje;
import no.nav.fo.security.jwt.context.SubjectHandler;
import no.nav.fo.security.jwt.filter.JWTInAuthorizationHeaderJAAS;
import no.nav.fo.security.jwt.filter.SessionTerminator;
import no.nav.fo.service.BrukertilgangService;
import no.nav.fo.service.SolrService;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_GATEWAY;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.slf4j.LoggerFactory.getLogger;

@JWTInAuthorizationHeaderJAAS
@SessionTerminator
@Path("/portefolje")
@Produces(APPLICATION_JSON)
public class PortefoljeController {

    private static final Logger logger = getLogger(PortefoljeController.class);

    @Inject
    BrukertilgangService brukertilgangService;

    @Inject
    SolrService solrService;

    @GET
    @Path("/enhet/{enhet}")
    public Response hentPortefoljeForEnhet(
            @PathParam("enhet") String enhet,
            @QueryParam("fra") int fra,
            @QueryParam("antall") int antall,
            @QueryParam("sortByLastName") String sortDirection) {

        try {
            String ident = SubjectHandler.getSubjectHandler().getUid();
            boolean brukerHarTilgangTilEnhet = brukertilgangService.harBrukerTilgang(ident, enhet);

            if (brukerHarTilgangTilEnhet) {

                List<Bruker> brukere = solrService.hentBrukereForEnhet(enhet, sortDirection);
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
            logger.error("Kall mot upstream service feilet", e);
            return Response.status(BAD_GATEWAY).build();
        }
    }

    @GET
    @Path("/veileder/{veilederident}")
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
            logger.error("Kall mot upstream service feilet", e);
            return Response.status(BAD_GATEWAY).build();
        }
    }
}
