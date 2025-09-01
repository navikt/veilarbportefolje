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


data class AapResponseMaksimumDto(
    val vedtak: List<Vedtak>
) {
    data class Vedtak(
        val vedtakId: String,
        val status: String,
        val saksnummer: String,
        val vedtaksdato: LocalDate,
        val periode: Periode,
        val rettighetsType: String,
        val kildesystem: String,
        val samordningsId: String? = null,
        val opphorsAarsak: String? = null,
        val vedtaksTypeKode: String?,
        val vedtaksTypeNavn: String?,

    )

    data class Periode(
        @JsonFormat(pattern = "yyyy-MM-dd")
        val fraOgMedDato: LocalDate,

        @JsonFormat(pattern = "yyyy-MM-dd")
        val tilOgMedDato: LocalDate
    )
}
