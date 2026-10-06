package no.nav.pto.veilarbportefolje.domene.frontendmodell

import java.time.LocalDate

data class Uforetrygd(
    val virkningsdato: LocalDate,
    val uforegrad: String
)
