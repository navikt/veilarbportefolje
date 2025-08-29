package no.nav.pto.veilarbportefolje.aap.client

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class AapResponseDto(
    val kilde: String,
    val periode: Periode,
    val sakId: String,
    val statusKode: String
) {
    data class Periode(
        @JsonFormat(pattern = "yyyy-MM-dd")
        val fraOgMedDato: LocalDate,

        @JsonFormat(pattern = "yyyy-MM-dd")
        val tilOgMedDato: LocalDate
    )
}
