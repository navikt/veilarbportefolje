package no.nav.pto.veilarbportefolje.aap.client

data class AapRequest(
    val personidentifikatorer: List<String>
)

data class AapIPeriodeRequest(
    val personidentifikator: String,
    val fraOgMedDato: String,
    val tilOgMedDato: String
)

