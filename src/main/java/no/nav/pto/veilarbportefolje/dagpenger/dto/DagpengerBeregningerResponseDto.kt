package no.nav.pto.veilarbportefolje.dagpenger.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class DagpengerBeregningerResponseDto(
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val fraOgMed: LocalDate,
    val sats: Int,
    val utbetaltBeløp: Int,
    val gjenståendeDager: Int
)


