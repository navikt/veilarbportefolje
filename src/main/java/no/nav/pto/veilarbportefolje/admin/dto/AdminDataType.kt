package no.nav.pto.veilarbportefolje.admin.dto

import no.nav.common.types.identer.AktorId


enum class AdminDataType(val displayName: String) {
    PDL_DATA("Persondata (PDL)"),
    ENSLIG_FORSORGER_DATA("Enslig forsørger")
}

data class AdminDataTypeResponse(
    val name: String,
    val displayName: String
)

data class AdminDataForBrukerRequest(
    val aktorId: AktorId,
    val valg: List<AdminDataType>
)
