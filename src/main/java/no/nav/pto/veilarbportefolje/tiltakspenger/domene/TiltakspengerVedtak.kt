package no.nav.pto.veilarbportefolje.tiltakspenger.domene

import java.time.LocalDate

data class TiltakspengerVedtak (
    val sakId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rettighet: TiltakspengerRettighet
)
