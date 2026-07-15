package no.nav.pto.veilarbportefolje.lagredefilter

import no.nav.pto.veilarbportefolje.lagredefilter.domene.LagretVeiledergruppe
import no.nav.pto.veilarbportefolje.lagredefilter.domene.NyVeiledergruppeRequest
import no.nav.pto.veilarbportefolje.lagredefilter.domene.OppdaterVeiledergruppeRequest
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
}
