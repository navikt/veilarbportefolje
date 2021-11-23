package no.nav.pto.veilarbportefolje.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesService;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesServicePostgres;
import no.nav.pto.veilarbportefolje.database.BrukerAktiviteterService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulConfig implements SchedulingConfigurer{
    private final BrukerAktiviteterService brukerAktiviteterService;
    private final YtelsesServicePostgres ytelsesServicePostgres;
    private final YtelsesService ytelsesService;
    private final LeaderElectionClient leaderElectionClient;

    // Denne jobben må kjøre etter midnatt
    @Scheduled(cron = "0 1 0 * * ?")
    public void oppdaterBrukerAktiviteter() {
        if (leaderElectionClient.isLeader()) {
            brukerAktiviteterService.syncAktivitetOgBrukerData();
        } else {
            log.info("Starter ikke jobb: oppdaterBrukerData");
        }
    }

    // Denne jobben må kjøre etter midnatt
    @Scheduled(cron = "0 0 1 * * ?")
    public void oppdaterNyeYtelser() {
        if (leaderElectionClient.isLeader()) {
            ytelsesService.oppdaterBrukereMedYtelserSomStarterIDagOracle();
        } else {
            log.info("Starter ikke jobb: oppdaterYtelser");
        }
    }

    // Denne jobben må kjøre etter midnatt
    @Scheduled(cron = "0 1 0 * * ?")
    public void oppdaterBrukerAktiviteterPostgres() {
        if (leaderElectionClient.isLeader()) {
            brukerAktiviteterService.syncAktivitetOgBrukerDataPostgres();
        } else {
            log.info("Starter ikke jobb: oppdaterBrukerData");
        }
    }

    // Denne jobben må kjøre etter midnatt
    @Scheduled(cron = "0 0 1 * * ?")
    public void oppdaterNyeYtelserPostgres() {
        if (leaderElectionClient.isLeader()) {
            ytelsesServicePostgres.oppdaterBrukereMedYtelserSomStarterIDagPostgres();
        } else {
            log.info("Starter ikke jobb: oppdaterYtelser");
        }
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(20);
        taskScheduler.initialize();
        taskRegistrar.setTaskScheduler(taskScheduler);
    }

}
