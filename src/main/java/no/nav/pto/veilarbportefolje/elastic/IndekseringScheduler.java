package no.nav.pto.veilarbportefolje.elastic;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler.gr199.ytelser.KopierGR199FraArena;
import no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler.gr202.tiltak.TiltakHandler;
import no.nav.pto.veilarbportefolje.krr.KrrService;
import no.nav.pto.veilarbportefolje.util.JobUtils;
import no.nav.pto.veilarbportefolje.util.RunningJob;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Optional;

import static no.nav.common.utils.IdUtils.generateId;

@Slf4j
public class IndekseringScheduler {

    private ElasticIndexer elasticIndexer;

    private TiltakHandler tiltakHandler;

    private KopierGR199FraArena kopierGR199FraArena;

    private KrrService krrService;

    private LeaderElectionClient leaderElectionClient;

    public IndekseringScheduler(no.nav.pto.veilarbportefolje.elastic.ElasticIndexer elasticIndexer, TiltakHandler tiltakHandler, KopierGR199FraArena kopierGR199FraArena, KrrService krrService, LeaderElectionClient leaderElectionClient) {
        this.elasticIndexer = elasticIndexer;
        this.tiltakHandler = tiltakHandler;
        this.kopierGR199FraArena = kopierGR199FraArena;
        this.krrService = krrService;
        this.leaderElectionClient = leaderElectionClient;
    }

    // Kjører hver dag kl 04:00
    @Scheduled(cron = "0 0 4 * * ?")
    public void indekserTiltakOgYtelser() {
        if(leaderElectionClient.isLeader()){
            String jobId = generateId();
            MDC.put("jobId", jobId);
            log.info("Startet nattlig elastic av tiltak og ytelser med jobId {}", jobId);
            try {
                kopierGR199FraArena.startOppdateringAvYtelser();
                tiltakHandler.startOppdateringAvTiltakIDatabasen();
            } finally {
                elasticIndexer.startIndeksering();
                MDC.remove("jobId");
            }
        }

    }

    // Kjører hver dag kl 00:00
    @Scheduled(cron = "0 0 0 * * ?")
    public void indekserKrr() {
        Optional<RunningJob> maybeJob = JobUtils.runAsyncJobOnLeader(
                () -> {
                    krrService.hentDigitalKontaktInformasjonBolk();
                    elasticIndexer.startIndeksering();
                },
                leaderElectionClient
        );
        maybeJob.ifPresent(job -> log.info("Startet nattlig elastic av krr med jobId {} på pod {}", job.getJobId(), job.getPodName()));
    }

    // Kjører hvert minutt
    @Scheduled(cron = "0 * * * * *")
    public void deltaindeksering() {
        if(leaderElectionClient.isLeader()) {
            elasticIndexer.deltaindeksering();
        }
    }

}
