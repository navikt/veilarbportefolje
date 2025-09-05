package no.nav.pto.veilarbportefolje.aap.repository

import java.time.LocalDate

data class AapVedtakPeriode(
    val status: String, //todo map til enums
    val saksid: String,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
)
