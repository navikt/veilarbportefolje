package no.nav.pto.veilarbportefolje.domene.frontendmodell

import java.time.LocalDateTime

data class MoterMedNavIDag(
    val harAvtaltMoteMedNavIDag: Boolean,
    val forstkommendeMoteTidspunkt: LocalDateTime?,
    val forstkommendeMoteVarighet: Int?,
)
