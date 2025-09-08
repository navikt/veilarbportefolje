package no.nav.pto.veilarbportefolje.aap.repository

import no.nav.poao_tilgang.client.NorskIdent
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakResponseDto
import no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSER_AAP
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class AapRepository(@Autowired private val db: JdbcTemplate) {

    fun upsertAap(norskIdent: NorskIdent, aap: AapVedtakResponseDto.Vedtak) {
        db.update(
            """
            INSERT INTO ${YTELSER_AAP.TABLE_NAME} (
                ${YTELSER_AAP.NORSK_IDENT},
                ${YTELSER_AAP.STATUS}, 
                ${YTELSER_AAP.SAKSID}, 
                ${YTELSER_AAP.NYESTE_PERIODE_FOM}, 
                ${YTELSER_AAP.NYESTE_PERIODE_TOM}, 
                ${YTELSER_AAP.RETTIGHETSTYPE}, 
                ${YTELSER_AAP.OPPHORSAARSAK}, 
                ${YTELSER_AAP.RAD_SIST_ENDRET}
            ) VALUES (?,?,?,?,?,?,?,current_timestamp) 
            ON CONFLICT (${YTELSER_AAP.NORSK_IDENT}) 
            DO UPDATE SET (
                ${YTELSER_AAP.STATUS}, 
                ${YTELSER_AAP.SAKSID}, 
                ${YTELSER_AAP.NYESTE_PERIODE_FOM}, 
                ${YTELSER_AAP.NYESTE_PERIODE_TOM}, 
                ${YTELSER_AAP.RETTIGHETSTYPE}, 
                ${YTELSER_AAP.OPPHORSAARSAK}, 
                ${YTELSER_AAP.RAD_SIST_ENDRET}
            ) = (
                excluded.${YTELSER_AAP.STATUS}, 
                excluded.${YTELSER_AAP.SAKSID}, 
                excluded.${YTELSER_AAP.NYESTE_PERIODE_FOM}, 
                excluded.${YTELSER_AAP.NYESTE_PERIODE_TOM}, 
                excluded.${YTELSER_AAP.RETTIGHETSTYPE}, 
                excluded.${YTELSER_AAP.OPPHORSAARSAK}, 
                excluded.${YTELSER_AAP.RAD_SIST_ENDRET}
            ) """,
            norskIdent,
            aap.status,
            aap.saksnummer,
            aap.periode.fraOgMedDato,
            aap.periode.tilOgMedDato,
            aap.rettighetsType,
            aap.opphorsAarsak
        )
    }

    fun hentAap(norskIdent: NorskIdent): AapVedtakPeriode? {
        val sql = "SELECT * FROM ${YTELSER_AAP.TABLE_NAME} WHERE ${YTELSER_AAP.NORSK_IDENT} = ?"
        return try {
            db.queryForObject(sql, { rs, _ ->
                AapVedtakPeriode(
                    status = rs.getString("STATUS"),
                    saksid = rs.getString("SAKSID"),
                    periodeFom = rs.getDate("NYESTE_PERIODE_FOM").toLocalDate(),
                    periodeTom = rs.getDate("NYESTE_PERIODE_TOM").toLocalDate()
                )
            }, norskIdent)
        } catch (ex: EmptyResultDataAccessException) {
           null
        }
    }

    fun slettAapForBruker(norskIdent: NorskIdent) {
        db.update("DELETE FROM ${YTELSER_AAP.TABLE_NAME} WHERE ${YTELSER_AAP.NORSK_IDENT} = ?", norskIdent)
    }
}


