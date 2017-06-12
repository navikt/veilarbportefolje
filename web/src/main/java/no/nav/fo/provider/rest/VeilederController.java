package no.nav.fo.provider.rest;

import io.swagger.annotations.Api;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.domene.Bruker;
import no.nav.fo.domene.Filtervalg;
import no.nav.fo.domene.Portefolje;
import no.nav.fo.domene.StatusTall;
import no.nav.fo.service.BrukertilgangService;
import no.nav.fo.service.PepClient;
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
import java.util.List;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.fo.provider.rest.RestUtils.createResponse;
import static org.slf4j.LoggerFactory.getLogger;

@Api(value = "Veileder")
@Path("/veileder")
@Produces(APPLICATION_JSON)
public class VeilederController {

    private static final Logger logger = getLogger(VeilederController.class);

    private BrukertilgangService brukertilgangService;
    private SolrService solrService;
    private PepClient pepClient;

    @Inject
    public VeilederController(
            BrukertilgangService brukertilgangService,
            SolrService solrService,
            PepClient pepClient
    ) {
        this.brukertilgangService = brukertilgangService;
        this.solrService = solrService;
        this.pepClient = pepClient;
    }

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

        return createResponse(() -> {
            ValideringsRegler.sjekkVeilederIdent(veilederIdent, false);
            ValideringsRegler.sjekkEnhet(enhet);
            ValideringsRegler.sjekkSortering(sortDirection, sortField);
            ValideringsRegler.sjekkFiltervalg(filtervalg);
            TilgangsRegler.tilgangTilOppfolging(pepClient);
            TilgangsRegler.tilgangTilEnhet(brukertilgangService, enhet);

            if (!TilgangsRegler.enhetErIPilot(enhet)) {
                return new Portefolje().setBrukere(new ArrayList<>());
            }

            String ident = SubjectHandler.getSubjectHandler().getUid();
            String identHash = DigestUtils.md5Hex(ident).toUpperCase();

            String token = TokenUtils.getTokenBody(SubjectHandler.getSubjectHandler().getSubject());

            List<Bruker> brukere = solrService.hentBrukere(enhet, Optional.of(veilederIdent), sortDirection, sortField, filtervalg);
            List<Bruker> brukereSublist = PortefoljeUtils.getSublist(brukere, fra, antall);
            List<Bruker> sensurerteBrukereSublist = PortefoljeUtils.sensurerBrukere(brukereSublist, token, pepClient);

            Portefolje portefolje = PortefoljeUtils.buildPortefolje(brukere, sensurerteBrukereSublist, enhet, fra);

            Event event = MetricsFactory.createEvent("minoversiktportefolje.lastet");
            event.addFieldToReport("identhash", identHash);
            event.report();

            return portefolje;
        });
    }

    @GET
    @Path("/{veilederident}/statustall")
    public Response hentStatusTall(@PathParam("veilederident") String veilederIdent, @QueryParam("enhet") String enhet) {
        return createResponse(() -> {
            ValideringsRegler.sjekkEnhet(enhet);
            ValideringsRegler.sjekkVeilederIdent(veilederIdent, false);

            if (!TilgangsRegler.enhetErIPilot(enhet)) {
                return new StatusTall();
            }

            return solrService.hentStatusTallForVeileder(enhet, veilederIdent);
        });
    }
}
