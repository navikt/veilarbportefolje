package no.nav.pto.veilarbportefolje.dagpenger.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate


data class DagpengerVedtakResponseDto(
    val personIdent: String,
    val perioder: List<DagpengerPeriodeDto>,
)

data class DagpengerPeriodeDto(
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val fraOgMedDato: LocalDate,
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val tilOgMedDato: LocalDate?,
    val ytelseType: String,
    val kilde: String,
)
