package no.nav.pto.veilarbportefolje.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.aktiviteter.ArenaAktivitetDTO;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.GruppeAktivitetService;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.GruppeAktivitetSchedueldDTO;
import no.nav.pto.veilarbportefolje.database.BrukerDataService;
import no.nav.pto.veilarbportefolje.util.BatchConsumer;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

import static no.nav.pto.veilarbportefolje.util.BatchConsumer.batchConsumer;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledJobs {
    private final AktivitetService aktivitetService;
    private final BrukerDataService brukerDataService;
    private final GruppeAktivitetService gruppeAktivitetService;
    private final LeaderElectionClient leaderElectionClient;

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


    @Scheduled(cron = "0 0 * * * ?")
    public void oppdater() {
        log.info("Debug jobb start min.");
        if (leaderElectionClient.isLeader()) {
            log.info("Debug jobb min.");
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void oppdaterBrukerData() {
        log.info("Debug jobb start");
        if (leaderElectionClient.isLeader()) {
            log.debug("Starter jobb: oppdaterBrukerData");
            List<AktorId> brukereSomMaOppdateres = brukerDataService.hentBrukerSomMaOppdaters();
            log.info("Oppdaterer brukerdata for: {} brukere", brukereSomMaOppdateres.size());

            BatchConsumer<AktorId> consumer = batchConsumer(1000, brukerDataService::oppdaterAktivitetBrukerDataOgHentPersonId);
            brukereSomMaOppdateres.forEach(consumer);

            consumer.flush();
        } else {
            log.debug("Starter ikke jobb: oppdaterBrukerData");
        }
    }
}
