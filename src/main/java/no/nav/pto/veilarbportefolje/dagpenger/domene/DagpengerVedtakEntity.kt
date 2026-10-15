package no.nav.pto.veilarbportefolje.dagpenger.domene

import java.time.LocalDate
import java.time.LocalDateTime

data class DagpengerVedtakEntity(
    val nyestePeriodeTom: LocalDate?,
    val rettighetstype: DagpengerRettighetstype,
    val antallResterendeDager: Int?,
    val radSistEndret: LocalDateTime
)
