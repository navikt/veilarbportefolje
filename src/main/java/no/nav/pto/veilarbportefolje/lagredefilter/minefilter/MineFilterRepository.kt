package no.nav.pto.veilarbportefolje.lagredefilter.minefilter

import no.nav.common.json.JsonUtils
import no.nav.pto.veilarbportefolje.database.PostgresTable.MINE_FILTER.*
import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.LagretFilter
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.NyttFilterRequest
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.OppdaterFilterRequest
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.SortOrderRequest
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.ResultSet

@Repository
class MineFilterRepository(private val db: JdbcTemplate) {

    fun hentFilterForVeileder(veilederIdent: String): List<LagretFilter> {
        val sql = """
            SELECT *
            FROM $TABLE_NAME
            WHERE $VEILEDER_IDENT = ?
            ORDER BY $SORT_ORDER
        """.trimIndent()

        return db.query(sql, { rs, _ -> rs.toLagretFilter() }, veilederIdent)
    }

    fun lagreNyttFilterForVeileder(
        veilederIdent: String,
        nyttFilterRequest: NyttFilterRequest
    ): LagretFilter {
        val sql = """
            INSERT INTO $TABLE_NAME (
                $VEILEDER_IDENT,
                $FILTER_NAVN,
                $AKTIVE_FILTER_VALG,
                $SORT_ORDER,
                $OPPRETTET,
                $RAD_SIST_ENDRET
            )
            VALUES (?, ?, ?, 0, now(), now())
            RETURNING $FILTER_ID, $FILTER_NAVN, $AKTIVE_FILTER_VALG, $SORT_ORDER
        """.trimIndent()

        return db.query(
            sql,
            { ps: PreparedStatement ->
                ps.setString(1, veilederIdent)
                ps.setString(2, nyttFilterRequest.filterNavn)
                ps.setObject(3, nyttFilterRequest.filterValg.toJsonb())
            },
            { rs, _ -> rs.toLagretFilter() }
        ).first()
    }

    fun oppdaterLagretFilterForVeileder(
        veilederIdent: String,
        oppdaterFilterRequest: OppdaterFilterRequest
    ): LagretFilter {
        val sql = """
            UPDATE $TABLE_NAME
            SET $FILTER_NAVN = ?,
                $AKTIVE_FILTER_VALG = ?,
                $RAD_SIST_ENDRET = now()
            WHERE $FILTER_ID = ? AND $VEILEDER_IDENT = ?
            RETURNING $FILTER_ID, $FILTER_NAVN, $AKTIVE_FILTER_VALG, $SORT_ORDER
        """.trimIndent()

        return db.query(
            sql,
            { ps: PreparedStatement ->
                ps.setString(1, oppdaterFilterRequest.filterNavn)
                ps.setObject(2, oppdaterFilterRequest.filterValg.toJsonb())
                ps.setInt(3, oppdaterFilterRequest.filterId)
                ps.setString(4, veilederIdent)
            },
            { rs, _ -> rs.toLagretFilter() }
        ).firstOrNull()
            ?: throw NoSuchElementException(
                "Fant ingen mine filter med filterId=${oppdaterFilterRequest.filterId} for veileder=$veilederIdent"
            )
    }

    fun slettFilterForVeileder(veilederIdent: String, filterId: Int): Int {
        val sql = """
            DELETE FROM $TABLE_NAME
            WHERE $FILTER_ID = ? AND $VEILEDER_IDENT = ?
        """.trimIndent()

        return db.update(sql, filterId, veilederIdent)
    }

    fun lagreSortering(veilederIdent: String, sortOrderRequest: SortOrderRequest): List<LagretFilter> {
        val updateSql = """
            UPDATE $TABLE_NAME
            SET $SORT_ORDER = ?,
                $RAD_SIST_ENDRET = now()
            WHERE $FILTER_ID = ? AND $VEILEDER_IDENT = ?
        """.trimIndent()

        db.update(updateSql, sortOrderRequest.sortOrder, sortOrderRequest.filterId, veilederIdent)

        return hentFilterForVeileder(veilederIdent)
    }

    private fun ResultSet.toLagretFilter(): LagretFilter =
        LagretFilter(
            filterId = getInt(FILTER_ID),
            filterNavn = getString(FILTER_NAVN),
            filterValg = JsonUtils.fromJson(getString(AKTIVE_FILTER_VALG), Filtervalg::class.java),
            sortOrder = getInt(SORT_ORDER),
        )

    private fun Filtervalg.toJsonb(): PGobject =
        PGobject().apply {
            type = "jsonb"
            value = JsonUtils.toJson(this@toJsonb)
        }
}
