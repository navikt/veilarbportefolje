package no.nav.pto.veilarbportefolje.dagpenger

import no.nav.poao_tilgang.client.NorskIdent
import no.nav.pto.veilarbportefolje.dagpenger.domene.DagpengerEntity
import no.nav.pto.veilarbportefolje.dagpenger.domene.DagpengerRettighetstype
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerBeregningerResponseDto
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerPeriodeDto
import no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSER_DAGPENGER
import org.jetbrains.annotations.TestOnly
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository


@Repository
class DagpengerRepository(@param:Autowired private val db: JdbcTemplate) {

    fun upsertDagpengerPerioder(
        norskIdent: NorskIdent,
        dagpengerPeriode: DagpengerPeriodeDto,
        beregning: DagpengerBeregningerResponseDto?
    ) {
        db.update(
            """
            INSERT INTO ${YTELSER_DAGPENGER.TABLE_NAME} (
                ${YTELSER_DAGPENGER.NORSK_IDENT},
                ${YTELSER_DAGPENGER.NYESTE_PERIODE_FOM}, 
                ${YTELSER_DAGPENGER.NYESTE_PERIODE_TOM}, 
                ${YTELSER_DAGPENGER.RETTIGHETSTYPE}, 
                ${YTELSER_DAGPENGER.DATO_ANTALL_DAGER_BLE_BEREGNET}, 
                ${YTELSER_DAGPENGER.ANTALL_RESTERENDE_DAGER}, 
                ${YTELSER_DAGPENGER.RAD_SIST_ENDRET}
            ) VALUES (?,?,?,?,?,?,current_timestamp) 
            ON CONFLICT (${YTELSER_DAGPENGER.NORSK_IDENT}) 
            DO UPDATE SET (
                ${YTELSER_DAGPENGER.NYESTE_PERIODE_FOM}, 
                ${YTELSER_DAGPENGER.NYESTE_PERIODE_TOM}, 
                ${YTELSER_DAGPENGER.RETTIGHETSTYPE}, 
                ${YTELSER_DAGPENGER.DATO_ANTALL_DAGER_BLE_BEREGNET}, 
                ${YTELSER_DAGPENGER.ANTALL_RESTERENDE_DAGER}, 
                ${YTELSER_DAGPENGER.RAD_SIST_ENDRET}
            ) = (
                excluded.${YTELSER_DAGPENGER.NYESTE_PERIODE_FOM}, 
                excluded.${YTELSER_DAGPENGER.NYESTE_PERIODE_TOM}, 
                excluded.${YTELSER_DAGPENGER.RETTIGHETSTYPE}, 
                excluded.${YTELSER_DAGPENGER.DATO_ANTALL_DAGER_BLE_BEREGNET}, 
                excluded.${YTELSER_DAGPENGER.ANTALL_RESTERENDE_DAGER}, 
                excluded.${YTELSER_DAGPENGER.RAD_SIST_ENDRET}
            ) """,
            norskIdent,
            dagpengerPeriode.fraOgMedDato,
            dagpengerPeriode.tilOgMedDato,
            dagpengerPeriode.ytelseType.toString(),
            beregning?.dato,
            beregning?.gjenståendeDager,
        )
    }

    @TestOnly
    fun hentDagpenger(norskIdent: NorskIdent): DagpengerEntity? {
        val sql = "SELECT * FROM ${YTELSER_DAGPENGER.TABLE_NAME} WHERE ${YTELSER_DAGPENGER.NORSK_IDENT} = ?"
        return try {
            db.queryForObject(sql, { rs, _ ->
                DagpengerEntity(
                    fom = rs.getDate(YTELSER_DAGPENGER.NYESTE_PERIODE_FOM).toLocalDate(),
                    tom = rs.getDate(YTELSER_DAGPENGER.NYESTE_PERIODE_TOM)?.toLocalDate(),
                    rettighetstype = DagpengerRettighetstype.fraDb(rs.getString(YTELSER_DAGPENGER.RETTIGHETSTYPE)),
                    antallDagerResterende = rs.getObject(YTELSER_DAGPENGER.ANTALL_RESTERENDE_DAGER) as Int?,
                    datoAntallDagerBleBeregnet = rs.getDate(YTELSER_DAGPENGER.DATO_ANTALL_DAGER_BLE_BEREGNET)
                        ?.toLocalDate()
                )
            }, norskIdent)
        } catch (ex: EmptyResultDataAccessException) {
            null
        }
    }

    fun slettDagpengerForBruker(norskIdent: NorskIdent) {
        db.update(
            "DELETE FROM ${YTELSER_DAGPENGER.TABLE_NAME} WHERE ${YTELSER_DAGPENGER.NORSK_IDENT} = ?",
            norskIdent
        )
    }
}
