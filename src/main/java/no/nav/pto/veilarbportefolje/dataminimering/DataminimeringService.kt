package no.nav.pto.veilarbportefolje.dataminimering

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class DataminimeringService(private val dataminimeringRepository: DataminimeringRepository) {

    val log: Logger = LoggerFactory.getLogger(DataminimeringService::class.java)

    companion object {
        const val JOBB_NAVN = "slettBrukerProfileringData"
    }

    // Kjøres klokken 3 hver natt
    @Scheduled(cron = "0 0 3 * * ?")
    fun slettBrukerProfileringData() {
        log.info("Jobb $JOBB_NAVN: starter.")

        try {
            dataminimeringRepository.slettBrukerProfileringData()
                .also { log.info("Jobb $JOBB_NAVN: suksess. Slettet $it rader med data fra bruker_profilering.") }
        } catch (e: DataAccessException) {
            log.error("Jobb $JOBB_NAVN: feilet.", e)
        }
    }
}