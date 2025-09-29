package no.nav.pto.veilarbportefolje.aap.domene

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate


data class AapVedtakResponseDto(
    val vedtak: List<Vedtak>
) {
    data class Vedtak(
        val status: String,
        val saksnummer: String,
        val periode: Periode,
        val rettighetsType: String,
        val kildesystem: String,
        val opphorsAarsak: String? = null,
    )

    data class Periode(
        @JsonFormat(pattern = "yyyy-MM-dd")
        val fraOgMedDato: LocalDate,

        @JsonFormat(pattern = "yyyy-MM-dd")
        val tilOgMedDato: LocalDate
    )
}

