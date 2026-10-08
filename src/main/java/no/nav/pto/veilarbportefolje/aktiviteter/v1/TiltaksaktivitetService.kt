package no.nav.pto.veilarbportefolje.aktiviteter.v1

import no.nav.common.types.identer.EnhetId
import no.nav.pto.veilarbportefolje.aktiviteter.dto.TiltakskodeverkDTO
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService
import no.nav.pto.veilarbportefolje.kodeverk.CacheConfig
import org.springframework.cache.annotation.Cacheable
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

    @Cacheable(CacheConfig.ENHETS_TILTAKTYPER_CACHE_NAME)
    fun hentTiltakstyper(enhetId: EnhetId): TiltakskodeMapping {
        return tiltaksaktivitetRepository.hentTiltakstyperForEnhet(enhetId)
    }
}