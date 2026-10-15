package no.nav.pto.veilarbportefolje.tiltakspenger


import no.nav.poao_tilgang.client.NorskIdent
import no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSER_TILTAKSPENGER
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerVedtakEntity
import no.nav.pto.veilarbportefolje.tiltakspenger.dto.TiltakspengerResponseDto
import org.jetbrains.annotations.TestOnly
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class TiltakspengerRespository(
    private val db: JdbcTemplate,
    private val namedDb: NamedParameterJdbcTemplate
) {

    fun upsertTiltakspenger(norskIdent: NorskIdent, tiltakspenger: TiltakspengerResponseDto) {
        db.update(
            """
            INSERT INTO ${YTELSER_TILTAKSPENGER.TABLE_NAME} (
                ${YTELSER_TILTAKSPENGER.NORSK_IDENT},
                ${YTELSER_TILTAKSPENGER.SAKSID}, 
                ${YTELSER_TILTAKSPENGER.NYESTE_PERIODE_FOM}, 
                ${YTELSER_TILTAKSPENGER.NYESTE_PERIODE_TOM}, 
                ${YTELSER_TILTAKSPENGER.RETTIGHET}, 
                ${YTELSER_TILTAKSPENGER.RAD_SIST_ENDRET}
            ) VALUES (?,?,?,?,?,current_timestamp) 
            ON CONFLICT (${YTELSER_TILTAKSPENGER.NORSK_IDENT}) 
            DO UPDATE SET (
                ${YTELSER_TILTAKSPENGER.SAKSID}, 
                ${YTELSER_TILTAKSPENGER.NYESTE_PERIODE_FOM}, 
                ${YTELSER_TILTAKSPENGER.NYESTE_PERIODE_TOM}, 
                ${YTELSER_TILTAKSPENGER.RETTIGHET}, 
                ${YTELSER_TILTAKSPENGER.RAD_SIST_ENDRET}
            ) = (
                excluded.${YTELSER_TILTAKSPENGER.SAKSID}, 
                excluded.${YTELSER_TILTAKSPENGER.NYESTE_PERIODE_FOM}, 
                excluded.${YTELSER_TILTAKSPENGER.NYESTE_PERIODE_TOM}, 
                excluded.${YTELSER_TILTAKSPENGER.RETTIGHET}, 
                excluded.${YTELSER_TILTAKSPENGER.RAD_SIST_ENDRET}
            ) """,
            norskIdent,
            tiltakspenger.sakId,
            tiltakspenger.fom,
            tiltakspenger.tom,
            tiltakspenger.rettighet.toString()
        )
    }

    fun hentTiltakspengerVedtak(norskeIdenter: List<String>): Map<String, TiltakspengerVedtakEntity> {
        if (norskeIdenter.isEmpty()) return emptyMap()

        val sql = """
            SELECT 
            ${YTELSER_TILTAKSPENGER.NORSK_IDENT},
            ${YTELSER_TILTAKSPENGER.SAKSID}, 
            ${YTELSER_TILTAKSPENGER.NYESTE_PERIODE_FOM}, 
            ${YTELSER_TILTAKSPENGER.NYESTE_PERIODE_TOM}, 
            ${YTELSER_TILTAKSPENGER.RETTIGHET}, 
            ${YTELSER_TILTAKSPENGER.RAD_SIST_ENDRET}
            FROM ${YTELSER_TILTAKSPENGER.TABLE_NAME}
            WHERE ${YTELSER_TILTAKSPENGER.NORSK_IDENT} IN (:norskeIdenter)
        """

        return namedDb.query(sql, mapOf("norskeIdenter" to norskeIdenter)) { rs, rowNum ->
            rs.getString(YTELSER_TILTAKSPENGER.NORSK_IDENT) to tiltakspengerVedtakRowMapper.mapRow(rs, rowNum)
        }.toMap()
    }

    @TestOnly
    fun hentTiltakspenger(norskIdent: NorskIdent): TiltakspengerVedtakEntity? {
        val sql = "SELECT * FROM ${YTELSER_TILTAKSPENGER.TABLE_NAME} WHERE ${YTELSER_TILTAKSPENGER.NORSK_IDENT} = ?"
        return try {
            db.queryForObject(sql, tiltakspengerVedtakRowMapper, norskIdent)
        } catch (ex: EmptyResultDataAccessException) {
            null
        }
    }

    private val tiltakspengerVedtakRowMapper = RowMapper { rs, _ ->
        TiltakspengerVedtakEntity(
            fom = rs.getDate(YTELSER_TILTAKSPENGER.NYESTE_PERIODE_FOM).toLocalDate(),
            tom = rs.getDate(YTELSER_TILTAKSPENGER.NYESTE_PERIODE_TOM).toLocalDate(),
            rettighet = TiltakspengerRettighet.fraDb(rs.getString(YTELSER_TILTAKSPENGER.RETTIGHET)),
            radSistEndret = rs.getTimestamp(YTELSER_TILTAKSPENGER.RAD_SIST_ENDRET).toLocalDateTime()
        )
    }

    fun slettTiltakspengerForBruker(norskIdent: NorskIdent) {
        db.update(
            "DELETE FROM ${YTELSER_TILTAKSPENGER.TABLE_NAME} WHERE ${YTELSER_TILTAKSPENGER.NORSK_IDENT} = ?",
            norskIdent
        )
    }
}
