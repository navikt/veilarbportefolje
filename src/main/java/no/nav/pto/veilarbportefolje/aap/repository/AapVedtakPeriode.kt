package no.nav.pto.veilarbportefolje.aap.repository

import no.nav.pto.veilarbportefolje.aap.domene.VedtakStatus
import java.time.LocalDate

data class AapVedtakPeriode(
    val status: AapStatus,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
)

enum class AapStatus {
    LOPENDE,
    AVSLUTTET,
    UTREDES;

    companion object {
        private val mapping = mapOf(
            VedtakStatus.LØPENDE.toString() to LOPENDE,
            VedtakStatus.UTREDES.toString() to UTREDES,
            VedtakStatus.AVSLUTTET.toString() to AVSLUTTET
        )

        fun fromDb(value: String): AapStatus =
            mapping[value.uppercase()]
                ?: throw IllegalArgumentException("Ukjent AAP status: $value")
    }

}
