package no.nav.pto.veilarbportefolje.ytelserkafka

import no.nav.pto.veilarbportefolje.aap.AapService
import no.nav.pto.veilarbportefolje.dagpenger.DagpengerService
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon.Topic
import no.nav.pto.veilarbportefolje.tiltakspenger.TiltakspengerService
import no.nav.pto.veilarbportefolje.uforetrygd.UforetrygdService
import org.springframework.stereotype.Service


/**
 * Håndterer behandling av Kafka-meldinger fra [Topic.YTELSER_TOPIC].
 *
 * Denne klassen håndterer funksjonalitet knyttet til å route ytelses-kafkameldinger til riktig service basert på [YTELSE_TYPE]
 */
@Service
class YtelserKafkaService(
    private val aapService: AapService,
    private val tiltakspengerService: TiltakspengerService,
    private val dagpengerService: DagpengerService,
    private val uforetrygdService: UforetrygdService
) : KafkaCommonNonKeyedConsumerService<YtelserKafkaDTO>() {

    override fun behandleKafkaMeldingLogikk(kafkaMelding: YtelserKafkaDTO) {
        when (kafkaMelding.ytelsestype) {
            YTELSE_TYPE.AAP -> aapService.behandleKafkaMeldingLogikk(kafkaMelding)
            YTELSE_TYPE.TILTAKSPENGER -> tiltakspengerService.behandleKafkaMeldingLogikk(kafkaMelding)
            YTELSE_TYPE.DAGPENGER -> dagpengerService.behandleKafkaMeldingLogikk(kafkaMelding)
            YTELSE_TYPE.UFORETRYGD -> uforetrygdService.behandleKafkaMeldingLogikk(kafkaMelding)
        }
    }

}
