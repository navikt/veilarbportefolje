package no.nav.pto.veilarbportefolje.uforetrygd

import no.nav.poao_tilgang.client.NorskIdent
import no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSER_UFORETRYGD
import no.nav.pto.veilarbportefolje.uforetrygd.domene.UforetrygdEntity
import no.nav.pto.veilarbportefolje.uforetrygd.dto.UforetrygdResponseDto
import org.jetbrains.annotations.TestOnly
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class UforetrygdRepository(private val db: JdbcTemplate) {

    fun upsertUforetrygd(norskIdent: String, uføretrygd: UforetrygdResponseDto) {
        db.update(
            """
            INSERT INTO ${YTELSER_UFORETRYGD.TABLE_NAME} (
                ${YTELSER_UFORETRYGD.NORSK_IDENT},
                ${YTELSER_UFORETRYGD.UFOREGRAD}, 
                ${YTELSER_UFORETRYGD.VIRKNINGSDATO}, 
                ${YTELSER_UFORETRYGD.RAD_SIST_ENDRET}
            ) VALUES (?,?,?,current_timestamp) 
            ON CONFLICT (${YTELSER_UFORETRYGD.NORSK_IDENT}) 
            DO UPDATE SET (
                ${YTELSER_UFORETRYGD.UFOREGRAD}, 
                ${YTELSER_UFORETRYGD.VIRKNINGSDATO}, 
                ${YTELSER_UFORETRYGD.RAD_SIST_ENDRET}
            ) = (
                excluded.${YTELSER_UFORETRYGD.UFOREGRAD}, 
                excluded.${YTELSER_UFORETRYGD.VIRKNINGSDATO}, 
                excluded.${YTELSER_UFORETRYGD.RAD_SIST_ENDRET}
            ) """,
            norskIdent,
            uføretrygd.uføregrad,
            uføretrygd.virkningsdato
        )
    }


    @TestOnly
    fun hentUforetrygd(norskIdent: NorskIdent): UforetrygdEntity? {
        val sql = "SELECT * FROM ${YTELSER_UFORETRYGD.TABLE_NAME} WHERE ${YTELSER_UFORETRYGD.NORSK_IDENT} = ?"
        return try {
            db.queryForObject(sql, { rs, _ ->
                UforetrygdEntity(
                    uføregrad = rs.getInt(YTELSER_UFORETRYGD.UFOREGRAD),
                    virkningsdato = rs.getDate(YTELSER_UFORETRYGD.VIRKNINGSDATO).toLocalDate(),
                )
            }, norskIdent)
        } catch (ex: EmptyResultDataAccessException) {
            null
        }
    }


    fun slettUforetrygdForBruker(norskIdent: NorskIdent) {
        db.update(
            "DELETE FROM ${YTELSER_UFORETRYGD.TABLE_NAME} WHERE ${YTELSER_UFORETRYGD.NORSK_IDENT} = ?",
            norskIdent
        )
    }

}
