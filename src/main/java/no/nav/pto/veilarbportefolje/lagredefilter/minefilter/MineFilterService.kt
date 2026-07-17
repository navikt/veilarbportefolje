package no.nav.pto.veilarbportefolje.lagredefilter.minefilter

import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.LagretFilter
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.NyttFilterRequest
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.OppdaterFilterRequest
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.SortOrderRequest
import no.nav.pto.veilarbportefolje.lagredefilter.validerFilterNavn
import no.nav.pto.veilarbportefolje.lagredefilter.validerFiltervalg
import no.nav.pto.veilarbportefolje.lagredefilter.validerUnikhet
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class MineFilterService(private val mineFilterRepository: MineFilterRepository) {

    fun hentFilterForVeileder(veilederIdent: String): List<LagretFilter> {
        return mineFilterRepository.hentFilterForVeileder(veilederIdent)
    }

    fun lagreNyttFilterForVeileder(
        veilederIdent: String,
        nyttFilterRequest: NyttFilterRequest
    ): LagretFilter {
        validerFilterNavnEllerKast(nyttFilterRequest.filterNavn)
        validerFiltervalgEllerKast(nyttFilterRequest.filterValg)
        validerUnikhetEllerKast(veilederIdent, nyttFilterRequest.filterNavn, nyttFilterRequest.filterValg)
        return mineFilterRepository.lagreNyttFilterForVeileder(veilederIdent, nyttFilterRequest)
    }

    fun oppdaterLagretFilterForVeileder(
        veilederIdent: String,
        oppdaterFilterRequest: OppdaterFilterRequest
    ): LagretFilter {
        validerFilterNavnEllerKast(oppdaterFilterRequest.filterNavn)
        validerFiltervalgEllerKast(oppdaterFilterRequest.filterValg)
        validerUnikhetEllerKast(
            veilederIdent,
            oppdaterFilterRequest.filterNavn,
            oppdaterFilterRequest.filterValg,
            ekskluderFilterId = oppdaterFilterRequest.filterId
        )
        return mineFilterRepository.oppdaterLagretFilterForVeileder(veilederIdent, oppdaterFilterRequest)
    }

    fun slettFilterForVeileder(veilederIdent: String, filterId: Int): Int {
        return mineFilterRepository.slettFilterForVeileder(veilederIdent, filterId)
    }

    fun lagreSortering(veilederIdent: String, sortOrderRequest: SortOrderRequest): List<LagretFilter> {
        return mineFilterRepository.lagreSortering(veilederIdent, sortOrderRequest)
    }

    private fun validerFilterNavnEllerKast(filterNavn: String) {
        validerFilterNavn(filterNavn)?.let {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, it.message)
        }
    }

    private fun validerFiltervalgEllerKast(filtervalg: Filtervalg) {
        validerFiltervalg(filtervalg)?.let {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, it.message)
        }
    }

    private fun validerUnikhetEllerKast(
        veilederIdent: String,
        filterNavn: String,
        filtervalg: Filtervalg,
        ekskluderFilterId: Int? = null
    ) {
        val navnEksisterer = mineFilterRepository.eksistererFilterNavn(veilederIdent, filterNavn, ekskluderFilterId)
        val valgEksisterer = mineFilterRepository.eksistererFiltervalg(veilederIdent, filtervalg, ekskluderFilterId)
        validerUnikhet(navnEksisterer, valgEksisterer)?.let {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, it.message)
        }
    }
}
