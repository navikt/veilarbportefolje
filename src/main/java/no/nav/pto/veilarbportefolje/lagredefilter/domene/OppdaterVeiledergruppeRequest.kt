package no.nav.pto.veilarbportefolje.lagredefilter.domene

data class OppdaterVeiledergruppeRequest(
    val filterId: Int,
    val filterNavn: String,
    val veiledere: List<String>,
)
