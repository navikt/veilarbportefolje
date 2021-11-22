package no.nav.pto.veilarbportefolje.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesService;
import no.nav.pto.veilarbportefolje.database.BrukerAktiviteterService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledJobs {
    private final BrukerAktiviteterService brukerAktiviteterService;
    private final YtelsesService ytelsesService;
    private final LeaderElectionClient leaderElectionClient;
    private final ArbeidslisteService arbeidslisteService;

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

    @Scheduled(cron = "0 0 3 * * ?")
    public void migrerArbeidslistaTilPostgresCron() {
        if (leaderElectionClient.isLeader()) {
            log.info("Starter jobb: migrerArbeidslistaTilPostgres");
            arbeidslisteService.migrerArbeidslistaTilPostgres();
        } else {
            log.info("Starter ikke jobb: migrerArbeidslistaTilPostgres");
        }
    }
}
