package no.nav.pto.veilarbportefolje.sykefravaer.domene

import java.time.LocalDate
import java.time.LocalDateTime

data class SykefravaerEntity(
    val start: LocalDate,
    val slutt: LocalDate,
    val harArbeidsgiver: Boolean,
    val radSistEndret: LocalDateTime
)
