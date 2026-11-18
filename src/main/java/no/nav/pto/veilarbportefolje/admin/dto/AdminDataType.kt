package no.nav.pto.veilarbportefolje.admin.dto

import no.nav.common.types.identer.AktorId


enum class AdminDataType(val displayName: String) {
    PDL_DATA("Persondata (PDL)"),
    ENSLIG_FORSORGER_DATA("Enslig forsørger"),
    AAP_DATA("Aap data (kelvin)"),
    DAGPENGER_DATA("Dagpenger (dpsak"),
    TILTAKSPENGER_DATA("Tiltakspenger (tpsak)"),
    ARBEIDSSOKER_DATA("Arbeidssøkerdata"),
    ARBEIDSSOKER_DATA_V3("Arbeidssøkerdata v3"),
    UFORETRYGD_DATA("Uføretrygd"),
    SYKMELDT_DATA("Sykmeldt"),
}

data class AdminDataTypeResponse(
    val name: String,
    val displayName: String
)

data class AdminDataForBrukerRequest(
    val aktorId: AktorId,
    val valg: List<AdminDataType>
)
