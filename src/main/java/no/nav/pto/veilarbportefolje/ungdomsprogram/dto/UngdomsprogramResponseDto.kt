package no.nav.pto.veilarbportefolje.ungdomsprogram.dto

import java.time.LocalDate
import java.util.UUID


data class UngdomsprogramResponseDto(
    val deltakelser: List<Deltakelse>
)

data class Deltakelse(
    val deltakelseId: UUID,
    val deltakerIdent: String,
    val periode: Periode
)

data class Periode(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val harForlengetPeriode: Boolean,
    val periodeMaksDato: LocalDate
)
