package no.nav.pto.veilarbportefolje.aktiviteter.v1

import no.nav.common.types.identer.EnhetId
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Service
@Component
class TiltaksaktivitetService(
    val brukertiltakRepository: BrukertiltakRepository,
) {
    fun hentTiltakstyper(
        enhetId: EnhetId,
    ): TiltakskodeMapping {
        return brukertiltakRepository.hentTiltakstyperForEnhet(enhetId)
    }
}