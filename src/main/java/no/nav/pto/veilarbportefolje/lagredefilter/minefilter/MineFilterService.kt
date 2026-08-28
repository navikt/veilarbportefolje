package no.nav.pto.veilarbportefolje.lagredefilter.minefilter

import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg
import no.nav.pto.veilarbportefolje.lagredefilter.harGyldigFilterNavn
import no.nav.pto.veilarbportefolje.lagredefilter.harUniktNavnOgFiltervalg
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.LagretFilter
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.NyttFilterRequest
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.OppdaterFilterRequest
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.SortOrderRequest
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
        val aktiveFiltervalg = ekstraherAktiveFiltervalg(nyttFilterRequest.filterValg)
        validerFilterNavnEllerKast(nyttFilterRequest.filterNavn)
        validerFiltervalgEllerKast(nyttFilterRequest.filterValg)
        validerUnikhetEllerKast(veilederIdent, nyttFilterRequest.filterNavn, aktiveFiltervalg)
        return mineFilterRepository.lagreNyttFilterForVeileder(
            veilederIdent = veilederIdent,
            filterNavn = nyttFilterRequest.filterNavn,
            aktiveFiltervalg = aktiveFiltervalg
        )
    }

    fun oppdaterLagretFilterForVeileder(
        veilederIdent: String,
        oppdaterFilterRequest: OppdaterFilterRequest
    ): LagretFilter {
        val aktiveFiltervalg = ekstraherAktiveFiltervalg(oppdaterFilterRequest.filterValg)
        validerFilterNavnEllerKast(oppdaterFilterRequest.filterNavn)
        validerFiltervalgEllerKast(oppdaterFilterRequest.filterValg)
        validerUnikhetEllerKast(
            veilederIdent,
            oppdaterFilterRequest.filterNavn,
            aktiveFiltervalg,
            ekskluderFilterId = oppdaterFilterRequest.filterId
        )
        return mineFilterRepository.oppdaterLagretFilterForVeileder(
            veilederIdent = veilederIdent,
            filterId = oppdaterFilterRequest.filterId,
            filterNavn = oppdaterFilterRequest.filterNavn,
            aktiveFiltervalg = aktiveFiltervalg
        )
    }

    fun slettFilterForVeileder(veilederIdent: String, filterId: Int): Int {
        return mineFilterRepository.slettFilterForVeileder(veilederIdent, filterId)
    }

    fun lagreSortering(veilederIdent: String, sortOrderRequest: List<SortOrderRequest>): List<LagretFilter> {
        return mineFilterRepository.lagreSortering(veilederIdent, sortOrderRequest)
    }

    private fun validerFilterNavnEllerKast(filterNavn: String) {
        if (!harGyldigFilterNavn(filterNavn)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
    }

    private fun validerFiltervalgEllerKast(filtervalg: Filtervalg) {
        if (!filtervalg.harAktiveFilter()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
    }

    private fun validerUnikhetEllerKast(
        veilederIdent: String,
        filterNavn: String,
        aktiveFiltervalg: AktiveFiltervalg,
        ekskluderFilterId: Int? = null
    ) {
        val navnEksisterer = mineFilterRepository.eksistererFilterNavn(veilederIdent, filterNavn, ekskluderFilterId)
        val valgEksisterer =
            mineFilterRepository.eksistererFiltervalg(veilederIdent, aktiveFiltervalg, ekskluderFilterId)
        if (!harUniktNavnOgFiltervalg(navnEksisterer, valgEksisterer)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
    }
}
