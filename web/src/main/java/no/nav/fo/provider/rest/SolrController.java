package no.nav.fo.provider.rest;

import io.swagger.annotations.Api;
import no.nav.fo.service.AktivitetService;
import no.nav.fo.service.SolrService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static no.nav.fo.util.MetricsUtils.timed;

@Api(value="Solr")
@Path("solr")
public class SolrController {

    @Inject
    SolrService solrService;

    @Inject
    AktivitetService aktivitetService;

    @Path("hovedindeksering")
    @GET
    public boolean hovedIndeksering() {
        timed("aktiviteter.utled.statuser", () -> {aktivitetService.utledOgLagreAlleAktivitetstatuser(); return null; });

        solrService.hovedindeksering();
        return true;
    }

    @Path("deltaindeksering")
    @GET
    public boolean deltaIndeksering() {
        solrService.deltaindeksering();
        return true;
    }
}
