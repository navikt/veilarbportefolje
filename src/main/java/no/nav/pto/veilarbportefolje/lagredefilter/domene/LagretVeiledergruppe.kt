package no.nav.pto.veilarbportefolje.lagredefilter.domene

data class LagretVeiledergruppe(
    val filterNavn: String,
    val filterId: Int,
    val veiledere: List<String>
)
