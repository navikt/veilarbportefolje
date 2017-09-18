package no.nav.fo.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.filmottak.tiltak.TiltakHandler;
import no.nav.fo.filmottak.ytelser.KopierGR199FraArena;
import no.nav.fo.service.SolrService;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;

import static no.nav.fo.util.MetricsUtils.timed;

@Slf4j
public class HovedindekseringScheduler {

    private boolean ismasternode = Boolean.valueOf(System.getProperty("cluster.ismasternode", "false"));

    @Inject
    private SolrService solrService;

    @Inject
    private TiltakHandler tiltakHandler;

    @Inject
    private KopierGR199FraArena kopierGR199FraArena;


    @Scheduled(cron = "${veilarbportefolje.cron.hovedindeksering}")
    public void prosessScheduler() {
        if(ismasternode) {
            timed("indeksering.total", () -> {
                log.info("Setter i gang oppdatering av ytelser i databasen");
                timed("indeksering.oppdatering.ytelser", () -> kopierGR199FraArena.startOppdateringAvYtelser());
                log.info("Ferdig med oppdatering av ytelser");
                log.info("Setter i gang oppdatering av tiltak i databasen");
                timed("indeksering.oppdatering.tiltak", () -> tiltakHandler.startOppdateringAvTiltakIDatabasen());
                log.info("Ferdig med oppdatering av tiltak");
                log.info("Setter i gang hovedindeksering");
                timed("indeksering.populerindeks", () -> solrService.hovedindeksering());
                log.info("Ferdig med hovedindeksering");
            });
        }
    }
}
