package no.nav.pto.veilarbportefolje.admin.dto

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr

enum class AdminDataType(val displayName: String) {
    PDL_DATA("Persondata (PDL)"),
    ENSLIG_FORSORGER_DATA("Enslig forsørger"),
    AAP_DATA("Aap data (kelvin)")
}

data class AdminDataTypeResponse(
    val name: String,
    val displayName: String
)

data class AdminDataForBrukerRequest(
    val aktorId: AktorId,
    val valg: List<AdminDataType>
)

data class AdminAktorIdRequest(
    val aktorId: AktorId
)

data class AdminFnrRequest(
    val fnr: Fnr
)
