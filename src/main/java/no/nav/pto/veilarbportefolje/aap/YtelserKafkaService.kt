package no.nav.pto.veilarbportefolje.aap

import no.nav.pto.veilarbportefolje.aap.domene.YTELSE_KILDESYSTEM
import no.nav.pto.veilarbportefolje.aap.domene.YTELSE_TYPE
import no.nav.pto.veilarbportefolje.aap.domene.YtelserKafkaDTO
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class YtelserKafkaService(
    private val aapService: AapService
) : KafkaCommonNonKeyedConsumerService<YtelserKafkaDTO>() {
    private val logger: Logger = LoggerFactory.getLogger(YtelserKafkaService::class.java)

    override fun behandleKafkaMeldingLogikk(kafkaMelding: YtelserKafkaDTO) {
        if (kafkaMelding.ytelsestype != YTELSE_TYPE.AAP ) {
            logger.warn("Mottok ytelse-melding med uventet ytelsestype: ${kafkaMelding.ytelsestype}, forventet AAP. Ignorerer melding.")
            return
        }

        if (kafkaMelding.kildesystem != YTELSE_KILDESYSTEM.KELVIN) {
            logger.warn("Mottok ytelse-melding med uventet kildesystem: ${kafkaMelding.kildesystem}, forventet KELVIN. Ignorerer melding.")
            return
        }
        aapService.behandleKafkaMeldingLogikk(kafkaMelding)
    }
}
