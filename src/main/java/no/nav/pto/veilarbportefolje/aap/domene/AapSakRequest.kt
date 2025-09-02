package no.nav.pto.veilarbportefolje.aap.domene

data class AapSakRequest(
    val personidentifikatorer: List<String>
)

data class AapVedtakRequest(
    val personidentifikator: String,
    val fraOgMedDato: String,
    val tilOgMedDato: String
)

