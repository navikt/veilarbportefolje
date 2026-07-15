package no.nav.pto.veilarbportefolje.lagredefilter

import no.nav.pto.veilarbportefolje.lagredefilter.domene.LagretVeiledergruppe
import no.nav.pto.veilarbportefolje.lagredefilter.domene.NyVeiledergruppeRequest
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
