package no.nav.pto.veilarbportefolje.sykmeldt.domene

import java.time.LocalDate
import java.time.LocalDateTime

data class SykmeldtEntity(
    val start: LocalDate,
    val slutt: LocalDate,
    val harArbeidsgiver: Boolean,
    val radSistEndret: LocalDateTime
)
