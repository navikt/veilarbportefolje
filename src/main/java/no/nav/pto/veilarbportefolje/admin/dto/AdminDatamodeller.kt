package no.nav.pto.veilarbportefolje.admin.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime

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

enum class Opplysningstype {
    SKJERMING
}

sealed interface OpplysningMetadata {
    val database: Any?
    val opensearch: Any?
    val api: Any?
}

data class SkjermingOpplysningMetadata(
    private val databaseSupplier: () -> Database?,
    private val opensearchSupplier: () -> OpenSearch?,
    private val apiSupplier: () -> API?
) : OpplysningMetadata {
    override val database: Database? by lazy { databaseSupplier() }
    override val opensearch: OpenSearch? by lazy { opensearchSupplier() }
    override val api: API? by lazy { apiSupplier() }

    data class Database(
        val erSkjermet: Boolean?,
        val fodselsnummer: Fnr?,
        val skjermetFra: Timestamp?,
        val skjermetTil: Timestamp?
    )

    data class OpenSearch(
        val egenAnsatt: Boolean,
        val skjermetTil: LocalDateTime?
    )

    data class API(
        val egenAnsatt: Boolean,
        val skjermetTil: LocalDate?,
    )
}
