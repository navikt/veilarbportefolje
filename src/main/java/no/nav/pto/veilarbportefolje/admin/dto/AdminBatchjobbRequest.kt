package no.nav.pto.veilarbportefolje.admin.dto

import java.time.LocalDate

data class AdminBatchjobbRequest(
    val datakilde: AdminDataType,
    val startFra: Int = 0,
    val oppfolgingStartetFra: LocalDate? = null,
    val oppfolgingStarterTil: LocalDate? = null
)
