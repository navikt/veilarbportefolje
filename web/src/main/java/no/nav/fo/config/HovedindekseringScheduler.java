package no.nav.fo.config;

import no.nav.fo.filmottak.tiltak.TiltakHandler;
import no.nav.fo.filmottak.ytelser.KopierGR199FraArena;
import no.nav.fo.service.SolrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;

import static no.nav.fo.util.MetricsUtils.timed;

public class HovedindekseringScheduler {

    private static Logger logger = LoggerFactory.getLogger(HovedindekseringScheduler.class);

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
                logger.info("Setter i gang oppdatering av ytelser i databasen");
                timed("indeksering.oppdatering.ytelser", () -> kopierGR199FraArena.startOppdateringAvYtelser());
                logger.info("Ferdig med oppdatering av ytelser");
                logger.info("Setter i gang oppdatering av tiltak i databasen");
                timed("indeksering.oppdatering.tiltak", () -> tiltakHandler.startOppdateringAvTiltakIDatabasen());
                logger.info("Ferdig med oppdatering av tiltak");
                logger.info("Setter i gang hovedindeksering");
                timed("indeksering.populerindeks", () -> solrService.hovedindeksering());
                logger.info("Ferdig med hovedindeksering");
            });
        }
    }
}
