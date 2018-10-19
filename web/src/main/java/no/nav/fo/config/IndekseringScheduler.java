package no.nav.fo.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.filmottak.tiltak.TiltakHandler;
import no.nav.fo.filmottak.ytelser.KopierGR199FraArena;
import no.nav.fo.service.KrrService;
import no.nav.fo.service.SolrService;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;

import static no.nav.fo.util.MetricsUtils.timed;

@Slf4j
public class IndekseringScheduler {

    @Inject
    private SolrService solrService;

    @Inject
    private TiltakHandler tiltakHandler;

    @Inject
    private KopierGR199FraArena kopierGR199FraArena;

    @Inject
    private KrrService krrService;

    @Scheduled(cron = "0 0 4 * * ?")
    public void totalIndexering() {
        timed("indeksering.total", () -> {
            kopierGR199FraArena.startOppdateringAvYtelser();
            tiltakHandler.startOppdateringAvTiltakIDatabasen();
            krrService.hentDigitalKontaktInformasjonBolk();
            solrService.hovedindeksering();
        });
    }

    @Scheduled(cron = "0 * * * * *")
    public void deltaindeksering() {
        solrService.deltaindeksering();
    }

}
