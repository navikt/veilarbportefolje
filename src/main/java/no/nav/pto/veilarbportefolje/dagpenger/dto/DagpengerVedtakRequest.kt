package no.nav.pto.veilarbportefolje.dagpenger.dto


data class DagpengerVedtakRequest(
    val personIdent: String,
    val fraOgMedDato: String,
    val tilOgMedDato: String
)
