package no.nav.pto.veilarbportefolje.aap.dto

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
        val kildesystem: String,
        val opphorsAarsak: String? = null,
    )

    data class Periode(
        val fraOgMedDato: LocalDate,

        val tilOgMedDato: LocalDate
    )
}

