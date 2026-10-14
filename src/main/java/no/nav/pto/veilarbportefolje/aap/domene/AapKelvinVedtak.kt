package no.nav.pto.veilarbportefolje.aap.domene

import java.time.LocalDate
import java.time.LocalDateTime

data class AapKelvinVedtak(
    val nyestePeriodeTom: LocalDate,
    val rettighetstype: AapRettighetstype,
    val maksdato: LocalDate?,
    val radSistEndret: LocalDateTime
)
