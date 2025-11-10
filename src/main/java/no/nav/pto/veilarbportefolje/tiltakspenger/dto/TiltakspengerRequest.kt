package no.nav.pto.veilarbportefolje.tiltakspenger.dto

data class TiltakspengerRequest(
    val ident: String,
    val fom: String,
    val tom: String? = null,
)
