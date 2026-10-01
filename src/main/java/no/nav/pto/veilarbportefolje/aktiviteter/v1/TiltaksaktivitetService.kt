package no.nav.pto.veilarbportefolje.aktiviteter.v1

import no.nav.common.types.identer.EnhetId
import no.nav.pto.veilarbportefolje.aktiviteter.dto.TiltakskodeverkDTO
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Service
@Component
class TiltaksaktivitetService(
    val tiltaksaktivitetRepository: TiltaksaktivitetRepository,
): KafkaCommonNonKeyedConsumerService<TiltakskodeverkDTO>() {

    override fun behandleKafkaMeldingLogikk(kafkaMelding: TiltakskodeverkDTO) {
        tiltaksaktivitetRepository.lagreTiltakskodenavn(kafkaMelding.tiltakskode, kafkaMelding.navn)
    }

    fun hentTiltakstyper(
        enhetId: EnhetId,
    ): TiltakskodeMapping {
        return tiltaksaktivitetRepository.hentTiltakstyperForEnhet(enhetId)
    }
}