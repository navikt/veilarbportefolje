package no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene

import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg

data class NyttFilterRequest(
    val filterNavn: String,
    val filterValg: Filtervalg
)
