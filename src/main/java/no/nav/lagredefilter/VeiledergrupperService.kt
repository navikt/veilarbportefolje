package no.nav.lagredefilter

import no.nav.lagredefilter.domene.LagretVeiledergruppe
import no.nav.lagredefilter.domene.NyVeiledergruppeRequest
import org.springframework.stereotype.Service

@Service
class VeiledergrupperService(private val veiledergrupperRepository: VeiledergrupperRepository) {


    fun hentVeiledergrupperForEnhet(enhetId: String): List<LagretVeiledergruppe> {
        return veiledergrupperRepository.hentVeiledergrupperForEnhet(enhetId)
    }


    fun lagreNyVeiledergrupperForEnhet(enhetId: String, nyVeildergruppeRequest: NyVeiledergruppeRequest): LagretVeiledergruppe {
        return veiledergrupperRepository.lagreNyVeiledergruppeForEnhet(enhetId, nyVeildergruppeRequest)
    }
}
