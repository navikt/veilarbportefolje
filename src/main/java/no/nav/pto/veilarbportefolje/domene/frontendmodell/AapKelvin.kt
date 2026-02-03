package no.nav.pto.veilarbportefolje.domene.frontendmodell

import java.time.LocalDate

data class AapKelvin(
    val vedtaksdatoTilOgMed: LocalDate,
    val rettighetstype: String
)
