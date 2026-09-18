package no.nav.pto.veilarbportefolje.aktiviteter.v1

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.common.client.utils.CacheUtils.tryCacheFirst
import no.nav.common.types.identer.EnhetId
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
@Component
class TiltaksaktivitetService(
    val tiltaksaktivitetRepository: TiltaksaktivitetRepository,
) {
    private val enhetTiltakCachePostgres = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build<EnhetId,TiltakskodeMapping>()

    fun hentTiltakstyper(
        enhetId: EnhetId,
    ): TiltakskodeMapping {

        return tryCacheFirst(enhetTiltakCachePostgres, enhetId) {
            tiltaksaktivitetRepository.hentTiltakstyperForEnhet(enhetId)
        }

    }
}