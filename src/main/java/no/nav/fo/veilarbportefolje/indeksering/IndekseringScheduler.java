package no.nav.fo.veilarbportefolje.indeksering;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.filmottak.tiltak.TiltakHandler;
import no.nav.fo.veilarbportefolje.filmottak.ytelser.KopierGR199FraArena;
import no.nav.fo.veilarbportefolje.service.KrrService;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;

import static no.nav.batch.BatchJob.runAsyncOnLeader;

@Slf4j
public class IndekseringScheduler {

    @Inject
    private ElasticIndexer elasticIndexer;

    @Inject
    private TiltakHandler tiltakHandler;

    @Inject
    private KopierGR199FraArena kopierGR199FraArena;

    @Inject
    private KrrService krrService;

    @Scheduled(cron = "0 0 4 * * ?")
    public void totalIndexering() {
        runAsyncOnLeader(
                () -> {
                    kopierGR199FraArena.startOppdateringAvYtelser();
                    tiltakHandler.startOppdateringAvTiltakIDatabasen();
                    krrService.hentDigitalKontaktInformasjonBolk();
                    elasticIndexer.hovedindeksering();
                }
        );
    }

    @Scheduled(cron = "0 * * * * *")
    public void deltaindeksering() {
        runAsyncOnLeader(() -> elasticIndexer.deltaindeksering());
    }

}
