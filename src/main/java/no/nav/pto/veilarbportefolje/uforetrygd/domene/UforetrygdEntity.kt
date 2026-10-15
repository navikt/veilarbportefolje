package no.nav.pto.veilarbportefolje.uforetrygd.domene

import java.time.LocalDate
import java.time.LocalDateTime

data class UforetrygdEntity(
    val uføregrad: Int,
    val virkningsdato: LocalDate,
    val radSistEndret: LocalDateTime
)
