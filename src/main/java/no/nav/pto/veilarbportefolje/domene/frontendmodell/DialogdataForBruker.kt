package no.nav.pto.veilarbportefolje.domene.frontendmodell

import java.time.LocalDate

data class DialogdataForBruker(
    val venterPaSvarFraNavDato: LocalDate? = null,
    val venterPaSvarFraBrukerDato: LocalDate? = null,
)
