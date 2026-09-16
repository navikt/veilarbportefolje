package no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene

import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg

data class LagretFilter(
    val filterId: Int,
    val filterNavn: String,
    val filterValg: Filtervalg,
    val sortOrder: Int,
    val infoOmSlettetFiltervalg: List<String>? = null //Denne settes i gcp når migrering av lagra filtre sletter ett av flere filtervalg
)
