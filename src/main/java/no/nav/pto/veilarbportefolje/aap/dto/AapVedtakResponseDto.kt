package no.nav.pto.veilarbportefolje.aap.dto

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakStatus
import java.time.LocalDate


data class AapVedtakResponseDto(
    val vedtak: List<Vedtak>
) {
    data class Vedtak(
        val status: AapVedtakStatus,
        val saksnummer: String,
        val periode: Periode,
        val rettighetsType: AapRettighetstype,
        val opphorsAarsak: String? = null, // ikke klar og vil bli erstattet med noe annet
    )

    data class Periode(
        @param:JsonFormat(pattern = "yyyy-MM-dd")
        val fraOgMedDato: LocalDate,

        @param:JsonFormat(pattern = "yyyy-MM-dd")
        val tilOgMedDato: LocalDate
    )
}

