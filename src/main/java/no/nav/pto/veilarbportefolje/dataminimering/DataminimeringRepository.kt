package no.nav.pto.veilarbportefolje.dataminimering

import no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_PROFILERING
import no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGING_DATA
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class DataminimeringRepository(private val jdbcTemplate: JdbcTemplate) {

    /**
     * Sletter data i bruker_profilering-tabell for brukere som ikke er under oppfølging
     */
    fun slettBrukerProfileringData(): Int {
        //language=postgresql
        val sqlQuery = """
            DELETE FROM ${BRUKER_PROFILERING.TABLE_NAME} WHERE ${BRUKER_PROFILERING.AKTOERID} NOT IN (
                SELECT DISTINCT ${OPPFOLGING_DATA.AKTOERID} FROM ${OPPFOLGING_DATA.TABLE_NAME} WHERE ${OPPFOLGING_DATA.OPPFOLGING} IS TRUE
            )
            """.trimIndent()

        return jdbcTemplate.update(sqlQuery)
    }
}
