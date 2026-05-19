package no.nav.pto.veilarbportefolje.aap.dto

import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakStatus
import java.time.LocalDate


data class AapVedtakResponseDto(
    val vedtak: List<Vedtak>,
    val sakstatus: String,
    val maksdato: LocalDate?  // kan være null i starten, men etterhvert som maksdatoer blir generert skal den alltid komme med
) {
    data class Vedtak(
        val status: AapVedtakStatus,
        val saksnummer: String,
        val periode: Periode,
        val rettighetsType: AapRettighetstype,
    )

    data class Periode(
        val fraOgMedDato: LocalDate,
        val tilOgMedDato: LocalDate
    )
}

