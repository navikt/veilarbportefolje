package no.nav.pto.veilarbportefolje.domene.frontendmodell

import java.time.LocalDate

data class Ungdomsprogram(
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
    val maksdato: LocalDate,
    val rettighet: String
)
