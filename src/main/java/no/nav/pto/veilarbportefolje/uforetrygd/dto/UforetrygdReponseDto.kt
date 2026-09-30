package no.nav.pto.veilarbportefolje.uforetrygd.dto

import java.time.LocalDate

data class UforetrygdResponseDto(
    val uføregrad: Int,
    val virkningsdato: LocalDate,
)
