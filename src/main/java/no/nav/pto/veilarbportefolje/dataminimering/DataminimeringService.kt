package no.nav.pto.veilarbportefolje.dataminimering

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class DataminimeringService(private val dataminimeringRepository: DataminimeringRepository) {

    // Kjøres klokken 3 hver natt
    @Scheduled(cron = "0 0 3 * * ?")
    fun slettBrukerProfileringData() {
        dataminimeringRepository.slettBrukerProfileringData()
    }
}