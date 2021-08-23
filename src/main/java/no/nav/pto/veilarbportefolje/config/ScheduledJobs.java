package no.nav.pto.veilarbportefolje.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.aktiviteter.ArenaAktivitetDTO;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.GruppeAktivitetService;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.GruppeAktivitetSchedueldDTO;
import no.nav.pto.veilarbportefolje.database.BrukerAktiviteterService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.util.BatchConsumer;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.erGR202PaKafka;
import static no.nav.pto.veilarbportefolje.util.BatchConsumer.batchConsumer;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledJobs {
    private final AktivitetService aktivitetService;
    private final BrukerAktiviteterService brukerAktiviteterService;
    private final OppfolgingRepository oppfolgingRepository;
    private final GruppeAktivitetService gruppeAktivitetService;
    private final LeaderElectionClient leaderElectionClient;
    private final UnleashService unleashService;

    @Scheduled(cron = "0 1 0 * * ?")
    public void slettUtgatteUtdanningAktivteter() {
        if (leaderElectionClient.isLeader()) {
            List<ArenaAktivitetDTO> utgatteUtdanningAktiviteter = aktivitetService.hentUtgatteUtdanningAktiviteter();
            log.info("Sletter: {} utgatte utdanningaktiviteter", utgatteUtdanningAktiviteter.size());
            utgatteUtdanningAktiviteter.forEach(utgattAktivitet -> aktivitetService.slettUtgatteAktivitet(utgattAktivitet.getAktivitetId(), AktorId.of(utgattAktivitet.getAktoerid())));
        }
    }

    @Scheduled(cron = "0 1 0 * * ?")
    public void slettGruppeAktiviteter() {
        if (leaderElectionClient.isLeader()) {
            List<GruppeAktivitetSchedueldDTO> utgatteGruppeAktiviteter = gruppeAktivitetService.hentUtgatteUtdanningAktiviteter();
            log.info("Inaktiverer: {} utgatte gruppeaktivteter", utgatteGruppeAktiviteter.size());
            utgatteGruppeAktiviteter.forEach(gruppeAktivitet -> gruppeAktivitetService.settSomUtgatt(gruppeAktivitet.getMoteplanId(), gruppeAktivitet.getVeiledningdeltakerId()));
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void oppdaterBrukerAktiviteter() {
        if (leaderElectionClient.isLeader() && erGR202PaKafka(unleashService)) {
            log.info("Starter jobb: oppdater BrukerAktiviteter og BrukerData");
            List<AktorId> brukereSomMaOppdateres = oppfolgingRepository.hentAlleBrukereUnderOppfolging();
            log.info("Oppdaterer brukerdata for alle brukere under oppfolging: {}", brukereSomMaOppdateres.size());
            BatchConsumer<AktorId> consumer = batchConsumer(10_000, brukerAktiviteterService::syncAktivitetOgBrukerData);
            brukereSomMaOppdateres.forEach(consumer);

            consumer.flush();
        } else {
            log.info("Starter ikke jobb: oppdaterBrukerData");
        }
    }
}
