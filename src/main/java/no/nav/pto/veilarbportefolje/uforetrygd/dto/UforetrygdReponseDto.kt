package no.nav.pto.veilarbportefolje.uforetrygd.dto

import java.time.LocalDate

data class UforetrygdResponseDto(
    val lopendeUforetrygd: Boolean, // hvis true, så er de andre to verdiene garantert å være satt.
    val uforegrad: Int?,
    val forsteVirkningstidspunkt: LocalDate?,
)
