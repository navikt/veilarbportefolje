package no.nav.pto.veilarbportefolje.uforetrygd.domene

import java.time.LocalDate

data class UforetrygdEntity(
    val uføregrad: Int,
    val virkningsdato: LocalDate
)
