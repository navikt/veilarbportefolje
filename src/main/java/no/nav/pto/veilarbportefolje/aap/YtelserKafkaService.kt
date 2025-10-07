package no.nav.pto.veilarbportefolje.aap

import no.nav.pto.veilarbportefolje.aap.domene.YTELSE_TYPE
import no.nav.pto.veilarbportefolje.aap.domene.YtelserKafkaDTO
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon.Topic
import no.nav.pto.veilarbportefolje.tiltakspenger.TiltakspengerService
import org.springframework.stereotype.Service


/**
 * Håndterer behandling av Kafka-meldinger fra [Topic.YTELSER_TOPIC].
 *
 * Denne klassen håndterer funksjonalitet knyttet til å route ytelses-kafkameldinger til riktig service basert på [YTELSE_TYPE]
 */
@Service
class YtelserKafkaService(
    private val aapService: AapService,
    private val tiltakspengerService: TiltakspengerService
) : KafkaCommonNonKeyedConsumerService<YtelserKafkaDTO>() {

    override fun behandleKafkaMeldingLogikk(kafkaMelding: YtelserKafkaDTO) {
        when (kafkaMelding.ytelsestype) {
            YTELSE_TYPE.AAP -> aapService.behandleKafkaMeldingLogikk(kafkaMelding)
            YTELSE_TYPE.TILTAKSPENGER -> tiltakspengerService.behandleKafkaMeldingLogikk(kafkaMelding)
        }
    }

}
