package no.nav.pto.veilarbportefolje.domene.frontendmodell

import java.time.LocalDate

data class StatsborgerskapForBruker(
    val statsborgerskap: String? = null,
    val gyldigFra: LocalDate? = null
)
