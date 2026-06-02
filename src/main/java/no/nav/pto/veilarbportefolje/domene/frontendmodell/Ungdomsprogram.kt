package no.nav.pto.veilarbportefolje.domene.frontendmodell

import java.time.LocalDate

data class Ungdomsprogram(
    val startdatoPeriode: LocalDate,
    val maksdatoPeriode: LocalDate,
    val sluttdato: LocalDate?,
    val typePeriode: String
)
