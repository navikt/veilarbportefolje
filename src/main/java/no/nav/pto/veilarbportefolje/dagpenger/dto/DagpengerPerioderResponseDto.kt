package no.nav.pto.veilarbportefolje.dagpenger.dto

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.pto.veilarbportefolje.dagpenger.domene.DagpengerRettighetstype
import java.time.LocalDate


data class DagpengerPerioderResponseDto(
    val personIdent: String,
    val perioder: List<DagpengerPeriodeDto>,
)

data class DagpengerPeriodeDto(
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val fraOgMedDato: LocalDate,
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val tilOgMedDato: LocalDate?,
    val ytelseType: DagpengerRettighetstype,
    val kilde: String,
)
