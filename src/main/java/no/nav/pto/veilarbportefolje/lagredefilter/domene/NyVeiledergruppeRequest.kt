package no.nav.pto.veilarbportefolje.lagredefilter.domene

data class NyVeiledergruppeRequest(
    val filterNavn: String,
    val veiledere: List<String>,
)
