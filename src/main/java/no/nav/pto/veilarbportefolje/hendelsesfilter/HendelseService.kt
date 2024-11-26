package no.nav.pto.veilarbportefolje.hendelsesfilter

import no.nav.pto.veilarbportefolje.kafka.KafkaCommonKeyedConsumerService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class HendelseService : KafkaCommonKeyedConsumerService<HendelseRecordValue>() {
    val log = LoggerFactory.getLogger(HendelseService::class.java)

    override fun behandleKafkaMeldingLogikk(kafkaMelding: HendelseRecordValue, nokkel: String) {
        val hendelse = toHendelse(kafkaMelding, nokkel)
        // TODO: Handtere eventuelle feil i mapping

        when (kafkaMelding.operasjon) {
            Operasjon.START -> startHendelse(hendelse)
            Operasjon.ENDRING -> oppdaterHendelse(hendelse)
            Operasjon.STOPP -> stoppHendelse(hendelse)
        }
    }

    private fun startHendelse(hendelse: Hendelse) {
        log.info("Starter hendelse med id ${hendelse.id}.")
    }

    private fun oppdaterHendelse(hendelse: Hendelse) {
        log.info("Oppdaterer hendelse med id ${hendelse.id}.")
    }

    private fun stoppHendelse(hendelse: Hendelse) {
        log.info("Sletter hendelse med id ${hendelse.id}.")
    }
}