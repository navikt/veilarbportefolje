package no.nav.pto.veilarbportefolje.aap.domene

import java.time.LocalDate

data class AapKelvinVedtak(
    val nyestePeriodeTom: LocalDate,
    val rettighetstype: AapRettighetstype,
    val maksdato: LocalDate?
)
