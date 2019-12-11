package no.nav.fo.veilarbportefolje.indeksering;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.filmottak.tiltak.TiltakHandler;
import no.nav.fo.veilarbportefolje.filmottak.ytelser.KopierGR199FraArena;
import no.nav.fo.veilarbportefolje.krr.KrrService;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;

import static no.nav.jobutils.JobUtils.runAsyncJobOnLeader;


@Slf4j
public class IndekseringScheduler {

    private ElasticIndexer elasticIndexer;

    private TiltakHandler tiltakHandler;

    private KopierGR199FraArena kopierGR199FraArena;

    private KrrService krrService;

    @Inject
    public IndekseringScheduler(ElasticIndexer elasticIndexer, TiltakHandler tiltakHandler, KopierGR199FraArena kopierGR199FraArena, KrrService krrService) {
        this.elasticIndexer = elasticIndexer;
        this.tiltakHandler = tiltakHandler;
        this.kopierGR199FraArena = kopierGR199FraArena;
        this.krrService = krrService;
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void totalIndexering() {
        runAsyncJobOnLeader(
                () -> {
                    try {
                        kopierGR199FraArena.startOppdateringAvYtelser();
                        tiltakHandler.startOppdateringAvTiltakIDatabasen();
                        krrService.oppdaterDigitialKontaktinformasjon();
                    } finally {
                        elasticIndexer.startIndeksering();
                    }
                }
        );
    }

    @Scheduled(cron = "0 * * * * *")
    public void deltaindeksering() {
        runAsyncJobOnLeader(elasticIndexer::deltaindeksering);
    }

}
