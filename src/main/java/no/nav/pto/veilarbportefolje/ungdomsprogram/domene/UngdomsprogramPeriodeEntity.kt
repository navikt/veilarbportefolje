package no.nav.pto.veilarbportefolje.ungdomsprogram.domene

import java.time.LocalDate

data class UngdomsprogramPeriodeEntity(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val harForlengetPeriode: Boolean,
    val maksdato: LocalDate
)
