package no.nav.pto.veilarbportefolje.aktiviteter.v1

import no.nav.common.types.identer.EnhetId
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Service
@Component
class TiltaksaktivitetService(
    val tiltaksaktivitetRepository: TiltaksaktivitetRepository,
) {
    fun hentTiltakstyper(
        enhetId: EnhetId,
    ): TiltakskodeMapping {
        return tiltaksaktivitetRepository.hentTiltakstyperForEnhet(enhetId)
    }
}