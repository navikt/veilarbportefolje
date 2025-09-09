package no.nav.pto.veilarbportefolje.aap.repository

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
            "LØPENDE" to LOPENDE,
            "LOPENDE" to LOPENDE,
            "UTREDES" to UTREDES,
            "AVSLUTTET" to AVSLUTTET
        )

        fun fromDb(value: String): AapStatus =
            mapping[value.uppercase()]
                ?: throw IllegalArgumentException("Ukjent AAP status: $value")
    }

}
