package no.nav.pto.veilarbportefolje.hendelsesfilter

import no.nav.pto.veilarbportefolje.kafka.KafkaCommonKeyedConsumerService
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class HendelseService(
    @Autowired private val hendelseRepository: HendelseRepository,
    private val pdlIdentRepository: PdlIdentRepository,
) : KafkaCommonKeyedConsumerService<HendelseRecordValue>() {
    private val logger: Logger = LoggerFactory.getLogger(HendelseService::class.java)

    override fun behandleKafkaRecordLogikk(hendelseRecordValue: HendelseRecordValue, nokkel: String) {
        val operasjon = hendelseRecordValue.operasjon
        val hendelse = toHendelse(hendelseRecordValue, nokkel)
        // TODO: Handtere eventuelle feil i mapping

        val isUnderArbeidsrettetOppfolging = pdlIdentRepository.erBrukerUnderOppfolging(hendelse.personIdent.get())

        if (!isUnderArbeidsrettetOppfolging) {
            logger.info("Fikk melding på bruker som ikke er under oppfølging. Ignorerer melding.")
            return
        }

        when (operasjon) {
            Operasjon.START -> startHendelse(hendelse)
            Operasjon.OPPDATER -> oppdaterHendelse(hendelse)
            Operasjon.STOPP -> stoppHendelse(hendelse)
        }
    }

    private fun startHendelse(hendelse: Hendelse) {
        hendelseRepository.upsert(hendelse)
        logger.info("Hendelse med id ${hendelse.id} ble startet")
    }

    private fun oppdaterHendelse(hendelse: Hendelse) {
        hendelseRepository.upsert(hendelse)
        logger.info("Hendelse med id ${hendelse.id} ble oppdatert")
    }

    private fun stoppHendelse(hendelse: Hendelse) {
        hendelseRepository.delete(hendelse.id)
        logger.info("Hendelse med id ${hendelse.id} ble stoppet")
    }
}