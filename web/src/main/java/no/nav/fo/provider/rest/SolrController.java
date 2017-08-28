package no.nav.fo.provider.rest;

import io.swagger.annotations.Api;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.service.AktivitetService;
import no.nav.fo.service.SolrService;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static java.util.concurrent.CompletableFuture.runAsync;

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
    public String hovedIndeksering() {
        runAsync( () -> {
            aktivitetService.tryUtledOgLagreAlleAktivitetstatuser();
            Try.of(() -> {
                solrService.hovedindeksering();
                return null;
            }).onFailure(this::rapporterFeil);
        });
        return "Indeksering startet";
    }

    @Path("deltaindeksering")
    @GET
    public String deltaIndeksering() {
        runAsync(() -> solrService.deltaindeksering());
        return "Indeksering startet";
    }

    private void rapporterFeil(Throwable e) {
        log.warn("Indeksering feilet", e);
        Event event = MetricsFactory.createEvent("indeksering.feilet");
        event.report();
    }
}
