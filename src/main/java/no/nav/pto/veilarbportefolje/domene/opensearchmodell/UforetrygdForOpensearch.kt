package no.nav.pto.veilarbportefolje.domene.opensearchmodell

import java.time.LocalDate

data class UforetrygdForOpensearch(
    val virkningsdato: LocalDate,
    val uforegrad: Int
)
