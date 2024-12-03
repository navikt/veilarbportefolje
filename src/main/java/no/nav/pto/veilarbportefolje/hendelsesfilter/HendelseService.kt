package no.nav.pto.veilarbportefolje.hendelsesfilter

import no.nav.pto.veilarbportefolje.kafka.KafkaCommonKeyedConsumerService
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon.Topic
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import org.jetbrains.annotations.TestOnly
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

/**
 * Håndterer behandling av Kafka-meldinger fra [Topic.PORTEFOLJE_HENDELSESFILTER].
 *
 * Topic-et er et generisk topic som andre team kan produsere generelle oppfølgingshendelser på.
 * Bruksområdet for disse hendelsene er i hovedsak å populere statusfiltre i Oversikten (veilarbportefoljeflatefs).
 * Et eksempel på en hendelse er "Utgått varsel" som i skrivende stund produseres av "veilarbdialog"-applikasjonen.
 *
 * Denne klassen håndterer således funksjonalitet knyttet til å starte (les: lagre), oppdatere og stoppe (les: slette)
 * hendelser.
 */
@Service
class HendelseService(
    @Autowired private val hendelseRepository: HendelseRepository,
    @Autowired private val pdlIdentRepository: PdlIdentRepository
) : KafkaCommonKeyedConsumerService<HendelseRecordValue>() {
    private val logger: Logger = LoggerFactory.getLogger(HendelseService::class.java)

    /**
     * Behandle en [HendelseRecordValue] og tilhørende `hendelseId`:
     *
     * * dersom `hendelseRecordValue.operasjon` = [Operasjon.START] vil hendelsen kombineres med ID-en og persisteres
     * * dersom `hendelseRecordValue.operasjon` = [Operasjon.OPPDATER] vil persistert hendelse identifisert med `hendelseId` oppdateres
     * * dersom `hendelseRecordValue.operasjon` = [Operasjon.STOPP] vil persistert hendelse identifisert med `hendelseId` slettes
     */
    override fun behandleKafkaRecordLogikk(hendelseRecordValue: HendelseRecordValue, hendelseId: String) {
        val operasjon = hendelseRecordValue.operasjon
        val hendelse = toHendelse(hendelseRecordValue, hendelseId)

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

    @TestOnly
    fun hentHendelse(id: UUID): Hendelse? {
        return try {
            hendelseRepository.get(id)
        } catch (ex: IngenHendelseMedIdException) {
            null
        }
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