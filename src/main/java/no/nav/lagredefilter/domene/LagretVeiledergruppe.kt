package no.nav.lagredefilter.domene

data class LagretVeiledergruppe(
    val filterNavn: String,
    val filterId: Int,
    val veiledere: List<String>
)
