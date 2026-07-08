package no.nav.pto.veilarbportefolje.admin.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
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

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes(
    JsonSubTypes.Type(AdminAktorIdRequest::class),
    JsonSubTypes.Type(AdminFnrRequest::class)
)
sealed interface AdminIdRequest

data class AdminAktorIdRequest(
    val aktorId: AktorId
) : AdminIdRequest

data class AdminFnrRequest(
    val fnr: Fnr
) : AdminIdRequest

data class Identer(val fnr: Fnr, val aktorId: AktorId)
