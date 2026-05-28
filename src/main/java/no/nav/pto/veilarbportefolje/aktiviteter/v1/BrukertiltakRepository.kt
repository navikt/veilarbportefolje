package no.nav.pto.veilarbportefolje.aktiviteter.v1

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Repository for [no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKERTILTAK.TABLE_NAME]-tabellen
 */
@Component
class BrukertiltakRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {

    fun hentBrukertiltakBulk(personidenter: Set<String>): Map<String, List<BrukertiltakEntity>> {
        //language=postgresql
        val sql = """
            SELECT aktoerid, tildato, fradato, tiltakskode FROM brukertiltak
            WHERE aktoerid = ANY (:ids::varchar[])
        """.trimIndent()

        TODO()
    }
}

data class BrukertiltakEntity(
    val aktoerId: String?,
    val tildato: LocalDate?,
    val fradato: LocalDate?,
    val tiltakskode: String?
)