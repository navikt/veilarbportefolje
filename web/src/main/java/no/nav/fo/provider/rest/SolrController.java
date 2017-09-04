package no.nav.fo.provider.rest;

import io.swagger.annotations.Api;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.filmottak.tiltak.TiltakHandler;
import no.nav.fo.filmottak.ytelser.KopierGR199FraArena;
import no.nav.fo.internal.HovedindekseringUtils;
import no.nav.fo.service.AktivitetService;
import no.nav.fo.service.SolrService;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;


@Api(value = "Solr")
@Path("solr")
@Component
@Slf4j
public class SolrController {

    @Inject
    private SolrService solrService;

    @Inject
    private AktivitetService aktivitetService;

    @Inject
    private KopierGR199FraArena kopierGR199FraArena;

    @Inject
    private TiltakHandler tiltakHandler;


    @Path("hovedindeksering")
    @GET
    public boolean hovedIndeksering() {
            aktivitetService.tryUtledOgLagreAlleAktivitetstatuser();
            Try.of(() -> {
                solrService.hovedindeksering();
                return null;
            }).onFailure(this::rapporterFeil);
        return true;
    }

    @Path("deltaindeksering")
    @GET
    public boolean deltaIndeksering() {
        solrService.deltaindeksering();
        return true;
    }

    @Scheduled(cron = "${veilarbportefolje.cron.hovedindeksering}")
    private void prosessScheduler() {
        HovedindekseringUtils.totalHovedindeksering(kopierGR199FraArena, tiltakHandler, solrService);
    }

    private void rapporterFeil(Throwable e) {
        log.warn("Indeksering feilet", e);
        Event event = MetricsFactory.createEvent("indeksering.feilet");
        event.report();
    }

}
