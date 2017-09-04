package no.nav.fo.internal;

import no.nav.fo.filmottak.tiltak.TiltakHandler;
import no.nav.fo.filmottak.ytelser.KopierGR199FraArena;
import no.nav.fo.service.SolrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HovedindekseringUtils {

    private static Logger logger = LoggerFactory.getLogger(HovedindekseringUtils.class);

    public static void totalHovedindeksering(KopierGR199FraArena kopierGR199FraArena, TiltakHandler tiltakHandler, SolrService solrService) {
        logger.info("Setter i gang oppdatering av ytelser i databasen");
        kopierGR199FraArena.startOppdateringAvYtelser();
        logger.info("Ferdig med oppdatering av ytelser");
        logger.info("Setter i gang oppdatering av tiltak i databasen");
        tiltakHandler.startOppdateringAvTiltakIDatabasen();
        logger.info("Ferdig med oppdatering av tiltak");
        logger.info("Setter i gang hovedindeksering");
        solrService.hovedindeksering();
        logger.info("Ferdig med hovedindeksering");
    }
}
