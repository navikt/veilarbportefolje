package no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene

data class HentLagretFilterResponse(
    val filtre: List<LagretFilter>,
    val antallFiltreSomFeilet: Int
)
