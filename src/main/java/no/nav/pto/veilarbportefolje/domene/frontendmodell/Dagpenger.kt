package no.nav.pto.veilarbportefolje.domene.frontendmodell

import java.time.LocalDate

data class Dagpenger(
    val rettighetstype: String,
    val datoPlanlagtStans: LocalDate?,
    val resterendeDager: String?,
)
