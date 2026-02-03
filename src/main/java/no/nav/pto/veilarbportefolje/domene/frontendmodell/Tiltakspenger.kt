package no.nav.pto.veilarbportefolje.domene.frontendmodell

import java.time.LocalDate

data class Tiltakspenger(
    val vedtaksdatoTilOgMed: LocalDate,
    val rettighet: String
)
