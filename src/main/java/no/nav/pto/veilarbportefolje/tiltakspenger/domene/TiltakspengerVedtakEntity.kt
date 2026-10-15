package no.nav.pto.veilarbportefolje.tiltakspenger.domene

import java.time.LocalDate
import java.time.LocalDateTime

data class TiltakspengerVedtakEntity(
    val fom: LocalDate,
    val tom: LocalDate,
    val rettighet: TiltakspengerRettighet,
    val radSistEndret: LocalDateTime
)
