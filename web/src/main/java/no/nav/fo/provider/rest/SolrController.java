package no.nav.fo.provider.rest;

import io.swagger.annotations.Api;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.service.AktivitetService;
import no.nav.fo.service.SolrService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static no.nav.fo.util.MetricsUtils.timed;

@Api(value = "Solr")
@Path("solr")
@Slf4j
public class SolrController {

    @Inject
    private SolrService solrService;

    @Inject
    private AktivitetService aktivitetService;

    @Path("hovedindeksering")
    @GET
    public boolean hovedIndeksering() {
        aktivitetService.tryUtledOgLagreAlleAktivitetstatuser();
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
