package no.nav.pto.veilarbportefolje.ungdomsprogram.domene

import java.time.LocalDate
import java.time.LocalDateTime

data class UngdomsprogramPeriodeEntity(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val harForlengetPeriode: Boolean,
    val maksdato: LocalDate,
    val radSistEndret: LocalDateTime
)
