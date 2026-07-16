package no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper.domene

data class NyVeiledergruppeRequest(
    val filterNavn: String,
    val veiledere: List<String>,
)
