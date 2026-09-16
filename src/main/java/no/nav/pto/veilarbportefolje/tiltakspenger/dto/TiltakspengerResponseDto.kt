package no.nav.pto.veilarbportefolje.tiltakspenger.dto

import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet
import java.time.LocalDate

data class TiltakspengerResponseDto(
    val sakId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rettighet: TiltakspengerRettighet,
    val kilde: String,
)
