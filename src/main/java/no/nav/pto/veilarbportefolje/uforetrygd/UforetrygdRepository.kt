package no.nav.pto.veilarbportefolje.uforetrygd

import no.nav.poao_tilgang.client.NorskIdent
import no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSER_UFORETRYGD
import no.nav.pto.veilarbportefolje.uforetrygd.dto.UforetrygdResponseDto
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


    fun slettUforetrygdForBruker(norskIdent: NorskIdent) {
        db.update(
            "DELETE FROM ${YTELSER_UFORETRYGD.TABLE_NAME} WHERE ${YTELSER_UFORETRYGD.NORSK_IDENT} = ?",
            norskIdent
        )
    }

}
