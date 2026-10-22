package no.nav.pto.veilarbportefolje.domene.frontendmodell

import java.time.LocalDate

data class Sykmeldt(
    val startdato: LocalDate,
    val sluttdato: LocalDate,
    val harArbeidsgiver: String
)
