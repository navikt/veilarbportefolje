package no.nav.pto.veilarbportefolje.hendelsesfilter

import no.nav.pto.veilarbportefolje.kafka.KafkaCommonKeyedConsumerService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class HendelseService(
    @Autowired private val hendelseRepository: HendelseRepository,
) : KafkaCommonKeyedConsumerService<HendelseRecordValue>() {
    val log = LoggerFactory.getLogger(HendelseService::class.java)

    override fun behandleKafkaRecordLogikk(hendelseRecordValue: HendelseRecordValue, nokkel: String) {
        val hendelse = toHendelse(hendelseRecordValue, nokkel)
        // TODO: Handtere eventuelle feil i mapping

        when (hendelseRecordValue.operasjon) {
            Operasjon.START -> startHendelse(hendelse)
            Operasjon.OPPDATER -> oppdaterHendelse(hendelse)
            Operasjon.STOPP -> stoppHendelse(hendelse)
        }
    }

    private fun startHendelse(hendelse: Hendelse) {
        hendelseRepository.upsert(hendelse)
        log.info("Hendelse med id ${hendelse.id} ble startet")
    }

    private fun oppdaterHendelse(hendelse: Hendelse) {
        hendelseRepository.upsert(hendelse)
        log.info("Hendelse med id ${hendelse.id} ble oppdatert")
    }

    private fun stoppHendelse(hendelse: Hendelse) {
        hendelseRepository.delete(hendelse.id)
        log.info("Hendelse med id ${hendelse.id} ble stoppet")
    }
}