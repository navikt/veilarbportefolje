package no.nav.pto.veilarbportefolje.ungdomsprogram

import no.nav.poao_tilgang.client.NorskIdent
import no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSER_UNGDOMSPROGRAM
import no.nav.pto.veilarbportefolje.ungdomsprogram.domene.UngdomsprogramPeriodeEntity
import no.nav.pto.veilarbportefolje.ungdomsprogram.dto.Periode
import org.jetbrains.annotations.TestOnly
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class UngdomsprogramRepository(private val db: JdbcTemplate) {
    fun upsertUngdomsprogram(
        norskIdent: NorskIdent,
        ungdomsprogramperiode: Periode,
    ) {
        db.update(
            """
            INSERT INTO ${YTELSER_UNGDOMSPROGRAM.TABLE_NAME} (
                ${YTELSER_UNGDOMSPROGRAM.NORSK_IDENT},
                ${YTELSER_UNGDOMSPROGRAM.NYESTE_PERIODE_FOM}, 
                ${YTELSER_UNGDOMSPROGRAM.NYESTE_PERIODE_TOM}, 
                ${YTELSER_UNGDOMSPROGRAM.HAR_FORLENGET_PERIODE}, 
                ${YTELSER_UNGDOMSPROGRAM.MAKSDATO}, 
                ${YTELSER_UNGDOMSPROGRAM.RAD_SIST_ENDRET}
            ) VALUES (?,?,?,?,?,current_timestamp) 
            ON CONFLICT (${YTELSER_UNGDOMSPROGRAM.NORSK_IDENT}) 
            DO UPDATE SET (
                ${YTELSER_UNGDOMSPROGRAM.NYESTE_PERIODE_FOM}, 
                ${YTELSER_UNGDOMSPROGRAM.NYESTE_PERIODE_TOM}, 
                ${YTELSER_UNGDOMSPROGRAM.HAR_FORLENGET_PERIODE}, 
                ${YTELSER_UNGDOMSPROGRAM.MAKSDATO}, 
                ${YTELSER_UNGDOMSPROGRAM.RAD_SIST_ENDRET}
            ) = (
                excluded.${YTELSER_UNGDOMSPROGRAM.NYESTE_PERIODE_FOM}, 
                excluded.${YTELSER_UNGDOMSPROGRAM.NYESTE_PERIODE_TOM}, 
                excluded.${YTELSER_UNGDOMSPROGRAM.HAR_FORLENGET_PERIODE}, 
                excluded.${YTELSER_UNGDOMSPROGRAM.MAKSDATO}, 
                excluded.${YTELSER_UNGDOMSPROGRAM.RAD_SIST_ENDRET}
            ) """,
            norskIdent,
            ungdomsprogramperiode.fraOgMed,
            ungdomsprogramperiode.tilOgMed,
            ungdomsprogramperiode.harForlengetPeriode,
            ungdomsprogramperiode.periodeMaksDato,
        )
    }

    @TestOnly
    fun hentUngdomsprogram(norskIdent: NorskIdent): UngdomsprogramPeriodeEntity? {
        val sql = "SELECT * FROM ${YTELSER_UNGDOMSPROGRAM.TABLE_NAME} WHERE ${YTELSER_UNGDOMSPROGRAM.NORSK_IDENT} = ?"
        return try {
            db.queryForObject(sql, { rs, _ ->
                UngdomsprogramPeriodeEntity(
                    fraOgMed = rs.getDate(YTELSER_UNGDOMSPROGRAM.NYESTE_PERIODE_FOM).toLocalDate(),
                    tilOgMed = rs.getDate(YTELSER_UNGDOMSPROGRAM.NYESTE_PERIODE_TOM)?.toLocalDate(),
                    harForlengetPeriode = rs.getBoolean(YTELSER_UNGDOMSPROGRAM.HAR_FORLENGET_PERIODE),
                    maksdato = rs.getDate(YTELSER_UNGDOMSPROGRAM.MAKSDATO).toLocalDate()
                )
            }, norskIdent)
        } catch (ex: EmptyResultDataAccessException) {
            null
        }
    }

    fun slettUngdomsprogramForBruker(norskIdent: NorskIdent) {
        db.update(
            "DELETE FROM ${YTELSER_UNGDOMSPROGRAM.TABLE_NAME} WHERE ${YTELSER_UNGDOMSPROGRAM.NORSK_IDENT} = ?",
            norskIdent
        )
    }

}
