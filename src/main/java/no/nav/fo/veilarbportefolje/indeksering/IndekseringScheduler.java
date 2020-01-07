package no.nav.fo.veilarbportefolje.indeksering;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.filmottak.tiltak.TiltakHandler;
import no.nav.fo.veilarbportefolje.filmottak.ytelser.KopierGR199FraArena;
import no.nav.fo.veilarbportefolje.service.KrrService;
import no.nav.jobutils.JobUtils;
import no.nav.jobutils.RunningJob;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;
import java.util.Optional;

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
    public void indekserTiltakOgYtelser() {
        Optional<RunningJob> maybeJob = JobUtils.runAsyncJobOnLeader(
                () -> {
                    try {
                        kopierGR199FraArena.startOppdateringAvYtelser();
                        tiltakHandler.startOppdateringAvTiltakIDatabasen();
                    } finally {
                        elasticIndexer.startIndeksering();
                    }
                }
        );
        maybeJob.ifPresent(job -> log.info("Startet nattlig indeksering av tiltak og ytelser med jobId {} på pod {}", job.getJobId(), job.getPodName()));
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void indekserKrr() {
        Optional<RunningJob> maybeJob = JobUtils.runAsyncJobOnLeader(
                () -> {
                    krrService.hentDigitalKontaktInformasjonBolk();
                    elasticIndexer.startIndeksering();
                }
        );
        maybeJob.ifPresent(job -> log.info("Startet nattlig indeksering av krr med jobId {} på pod {}", job.getJobId(), job.getPodName()));
    }

    @Scheduled(cron = "0 * * * * *")
    public void deltaindeksering() {
        JobUtils.runAsyncJobOnLeader(elasticIndexer::deltaindeksering);
    }

}
