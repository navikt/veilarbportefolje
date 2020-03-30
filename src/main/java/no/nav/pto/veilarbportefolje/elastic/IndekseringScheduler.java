package no.nav.pto.veilarbportefolje.elastic;

import lombok.extern.slf4j.Slf4j;
import no.nav.jobutils.JobUtils;
import no.nav.jobutils.RunningJob;
import no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser.KopierGR199FraArena;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakHandler;
import no.nav.pto.veilarbportefolje.krr.KrrService;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.MINUTES;

@Slf4j
public class IndekseringScheduler {

    private static ScheduledFuture<?> deltaIndeksering;

    private ElasticIndexer           elasticIndexer;

    private TiltakHandler tiltakHandler;

    private KopierGR199FraArena kopierGR199FraArena;

    private KrrService               krrService;

    @Inject
    public IndekseringScheduler(ElasticIndexer elasticIndexer, TiltakHandler tiltakHandler, KopierGR199FraArena kopierGR199FraArena, KrrService krrService) {
        this.elasticIndexer = elasticIndexer;
        this.tiltakHandler = tiltakHandler;
        this.kopierGR199FraArena = kopierGR199FraArena;
        this.krrService = krrService;

        deltaIndeksering = Executors.newSingleThreadScheduledExecutor()
                                    .scheduleWithFixedDelay(elasticIndexer::deltaindeksering, 1, 1, MINUTES);
    }

    // Kjører hver dag kl 04:00
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
        maybeJob.ifPresent(job -> log.info("Startet nattlig elastic av tiltak og ytelser med jobId {} på pod {}", job.getJobId(), job.getPodName()));
    }

    // Kjører hver dag kl 00:00
    @Scheduled(cron = "0 0 0 * * ?")
    public void indekserKrr() {
        Optional<RunningJob> maybeJob = JobUtils.runAsyncJobOnLeader(
                () -> {
                    krrService.hentDigitalKontaktInformasjonBolk();
                    elasticIndexer.startIndeksering();
                }
        );
        maybeJob.ifPresent(job -> log.info("Startet nattlig elastic av krr med jobId {} på pod {}", job.getJobId(), job.getPodName()));
    }
}
