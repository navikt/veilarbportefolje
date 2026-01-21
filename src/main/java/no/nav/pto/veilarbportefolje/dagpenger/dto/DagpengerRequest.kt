package no.nav.pto.veilarbportefolje.dagpenger.dto


data class DagpengerRequest(
    val personIdent: String,
    val fraOgMedDato: String,
    val tilOgMedDato: String?
)
