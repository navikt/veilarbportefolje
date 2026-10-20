package no.nav.pto.veilarbportefolje.sykefravaer

import no.nav.poao_tilgang.client.NorskIdent
import no.nav.pto.veilarbportefolje.database.PostgresTable.SYKEFRAVAER_OPPFOLGING
import no.nav.pto.veilarbportefolje.sykefravaer.domene.SykefravaerEntity
import no.nav.pto.veilarbportefolje.sykefravaer.dto.OppfolgingstilfelleDTO
import org.jetbrains.annotations.TestOnly
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class SykefravaerRepository(
    private val db: JdbcTemplate,
    private val namedDb: NamedParameterJdbcTemplate
) {

    fun upsertSykefravaerOppfolgingsperiode(norskIdent: NorskIdent, oppfolgingstilfelle: OppfolgingstilfelleDTO) {
        db.update(
            """
            INSERT INTO ${SYKEFRAVAER_OPPFOLGING.TABLE_NAME} (
                ${SYKEFRAVAER_OPPFOLGING.NORSK_IDENT},
                ${SYKEFRAVAER_OPPFOLGING.START},
                ${SYKEFRAVAER_OPPFOLGING.SLUTT},
                ${SYKEFRAVAER_OPPFOLGING.HAR_ARBEIDSGIVER},
                ${SYKEFRAVAER_OPPFOLGING.RAD_SIST_ENDRET}
            ) VALUES (?,?,?,?,current_timestamp)
            ON CONFLICT (${SYKEFRAVAER_OPPFOLGING.NORSK_IDENT})
            DO UPDATE SET (
                ${SYKEFRAVAER_OPPFOLGING.START},
                ${SYKEFRAVAER_OPPFOLGING.SLUTT},
                ${SYKEFRAVAER_OPPFOLGING.HAR_ARBEIDSGIVER},
                ${SYKEFRAVAER_OPPFOLGING.RAD_SIST_ENDRET}
            ) = (
                excluded.${SYKEFRAVAER_OPPFOLGING.START},
                excluded.${SYKEFRAVAER_OPPFOLGING.SLUTT},
                excluded.${SYKEFRAVAER_OPPFOLGING.HAR_ARBEIDSGIVER},
                excluded.${SYKEFRAVAER_OPPFOLGING.RAD_SIST_ENDRET}
            ) """,
            norskIdent,
            oppfolgingstilfelle.start,
            oppfolgingstilfelle.end,
            oppfolgingstilfelle.arbeidstakerAtTilfelleEnd
        )
    }

    fun hentSykefravaerPerioder(norskeIdenter: List<String>): Map<String, SykefravaerEntity> {
        if (norskeIdenter.isEmpty()) return emptyMap()

        val sql = """
            SELECT
            ${SYKEFRAVAER_OPPFOLGING.NORSK_IDENT},
            ${SYKEFRAVAER_OPPFOLGING.START},
            ${SYKEFRAVAER_OPPFOLGING.SLUTT},
            ${SYKEFRAVAER_OPPFOLGING.HAR_ARBEIDSGIVER},
            ${SYKEFRAVAER_OPPFOLGING.RAD_SIST_ENDRET}
            FROM ${SYKEFRAVAER_OPPFOLGING.TABLE_NAME}
            WHERE ${SYKEFRAVAER_OPPFOLGING.NORSK_IDENT} IN (:norskeIdenter)
        """

        return namedDb.query(sql, mapOf("norskeIdenter" to norskeIdenter)) { rs, rowNum ->
            rs.getString(SYKEFRAVAER_OPPFOLGING.NORSK_IDENT) to sykefravaerRowMapper.mapRow(rs, rowNum)
        }.toMap()
    }

    @TestOnly
    fun hentSykefravaerPeriode(norskIdent: NorskIdent): SykefravaerEntity? {
        val sql = "SELECT * FROM ${SYKEFRAVAER_OPPFOLGING.TABLE_NAME} WHERE ${SYKEFRAVAER_OPPFOLGING.NORSK_IDENT} = ?"
        return try {
            db.queryForObject(sql, sykefravaerRowMapper, norskIdent)
        } catch (ex: EmptyResultDataAccessException) {
            null
        }
    }

    private val sykefravaerRowMapper = RowMapper { rs, _ ->
        SykefravaerEntity(
            start = rs.getDate(SYKEFRAVAER_OPPFOLGING.START).toLocalDate(),
            slutt = rs.getDate(SYKEFRAVAER_OPPFOLGING.SLUTT).toLocalDate(),
            harArbeidsgiver = rs.getBoolean(SYKEFRAVAER_OPPFOLGING.HAR_ARBEIDSGIVER),
            radSistEndret = rs.getTimestamp(SYKEFRAVAER_OPPFOLGING.RAD_SIST_ENDRET).toLocalDateTime()
        )
    }

    fun slettSykefravaerForBruker(norskIdent: NorskIdent) {
        db.update(
            "DELETE FROM ${SYKEFRAVAER_OPPFOLGING.TABLE_NAME} WHERE ${SYKEFRAVAER_OPPFOLGING.NORSK_IDENT} = ?",
            norskIdent
        )
    }
}
