package no.nav.pto.veilarbportefolje.aap.repository

import java.time.LocalDate

data class AapVedtakPeriode(
    val status: AapStatus,
    val saksid: String,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
)

enum class AapStatus {
    LØPENDE,
    AVSLUTTET,
    UTREDES
}
