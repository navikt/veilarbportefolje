package no.nav.pto.veilarbportefolje.aap

import no.nav.poao_tilgang.client.NorskIdent
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakPeriodeEntity
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakStatus
import no.nav.pto.veilarbportefolje.aap.dto.AapVedtakResponseDto
import no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSER_AAP
import org.jetbrains.annotations.TestOnly
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class AapRepository(@param:Autowired private val db: JdbcTemplate) {

    fun upsertAap(
        norskIdent: NorskIdent,
        aap: AapVedtakResponseDto.Vedtak,
        maksdato: LocalDate?,
        sakstatus: String
    ) {
        db.update(
            """
            INSERT INTO ${YTELSER_AAP.TABLE_NAME} (
                ${YTELSER_AAP.NORSK_IDENT},
                ${YTELSER_AAP.STATUS}, 
                ${YTELSER_AAP.SAKSID}, 
                ${YTELSER_AAP.NYESTE_PERIODE_FOM}, 
                ${YTELSER_AAP.NYESTE_PERIODE_TOM}, 
                ${YTELSER_AAP.RETTIGHETSTYPE}, 
                ${YTELSER_AAP.MAKSDATO}, 
                ${YTELSER_AAP.SAKSTATUS}, 
                ${YTELSER_AAP.RAD_SIST_ENDRET}
            ) VALUES (?,?,?,?,?,?,?,?,current_timestamp) 
            ON CONFLICT (${YTELSER_AAP.NORSK_IDENT}) 
            DO UPDATE SET (
                ${YTELSER_AAP.STATUS}, 
                ${YTELSER_AAP.SAKSID}, 
                ${YTELSER_AAP.NYESTE_PERIODE_FOM}, 
                ${YTELSER_AAP.NYESTE_PERIODE_TOM}, 
                ${YTELSER_AAP.RETTIGHETSTYPE}, 
                ${YTELSER_AAP.MAKSDATO}, 
                ${YTELSER_AAP.SAKSTATUS}, 
                ${YTELSER_AAP.RAD_SIST_ENDRET}
            ) = (
                excluded.${YTELSER_AAP.STATUS}, 
                excluded.${YTELSER_AAP.SAKSID}, 
                excluded.${YTELSER_AAP.NYESTE_PERIODE_FOM}, 
                excluded.${YTELSER_AAP.NYESTE_PERIODE_TOM}, 
                excluded.${YTELSER_AAP.RETTIGHETSTYPE}, 
                excluded.${YTELSER_AAP.MAKSDATO}, 
                excluded.${YTELSER_AAP.SAKSTATUS}, 
                excluded.${YTELSER_AAP.RAD_SIST_ENDRET}
            ) """,
            norskIdent,
            aap.status.toString(),
            aap.saksnummer,
            aap.periode.fraOgMedDato,
            aap.periode.tilOgMedDato,
            aap.rettighetsType.toString(),
            maksdato,
            sakstatus
        )
    }

    @TestOnly
    fun hentAap(norskIdent: NorskIdent): AapVedtakPeriodeEntity? {
        val sql = "SELECT * FROM ${YTELSER_AAP.TABLE_NAME} WHERE ${YTELSER_AAP.NORSK_IDENT} = ?"
        return try {
            db.queryForObject(sql, { rs, _ ->
                AapVedtakPeriodeEntity(
                    status = AapVedtakStatus.fraDb(rs.getString(YTELSER_AAP.STATUS)),
                    periodeFom = rs.getDate(YTELSER_AAP.NYESTE_PERIODE_FOM).toLocalDate(),
                    periodeTom = rs.getDate(YTELSER_AAP.NYESTE_PERIODE_TOM).toLocalDate(),
                    maksdato = rs.getDate(YTELSER_AAP.MAKSDATO).toLocalDate(),
                    sakstatus = rs.getString(YTELSER_AAP.SAKSTATUS)
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


