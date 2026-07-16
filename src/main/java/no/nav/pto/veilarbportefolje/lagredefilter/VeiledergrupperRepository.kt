package no.nav.pto.veilarbportefolje.lagredefilter

import no.nav.pto.veilarbportefolje.database.PostgresTable.LAGREDE_FILTER_VEILEDERGRUPPER.*
import no.nav.pto.veilarbportefolje.lagredefilter.domene.LagretVeiledergruppe
import no.nav.pto.veilarbportefolje.lagredefilter.domene.NyVeiledergruppeRequest
import no.nav.pto.veilarbportefolje.lagredefilter.domene.OppdaterVeiledergruppeRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.ResultSet

@Repository
class VeiledergrupperRepository(private val db: JdbcTemplate) {

    fun hentVeiledergrupperForEnhet(enhetId: String): List<LagretVeiledergruppe> {
        val sql = """
            SELECT *
            FROM ${TABLE_NAME}
            WHERE ${ENHET_ID} = ?
        """.trimIndent()

        return db.query(sql, { rs, _ ->
            LagretVeiledergruppe(
                filterId = rs.getInt(FILTER_ID),
                filterNavn = rs.getString(FILTER_NAVN),
                veiledere = rs.getStringList(VEILEDER_IDENTER)
            )
        }, enhetId)
    }

    fun lagreNyVeiledergruppeForEnhet(
        enhetId: String,
        nyVeiledergruppeRequest: NyVeiledergruppeRequest
    ): LagretVeiledergruppe {

        val sql = """
        INSERT INTO $TABLE_NAME (
            $ENHET_ID,
            $FILTER_NAVN,
            $VEILEDER_IDENTER,
            $RAD_SIST_ENDRET,
            $OPPRETTET
        )
        VALUES (?, ?, ?, now(), now())
        RETURNING $FILTER_ID, $FILTER_NAVN, $VEILEDER_IDENTER
    """.trimIndent()

        return db.query(
            sql,
            { ps: PreparedStatement ->
                ps.setString(1, enhetId)
                ps.setString(2, nyVeiledergruppeRequest.filterNavn)
                ps.setArray(
                    3,
                    ps.connection.createArrayOf("text", nyVeiledergruppeRequest.veiledere.toTypedArray())
                )
            },
            { rs, _ ->
                LagretVeiledergruppe(
                    filterId = rs.getInt(FILTER_ID),
                    filterNavn = rs.getString(FILTER_NAVN),
                    veiledere = rs.getStringList(VEILEDER_IDENTER)
                )
            }
        ).first()

    }

    fun oppdaterVeiledergruppeForEnhet(
        enhetId: String,
        oppdaterVeiledergruppeRequest: OppdaterVeiledergruppeRequest
    ): LagretVeiledergruppe {

        val sql = """
        UPDATE $TABLE_NAME
        SET $FILTER_NAVN = ?,
            $VEILEDER_IDENTER = ?,
            $RAD_SIST_ENDRET = now()
        WHERE $FILTER_ID = ? AND $ENHET_ID = ?
        RETURNING $FILTER_ID, $FILTER_NAVN, $VEILEDER_IDENTER
    """.trimIndent()

        return db.query(
            sql,
            { ps: PreparedStatement ->
                ps.setString(1, oppdaterVeiledergruppeRequest.filterNavn)
                ps.setArray(
                    2,
                    ps.connection.createArrayOf("text", oppdaterVeiledergruppeRequest.veiledere.toTypedArray())
                )
                ps.setInt(3, oppdaterVeiledergruppeRequest.filterId)
                ps.setString(4, enhetId)
            },
            { rs, _ ->
                LagretVeiledergruppe(
                    filterId = rs.getInt(FILTER_ID),
                    filterNavn = rs.getString(FILTER_NAVN),
                    veiledere = rs.getStringList(VEILEDER_IDENTER)
                )
            }
        ).firstOrNull()
            ?: throw NoSuchElementException(
                "Fant ingen veiledergruppe med filterId=${oppdaterVeiledergruppeRequest.filterId} på enhet=$enhetId"
            )
    }

    fun slettVeiledergruppeForEnhet(enhetId: String, filterId: Int): Int {
        val sql = """
            DELETE FROM $TABLE_NAME
            WHERE $FILTER_ID = ? AND $ENHET_ID = ?
        """.trimIndent()

        val antallRaderSletta = db.update(sql, filterId, enhetId)
        if (antallRaderSletta > 0) {
            // todo - opprydning av veiledergruppene i "mine filter", se deactivateMineFilterWithDeletedVeilederGroup i veilarbfilter
        }
        return antallRaderSletta
    }

    private fun ResultSet.getStringList(column: String): List<String> =
        (getArray(column).array as Array<*>).map { it as String }
}
