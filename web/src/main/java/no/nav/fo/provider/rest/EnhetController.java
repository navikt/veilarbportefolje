package no.nav.fo.provider.rest;

import io.swagger.annotations.Api;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.domene.*;
import no.nav.fo.service.PepClient;
import no.nav.fo.service.SolrService;
import no.nav.fo.service.TiltakService;
import no.nav.fo.util.PortefoljeUtils;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.fo.provider.rest.RestUtils.createResponse;
import static no.nav.fo.provider.rest.RestUtils.getSsoToken;

@Api(value = "Enhet")
@Path("/enhet")
@Component
@Produces(APPLICATION_JSON)
public class EnhetController {

    private SolrService solrService;
    private PepClient pepClient;
    private TiltakService tiltakService;

    @Inject
    public EnhetController(
            SolrService solrService,
            PepClient pepClient,
            TiltakService tiltakService) {
        this.solrService = solrService;
        this.tiltakService = tiltakService;
        this.pepClient = pepClient;
    }


    @POST
    @Path("/{enhet}/portefolje")
    public Response hentPortefoljeForEnhet(
            @PathParam("enhet") String enhet,
            @QueryParam("fra") Integer fra,
            @QueryParam("antall") Integer antall,
            @QueryParam("sortDirection") String sortDirection,
            @QueryParam("sortField") String sortField,
            Filtervalg filtervalg) {

        return createResponse(() -> {
            ValideringsRegler.sjekkEnhet(enhet);
            ValideringsRegler.sjekkSortering(sortDirection, sortField);
            ValideringsRegler.sjekkFiltervalg(filtervalg);
            TilgangsRegler.tilgangTilOppfolging(pepClient);
            TilgangsRegler.tilgangTilEnhet(pepClient, enhet);

            String ident = SubjectHandler.getSubjectHandler().getUid();
            String identHash = DigestUtils.md5Hex(ident).toUpperCase();

            BrukereMedAntall brukereMedAntall = solrService.hentBrukere(enhet, Optional.empty(), sortDirection, sortField, filtervalg, fra, antall);
            List<Bruker> sensurerteBrukereSublist = PortefoljeUtils.sensurerBrukere(brukereMedAntall.getBrukere(), getSsoToken(), pepClient);

            Portefolje portefolje = PortefoljeUtils.buildPortefolje(brukereMedAntall.getAntall(),
                    sensurerteBrukereSublist,
                    enhet,
                    Optional.ofNullable(fra).orElse(0));

            Event event = MetricsFactory.createEvent("enhetsportefolje.lastet");
            event.addFieldToReport("identhash", identHash);
            event.report();

            return portefolje;
        });
    }


    @GET
    @Path("/{enhet}/portefoljestorrelser")
    public Response hentPortefoljestorrelser(@PathParam("enhet") String enhet) {
        return createResponse(() -> {
            ValideringsRegler.sjekkEnhet(enhet);
            TilgangsRegler.tilgangTilEnhet(pepClient, enhet);

            return solrService.hentPortefoljestorrelser(enhet);
        });
    }

    @GET
    @Path("/{enhet}/statustall")
    public Response hentStatusTall(@PathParam("enhet") String enhet) {
        return createResponse(() -> {
            ValideringsRegler.sjekkEnhet(enhet);
            TilgangsRegler.tilgangTilEnhet(pepClient, enhet);

            return solrService.hentStatusTallForPortefolje(enhet);
        });
    }

    @GET
    @Path("/{enhet}/tiltak")
    public Response hentTiltak(@PathParam("enhet") String enhet) {
        return createResponse(() -> {
            ValideringsRegler.sjekkEnhet(enhet);
            TilgangsRegler.tilgangTilEnhet(pepClient, enhet);

            return tiltakService.hentEnhettiltak(enhet)
                    .getOrElse(new EnhetTiltak());
        });
    }
}
