package no.nav.fo.veilarbportefolje.provider.rest;

import io.swagger.annotations.Api;
import no.nav.common.auth.SubjectHandler;
import no.nav.fo.veilarbportefolje.domene.*;
import no.nav.fo.veilarbportefolje.indeksering.ElasticIndexer;
import no.nav.fo.veilarbportefolje.service.PepClient;
import no.nav.fo.veilarbportefolje.util.PortefoljeUtils;
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
import static no.nav.fo.veilarbportefolje.provider.rest.RestUtils.createResponse;
import static no.nav.fo.veilarbportefolje.util.SubjectUtils.getOidcToken;

@Api(value = "Veileder")
@Path("/veileder")
@Component
@Produces(APPLICATION_JSON)
public class VeilederController {

    private ElasticIndexer elasticIndexer;
    private PepClient pepClient;

    @Inject
    public VeilederController(
            ElasticIndexer elasticIndexer,
            PepClient pepClient
    ) {

        this.elasticIndexer = elasticIndexer;
        this.pepClient = pepClient;
    }

    @POST
    @Path("/{veilederident}/portefolje")
    public Response hentPortefoljeForVeileder(
            @PathParam("veilederident") String veilederIdent,
            @QueryParam("enhet") String enhet,
            @QueryParam("fra") Integer fra,
            @QueryParam("antall") Integer antall,
            @QueryParam("sortDirection") String sortDirection,
            @QueryParam("sortField") String sortField,
            Filtervalg filtervalg) {

        return createResponse(() -> {
            ValideringsRegler.sjekkVeilederIdent(veilederIdent, false);
            ValideringsRegler.sjekkEnhet(enhet);
            ValideringsRegler.sjekkSortering(sortDirection, sortField);
            ValideringsRegler.sjekkFiltervalg(filtervalg);
            TilgangsRegler.tilgangTilOppfolging(pepClient);
            TilgangsRegler.tilgangTilEnhet(pepClient, enhet);

            String ident = SubjectHandler.getIdent().orElseThrow(IllegalStateException::new);
            String identHash = DigestUtils.md5Hex(ident).toUpperCase();

            BrukereMedAntall brukereMedAntall = elasticIndexer.hentBrukere(enhet, Optional.of(veilederIdent), sortDirection, sortField, filtervalg, fra, antall);
            List<Bruker> sensurerteBrukereSublist = PortefoljeUtils.sensurerBrukere(brukereMedAntall.getBrukere(), getOidcToken(), pepClient);

            Portefolje portefolje = PortefoljeUtils.buildPortefolje(brukereMedAntall.getAntall(),
                    sensurerteBrukereSublist,
                    enhet,
                    Optional.ofNullable(fra).orElse(0));

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
            Event event = MetricsFactory.createEvent("minoversiktportefolje.statustall.lastet");
            event.report();
            ValideringsRegler.sjekkEnhet(enhet);
            ValideringsRegler.sjekkVeilederIdent(veilederIdent, false);
            TilgangsRegler.tilgangTilEnhet(pepClient, enhet);

            return elasticIndexer.hentStatusTallForVeileder(enhet, veilederIdent);
        });
    }

    @GET
    @Path("/{veilederident}/arbeidsliste")
    public Response hentArbeidsliste(@PathParam("veilederident") String veilederIdent, @QueryParam("enhet") String enhet) {
        return createResponse(() -> {
            Event event = MetricsFactory.createEvent("minoversiktportefolje.arbeidsliste.lastet");
            event.report();
            ValideringsRegler.sjekkEnhet(enhet);
            ValideringsRegler.sjekkVeilederIdent(veilederIdent, false);
            TilgangsRegler.tilgangTilEnhet(pepClient, enhet);

            return elasticIndexer.hentBrukereMedArbeidsliste(VeilederId.of(veilederIdent), enhet);
        });
    }

}
