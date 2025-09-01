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
        val barnMedStonad: Int,
        val barnetillegg: Int,
        val beregningsgrunnlag: Int,
        val dagsats: Int,
        val dagsatsEtterUføreReduksjon: Int,
        val kildesystem: String,
        val opphorsAarsak: String?,
        val periode: Periode,
        val rettighetsType: String,
        val saksnummer: String,
        val samordningsId: String,
        val status: String,
        val vedtakId: String,
        val vedtaksTypeKode: String,
        val vedtaksTypeNavn: String,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val vedtaksdato: LocalDate
    )

    data class Periode(
        @JsonFormat(pattern = "yyyy-MM-dd")
        val fraOgMedDato: LocalDate,

        @JsonFormat(pattern = "yyyy-MM-dd")
        val tilOgMedDato: LocalDate
    )
}
