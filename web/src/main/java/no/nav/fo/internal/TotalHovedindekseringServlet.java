package no.nav.fo.internal;

import no.nav.fo.filmottak.tiltak.TiltakHandler;
import no.nav.fo.service.SolrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TotalHovedindekseringServlet extends HttpServlet {
    private static Logger logger = LoggerFactory.getLogger(TotalHovedindekseringServlet.class);

    private boolean ismasternode;
    private SolrService solrService;
    private TiltakHandler tiltakHandler;

    @Override
    public void init() throws ServletException {
        this.tiltakHandler = WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean(TiltakHandler.class);
        this.solrService = WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean(SolrService.class);
        this.ismasternode = Boolean.valueOf(System.getProperty("cluster.ismasternode", "false"));
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(this.ismasternode) {
            kjorProsess();
        }
    }

    private void kjorProsess() {
        logger.info("Setter i gang oppdatering av tiltak i databasen");
        tiltakHandler.startOppdateringAvTiltakIDatabasen();
        logger.info("Ferdig med oppdatering av tiltak");
        logger.info("Setter i gang hovedindeksering");
        solrService.hovedindeksering();
        logger.info("Ferdig med hovedindeksering");
    }

    @Scheduled(cron = "${veilarbportefolje.cron.hovedindeksering}")
    private void prosessScheduler() {
        kjorProsess();
    }
}
