package no.nav.pto.veilarbportefolje.tiltakspenger


import no.nav.poao_tilgang.client.NorskIdent
import no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSER_TILTAKSPENGER
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerResponseDto
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerVedtak
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate

class TiltakspengerRespository(@Autowired private val db: JdbcTemplate) {

    fun upsertAap(norskIdent: NorskIdent, tiltakspenger: TiltakspengerResponseDto) {
        db.update(
            """
            INSERT INTO ${YTELSER_TILTAKSPENGER.TABLE_NAME} (
                ${YTELSER_TILTAKSPENGER.NORSK_IDENT},
                ${YTELSER_TILTAKSPENGER.SAKSID}, 
                ${YTELSER_TILTAKSPENGER.NYESTE_PERIODE_FOM}, 
                ${YTELSER_TILTAKSPENGER.NYESTE_PERIODE_TOM}, 
                ${YTELSER_TILTAKSPENGER.RETTIGHETSTYPE}, 
                ${YTELSER_TILTAKSPENGER.RAD_SIST_ENDRET}
            ) VALUES (?,?,?,?,?,current_timestamp) 
            ON CONFLICT (${YTELSER_TILTAKSPENGER.NORSK_IDENT}) 
            DO UPDATE SET (
                ${YTELSER_TILTAKSPENGER.SAKSID}, 
                ${YTELSER_TILTAKSPENGER.NYESTE_PERIODE_FOM}, 
                ${YTELSER_TILTAKSPENGER.NYESTE_PERIODE_TOM}, 
                ${YTELSER_TILTAKSPENGER.RETTIGHETSTYPE}, 
                ${YTELSER_TILTAKSPENGER.RAD_SIST_ENDRET}
            ) = (
                excluded.${YTELSER_TILTAKSPENGER.SAKSID}, 
                excluded.${YTELSER_TILTAKSPENGER.NYESTE_PERIODE_FOM}, 
                excluded.${YTELSER_TILTAKSPENGER.NYESTE_PERIODE_TOM}, 
                excluded.${YTELSER_TILTAKSPENGER.RETTIGHETSTYPE}, 
                excluded.${YTELSER_TILTAKSPENGER.RAD_SIST_ENDRET}
            ) """,
            norskIdent,
            tiltakspenger.sakId,
            tiltakspenger.fom,
            tiltakspenger.tom,
            tiltakspenger.rettighet.toString()
        )
    }

    fun hentTiltakspenger(norskIdent: NorskIdent): TiltakspengerVedtak? {
        val sql = "SELECT * FROM ${YTELSER_TILTAKSPENGER.TABLE_NAME} WHERE ${YTELSER_TILTAKSPENGER.NORSK_IDENT} = ?"
        return try {
            db.queryForObject(sql, { rs, _ ->
                TiltakspengerVedtak(
                    sakId = rs.getString(YTELSER_TILTAKSPENGER.SAKSID),
                    fom = rs.getDate(YTELSER_TILTAKSPENGER.NYESTE_PERIODE_FOM).toLocalDate(),
                    tom = rs.getDate(YTELSER_TILTAKSPENGER.NYESTE_PERIODE_TOM).toLocalDate(),
                    rettighet = TiltakspengerRettighet.fraDb(rs.getString(YTELSER_TILTAKSPENGER.RETTIGHETSTYPE)),
                )
            }, norskIdent)
        } catch (ex: EmptyResultDataAccessException) {
            null
        }
    }

    fun slettTiltakspengerForBruker(norskIdent: NorskIdent) {
        db.update(
            "DELETE FROM ${YTELSER_TILTAKSPENGER.TABLE_NAME} WHERE ${YTELSER_TILTAKSPENGER.NORSK_IDENT} = ?",
            norskIdent
        )
    }
}
