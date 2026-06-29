package no.nav.pto.veilarbportefolje.domene.opensearchmodell

import java.time.LocalDate

data class UngdomsprogramForOpensearch(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val maksdato: LocalDate,
    val harForlengetPeriode: Boolean
)
