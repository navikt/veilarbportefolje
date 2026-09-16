package no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene

import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg

data class OppdaterFilterRequest(
    val filterId: Int,
    val filterNavn: String,
    val filterValg: Filtervalg
)
