package no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper.domene

data class OppdaterVeiledergruppeRequest(
    val filterId: Int,
    val filterNavn: String,
    val veiledere: List<String>,
)
