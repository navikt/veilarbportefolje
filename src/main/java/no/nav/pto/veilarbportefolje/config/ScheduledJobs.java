package no.nav.pto.veilarbportefolje.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesService;
import no.nav.pto.veilarbportefolje.database.BrukerAktiviteterService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.util.BatchConsumer;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.erGR199PaKafka;
import static no.nav.pto.veilarbportefolje.config.FeatureToggle.erGR202PaKafka;
import static no.nav.pto.veilarbportefolje.util.BatchConsumer.batchConsumer;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledJobs {
    private final BrukerAktiviteterService brukerAktiviteterService;
    private final YtelsesService ytelsesService;
    private final LeaderElectionClient leaderElectionClient;
    private final UnleashService unleashService;

    @Scheduled(cron = "0 30 0 * * ?")
    public void oppdaterBrukerAktiviteter() {
        if (leaderElectionClient.isLeader() && erGR202PaKafka(unleashService)) {
            brukerAktiviteterService.syncAktivitetOgBrukerData();
        } else {
            log.info("Starter ikke jobb: oppdaterBrukerData");
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void oppdaterNyeYtelser() {
        if (leaderElectionClient.isLeader() && erGR199PaKafka(unleashService)) {
            ytelsesService.oppdaterBrukereMedYtelserSomStarterIDag();
        } else {
            log.info("Starter ikke jobb: oppdaterBrukerData");
        }
    }
}
