package no.nav.pto.veilarbportefolje.sykmeldt

import no.nav.poao_tilgang.client.NorskIdent
import no.nav.pto.veilarbportefolje.database.PostgresTable.SYKMELDT
import no.nav.pto.veilarbportefolje.sykmeldt.domene.SykmeldtEntity
import no.nav.pto.veilarbportefolje.sykmeldt.dto.OppfolgingstilfelleDTO
import org.jetbrains.annotations.TestOnly
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class SykmeldtRepository(
    private val db: JdbcTemplate,
    private val namedDb: NamedParameterJdbcTemplate
) {

    fun upsertSykmeldtperiode(norskIdent: NorskIdent, oppfolgingstilfelle: OppfolgingstilfelleDTO) {
        db.update(
            """
            INSERT INTO ${SYKMELDT.TABLE_NAME} (
                ${SYKMELDT.NORSK_IDENT},
                ${SYKMELDT.START},
                ${SYKMELDT.SLUTT},
                ${SYKMELDT.HAR_ARBEIDSGIVER},
                ${SYKMELDT.RAD_SIST_ENDRET}
            ) VALUES (?,?,?,?,current_timestamp)
            ON CONFLICT (${SYKMELDT.NORSK_IDENT})
            DO UPDATE SET (
                ${SYKMELDT.START},
                ${SYKMELDT.SLUTT},
                ${SYKMELDT.HAR_ARBEIDSGIVER},
                ${SYKMELDT.RAD_SIST_ENDRET}
            ) = (
                excluded.${SYKMELDT.START},
                excluded.${SYKMELDT.SLUTT},
                excluded.${SYKMELDT.HAR_ARBEIDSGIVER},
                excluded.${SYKMELDT.RAD_SIST_ENDRET}
            ) """,
            norskIdent,
            oppfolgingstilfelle.start,
            oppfolgingstilfelle.end,
            oppfolgingstilfelle.arbeidstakerAtTilfelleEnd
        )
    }

    fun hentSykmeldtPerioder(norskeIdenter: List<String>): Map<String, SykmeldtEntity> {
        if (norskeIdenter.isEmpty()) return emptyMap()

        val sql = """
            SELECT
            ${SYKMELDT.NORSK_IDENT},
            ${SYKMELDT.START},
            ${SYKMELDT.SLUTT},
            ${SYKMELDT.HAR_ARBEIDSGIVER},
            ${SYKMELDT.RAD_SIST_ENDRET}
            FROM ${SYKMELDT.TABLE_NAME}
            WHERE ${SYKMELDT.NORSK_IDENT} IN (:norskeIdenter)
        """

        return namedDb.query(sql, mapOf("norskeIdenter" to norskeIdenter)) { rs, rowNum ->
            rs.getString(SYKMELDT.NORSK_IDENT) to sykmeldtRowMapper.mapRow(rs, rowNum)
        }.toMap()
    }

    @TestOnly
    fun hentSykmeldtPeriode(norskIdent: NorskIdent): SykmeldtEntity? {
        val sql = "SELECT * FROM ${SYKMELDT.TABLE_NAME} WHERE ${SYKMELDT.NORSK_IDENT} = ?"
        return try {
            db.queryForObject(sql, sykmeldtRowMapper, norskIdent)
        } catch (ex: EmptyResultDataAccessException) {
            null
        }
    }

    private val sykmeldtRowMapper = RowMapper { rs, _ ->
        SykmeldtEntity(
            start = rs.getDate(SYKMELDT.START).toLocalDate(),
            slutt = rs.getDate(SYKMELDT.SLUTT).toLocalDate(),
            harArbeidsgiver = rs.getBoolean(SYKMELDT.HAR_ARBEIDSGIVER),
            radSistEndret = rs.getTimestamp(SYKMELDT.RAD_SIST_ENDRET).toLocalDateTime()
        )
    }

    fun slettSykmeldtForBruker(norskIdent: NorskIdent) {
        db.update(
            "DELETE FROM ${SYKMELDT.TABLE_NAME} WHERE ${SYKMELDT.NORSK_IDENT} = ?",
            norskIdent
        )
    }
}
