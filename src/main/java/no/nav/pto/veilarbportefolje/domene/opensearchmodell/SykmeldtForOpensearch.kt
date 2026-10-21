package no.nav.pto.veilarbportefolje.domene.opensearchmodell

import java.time.LocalDate

data class SykmeldtForOpensearch(
    val startdato: LocalDate,
    val sluttdato: LocalDate,
    val harArbeidsgiver: Boolean
)
