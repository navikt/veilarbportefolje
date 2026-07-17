package no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper

import no.nav.pto.veilarbportefolje.lagredefilter.LagredeFilterFeilmeldinger
import no.nav.pto.veilarbportefolje.lagredefilter.validerFilterNavn
import no.nav.pto.veilarbportefolje.lagredefilter.validerUnikhet
import no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper.domene.LagretVeiledergruppe
import no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper.domene.NyVeiledergruppeRequest
import no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper.domene.OppdaterVeiledergruppeRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class VeiledergrupperService(private val veiledergrupperRepository: VeiledergrupperRepository) {

    fun hentVeiledergrupperForEnhet(enhetId: String): List<LagretVeiledergruppe> {
        return veiledergrupperRepository.hentVeiledergrupperForEnhet(enhetId)
    }

    fun lagreNyVeiledergruppeForEnhet(
        enhetId: String,
        nyVeildergruppeRequest: NyVeiledergruppeRequest
    ): LagretVeiledergruppe {
        validerFilterNavnEllerKast(nyVeildergruppeRequest.filterNavn)
        validerVeiledereEllerKast(nyVeildergruppeRequest.veiledere)
        validerUnikhetEllerKast(enhetId, nyVeildergruppeRequest.filterNavn, nyVeildergruppeRequest.veiledere)
        return veiledergrupperRepository.lagreNyVeiledergruppeForEnhet(enhetId, nyVeildergruppeRequest)
    }

    fun oppdaterVeiledergruppeForEnhet(
        enhetId: String,
        oppdaterVeildergruppeRequest: OppdaterVeiledergruppeRequest
    ): LagretVeiledergruppe {
        validerFilterNavnEllerKast(oppdaterVeildergruppeRequest.filterNavn)
        validerVeiledereEllerKast(oppdaterVeildergruppeRequest.veiledere)
        validerUnikhetEllerKast(
            enhetId,
            oppdaterVeildergruppeRequest.filterNavn,
            oppdaterVeildergruppeRequest.veiledere,
            ekskluderFilterId = oppdaterVeildergruppeRequest.filterId
        )
        return veiledergrupperRepository.oppdaterVeiledergruppeForEnhet(enhetId, oppdaterVeildergruppeRequest)
    }

    fun slettVeiledergruppeForEnhet(enhetId: String, filterId: Int): Int {
        return veiledergrupperRepository.slettVeiledergruppeForEnhet(enhetId, filterId)
    }

    private fun validerFilterNavnEllerKast(filterNavn: String) {
        validerFilterNavn(filterNavn)?.let {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, it.message)
        }
    }

    private fun validerVeiledereEllerKast(veiledere: List<String>) {
        if (veiledere.isEmpty()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                LagredeFilterFeilmeldinger.FILTERVALG_TOMT.message
            )
        }
    }

    private fun validerUnikhetEllerKast(
        enhetId: String,
        filterNavn: String,
        veiledere: List<String>,
        ekskluderFilterId: Int? = null
    ) {
        val navnEksisterer = veiledergrupperRepository.eksistererFilterNavn(enhetId, filterNavn, ekskluderFilterId)
        val veiledereEksisterer = veiledergrupperRepository.eksistererVeiledere(enhetId, veiledere, ekskluderFilterId)
        validerUnikhet(navnEksisterer, veiledereEksisterer)?.let {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, it.message)
        }
    }
}
