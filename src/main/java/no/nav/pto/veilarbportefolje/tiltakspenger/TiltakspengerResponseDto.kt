package no.nav.pto.veilarbportefolje.tiltakspenger

data class TiltakspengerResponseDto(
    val sakId: String,
    val fom: String,
    val tom: String,
    val rettighet: TiltakspengerRettighet,
    val kilde: String,
)
