package no.nav.pto.veilarbportefolje.domene.frontendmodell

import java.time.LocalDateTime

data class MoterMedNav(
    val harAvtaltMoteMedNavIDag: Boolean,
    val forstkommendeMoteDato: LocalDateTime?,
    val forstkommendeMoteVarighetMinutter: Int?,
)
