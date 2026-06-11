package no.nav.pto.veilarbportefolje.ungdomsprogram

import no.nav.common.job.leader_election.LeaderElectionClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class UngdomsprogramScheduler(
    private val ungdomsprogramService: UngdomsprogramService,
    private val leaderElectionClient: LeaderElectionClient,
) {
    private val logger = LoggerFactory.getLogger(UngdomsprogramScheduler::class.java)

    // Hvert 10. minutt.
    @Scheduled(cron = "0 0/10 * * * ?")
    fun hentUngdomsprogramForAlleBrukere() {
        if (!leaderElectionClient.isLeader) return

        try {
            logger.info("Starter cron-jobb: hentUngdomsprogramForAlleBrukere")
            ungdomsprogramService.hentUngdomsprogramForAlleBrukere()
            logger.info("Ferdig med cron-jobb: hentUngdomsprogramForAlleBrukere")
        } catch (e: Exception) {
            logger.error("Feil under kjøring av ungdomsprogram cron-jobb", e)
        }
    }
}
