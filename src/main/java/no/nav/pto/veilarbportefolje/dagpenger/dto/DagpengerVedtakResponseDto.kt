package no.nav.pto.veilarbportefolje.dagpenger.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate


data class DagpengerVedtakResponseDto(
    val sakId: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fom: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val tom: LocalDate,
    val rettighet: String,
    val kilde: String,
)
