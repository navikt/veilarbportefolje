package no.nav.pto.veilarbportefolje.aap.domene

import java.time.LocalDate

data class AapVedtakPeriode(
    val status: AapVedtakStatus,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
)
