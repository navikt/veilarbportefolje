package no.nav.fo.provider.rest;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.service.AktivitetService;
import no.nav.fo.service.SolrService;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static io.vavr.control.Try.run;

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
                    run(() ->
                                solrService.hovedindeksering()
                    ).onFailure(this::rapporterFeil);

        return true;
    }

    @Path("deltaindeksering")
    @GET
    public boolean deltaIndeksering() {
        solrService.deltaindeksering();
        return true;
    }

    private void rapporterFeil(Throwable e) {
        log.warn("Indeksering feilet", e);
        Event event = MetricsFactory.createEvent("indeksering.feilet");
        event.report();
    }
}
