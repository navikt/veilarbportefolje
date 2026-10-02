package no.nav.pto.veilarbportefolje.admin.dto

import java.time.LocalDate

data class AdminBatchjobbRequest(
    val datakilde: AdminDataType,
    val startFra: Int = 0,
    val oppfolgingStartetEtter: LocalDate? = null,
    val oppfolgingStarterFor: LocalDate? = null
)
