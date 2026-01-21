package no.nav.pto.veilarbportefolje.dagpenger.dto

import com.fasterxml.jackson.annotation.JsonFormat

data class DagpengerBeregningerResponseDto(
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val dato: String,
    val sats: Int,
    val utbetaltBeløp: Int,
    val gjenståendeDager: Int
)


