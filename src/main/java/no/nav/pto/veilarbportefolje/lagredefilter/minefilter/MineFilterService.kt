package no.nav.pto.veilarbportefolje.lagredefilter.minefilter

import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.LagretFilter
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.NyttFilterRequest
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.OppdaterFilterRequest
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.SortOrderRequest
import org.springframework.stereotype.Service

@Service
class MineFilterService(private val mineFilterRepository: MineFilterRepository) {

    fun hentFilterForVeileder(veilederIdent: String): List<LagretFilter> {
        return mineFilterRepository.hentFilterForVeileder(veilederIdent)
    }

    fun lagreNyttFilterForVeileder(
        veilederIdent: String,
        nyttFilterRequest: NyttFilterRequest
    ): LagretFilter {
        return mineFilterRepository.lagreNyttFilterForVeileder(veilederIdent, nyttFilterRequest)
    }

    fun oppdaterLagretFilterForVeileder(
        veilederIdent: String,
        oppdaterFilterRequest: OppdaterFilterRequest
    ): LagretFilter {
        return mineFilterRepository.oppdaterLagretFilterForVeileder(veilederIdent, oppdaterFilterRequest)
    }

    fun slettFilterForVeileder(veilederIdent: String, filterId: Int): Int {
        return mineFilterRepository.slettFilterForVeileder(veilederIdent, filterId)
    }

    fun lagreSortering(veilederIdent: String, sortOrderRequest: SortOrderRequest): List<LagretFilter> {
        return mineFilterRepository.lagreSortering(veilederIdent, sortOrderRequest)
    }
}
