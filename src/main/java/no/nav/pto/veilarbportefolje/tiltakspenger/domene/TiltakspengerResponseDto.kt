package no.nav.pto.veilarbportefolje.tiltakspenger.domene

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class TiltakspengerResponseDto(
    val sakId: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fom: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val tom: LocalDate,
    val rettighet: TiltakspengerRettighet,
    val kilde: String,
)
