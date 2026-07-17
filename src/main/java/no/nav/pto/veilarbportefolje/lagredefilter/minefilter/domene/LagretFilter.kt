package no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene

import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg

data class LagretFilter(
    val filterId: Int,
    val filterNavn: String,
    val filterValg: Filtervalg,
    val sortOrder: Int,
    val aktiv: Boolean,
    val ikkeAktivBeskrivelse: String
)
