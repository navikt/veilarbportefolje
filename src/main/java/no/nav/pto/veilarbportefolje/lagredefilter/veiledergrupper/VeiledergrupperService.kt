package no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper

import no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper.domene.LagretVeiledergruppe
import no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper.domene.NyVeiledergruppeRequest
import no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper.domene.OppdaterVeiledergruppeRequest
import org.springframework.stereotype.Service

@Service
class VeiledergrupperService(private val veiledergrupperRepository: VeiledergrupperRepository) {

    fun hentVeiledergrupperForEnhet(enhetId: String): List<LagretVeiledergruppe> {
        return veiledergrupperRepository.hentVeiledergrupperForEnhet(enhetId)
    }

    fun lagreNyVeiledergruppeForEnhet(
        enhetId: String,
        nyVeildergruppeRequest: NyVeiledergruppeRequest
    ): LagretVeiledergruppe {
        return veiledergrupperRepository.lagreNyVeiledergruppeForEnhet(enhetId, nyVeildergruppeRequest)
    }

    fun oppdaterVeiledergruppeForEnhet(
        enhetId: String,
        oppdaterVeildergruppeRequest: OppdaterVeiledergruppeRequest
    ): LagretVeiledergruppe {
        return veiledergrupperRepository.oppdaterVeiledergruppeForEnhet(enhetId, oppdaterVeildergruppeRequest)
    }

    fun slettVeiledergruppeForEnhet(enhetId: String, filterId: Int): Int {
        return veiledergrupperRepository.slettVeiledergruppeForEnhet(enhetId, filterId)
    }
}
