package no.nav.pto.veilarbportefolje.hendelsesfilter

import no.nav.pto.veilarbportefolje.kafka.KafkaCommonKeyedConsumerService
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class HendelseService(
    @Autowired private val hendelseRepository: HendelseRepository,
    @Autowired private val pdlIdentRepository: PdlIdentRepository,
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

    fun hentHendelse(id: UUID): Hendelse {
        return hendelseRepository.get(id)
    }

    private fun startHendelse(hendelse: Hendelse) {
        try {
            hendelseRepository.insert(hendelse)
            logger.info("Hendelse med id ${hendelse.id} ble startet")
        } catch (ex: HendelseIdEksistererAlleredeException) {
            // TODO: Ignorer melding eller kast exception slik at den blir fanga opp av retry-mekanismen?
            logger.info("Hendelse med ID ${hendelse.id} allerede startet. Ignorerer melding.")
        }
    }

    private fun oppdaterHendelse(hendelse: Hendelse) {
        try {
            hendelseRepository.update(hendelse)
            logger.info("Hendelse med id ${hendelse.id} ble oppdatert")
        } catch (ex: IngenHendelseMedIdException) {
            // TODO: Ignorer melding eller kast exception slik at den blir fanga opp av retry-mekanismen?
            logger.warn("Fikk hendelse med operasjon ${Operasjon.OPPDATER} og ID ${hendelse.id}, men ingen hendelse med denne ID-en finnes. Ignorerer melding.")
        }
    }

    private fun stoppHendelse(hendelse: Hendelse) {
        try {
            hendelseRepository.delete(hendelse.id)
            logger.info("Hendelse med id ${hendelse.id} ble stoppet")
        } catch (ex: IngenHendelseMedIdException) {
            // TODO: Ignorer melding eller kast exception slik at den blir fanga opp av retry-mekanismen?
            logger.warn("Fikk hendelse med operasjon ${Operasjon.STOPP} og ID ${hendelse.id}, men ingen hendelse med denne ID-en finnes. Ignorerer melding.")
        }
    }
}