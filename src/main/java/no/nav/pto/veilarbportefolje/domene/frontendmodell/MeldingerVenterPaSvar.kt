package no.nav.pto.veilarbportefolje.domene.frontendmodell

import java.time.LocalDate

data class MeldingerVenterPaSvar(
    val datoMeldingVenterPaNav: LocalDate?,
    val datoMeldingVenterPaBruker: LocalDate?,
)
