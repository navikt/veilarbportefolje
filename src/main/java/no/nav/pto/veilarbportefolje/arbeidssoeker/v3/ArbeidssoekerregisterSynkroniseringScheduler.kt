package no.nav.pto.veilarbportefolje.arbeidssoeker.v3

import no.nav.common.job.leader_election.LeaderElectionClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ArbeidssoekerregisterSynkroniseringScheduler(
    private val arbeidssoekerregisterService: ArbeidssoekerregisterService,
    private val leaderElectionClient: LeaderElectionClient,
) {
    private val logger = LoggerFactory.getLogger(ArbeidssoekerregisterSynkroniseringScheduler::class.java)

    // Hvert 10. minutt.
    @Scheduled(cron = "0 0/10 * * * ?")
    fun synkroniserBrukereMedUsikkerStatus() {
        if (!leaderElectionClient.isLeader) return

        try {
            logger.info("Starter cron-jobb: synkroniserBrukereMedUsikkerStatus")
            arbeidssoekerregisterService.synkroniserBrukereMedUsikkerStatus()
            logger.info("Ferdig med cron-jobb: synkroniserBrukereMedUsikkerStatus")
        } catch (e: Exception) {
            logger.error("Feil under kjøring av arbeidssøkerregister synkronisering cron-jobb", e)
        }
    }
}
