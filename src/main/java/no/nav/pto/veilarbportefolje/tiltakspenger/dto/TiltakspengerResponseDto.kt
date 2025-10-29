package no.nav.pto.veilarbportefolje.tiltakspenger.dto

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet
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
