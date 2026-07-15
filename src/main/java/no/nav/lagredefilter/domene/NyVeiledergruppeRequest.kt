package no.nav.lagredefilter.domene

data class NyVeiledergruppeRequest(
    val filterNavn: String,
    val veiledere: List<String>,
)
