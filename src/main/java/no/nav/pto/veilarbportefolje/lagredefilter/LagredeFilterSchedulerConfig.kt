package no.nav.pto.veilarbportefolje.lagredefilter

import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper.VeiledergrupperService
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit

class LagredeFilterSchedulerConfig(
    private val veilederGrupperService: VeiledergrupperService,
    private val leaderElectionClient: LeaderElectionClient
) {
    private val log = org.slf4j.LoggerFactory.getLogger(LagredeFilterSchedulerConfig::class.java)

    @Scheduled(fixedDelay = 30, initialDelay = 2, timeUnit = TimeUnit.MINUTES)
    fun fjernVeiledereSomErIkkeAktive() {
        if (leaderElectionClient.isLeader) {
            try {
                log.info("Fjern veiledere som er ikke aktive...")
                veilederGrupperService.slettVeiledereSomIkkeErAktiveForHverEnhet()
                log.info("Fjern veiledere som er ikke aktive er ferdig")
            } catch (e: Exception) {
                log.warn("Exception during clanup $e", e)
            }
        } else {
            log.info("Starter ikke jobb: fjernVeiledereSomErIkkeAktive")
        }
    }
}


