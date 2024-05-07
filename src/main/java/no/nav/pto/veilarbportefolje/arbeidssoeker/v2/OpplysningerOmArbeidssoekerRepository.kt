package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.pto.veilarbportefolje.database.PostgresTable.OPPLYSNINGER_OM_ARBEIDSSOEKER
import no.nav.pto.veilarbportefolje.database.PostgresTable.OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON
import no.nav.pto.veilarbportefolje.util.DateUtils
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Types
import java.util.*

@Repository
class OpplysningerOmArbeidssoekerRepository(
    private val db: JdbcTemplate
) {
    fun insertOpplysningerOmArbeidssoekerOgJobbsituasjon(opplysningerOmArbeidssoekerEntity: OpplysningerOmArbeidssoekerEntity) {
        insertOpplysningerOmArbeidssoeker(opplysningerOmArbeidssoekerEntity)
        insertOpplysningerOmArbeidssoekerJobbsituasjon(opplysningerOmArbeidssoekerEntity.opplysningerOmJobbsituasjon)
    }

    private fun insertOpplysningerOmArbeidssoeker(opplysningerOmArbeidssoekerEntity: OpplysningerOmArbeidssoekerEntity) {
        val sqlString = """INSERT INTO ${OPPLYSNINGER_OM_ARBEIDSSOEKER.TABLE_NAME} ( 
                    ${OPPLYSNINGER_OM_ARBEIDSSOEKER.OPPLYSNINGER_OM_ARBEIDSSOEKER_ID}, 
                    ${OPPLYSNINGER_OM_ARBEIDSSOEKER.PERIODE_ID},
                    ${OPPLYSNINGER_OM_ARBEIDSSOEKER.SENDT_INN_TIDSPUNKT},
                    ${OPPLYSNINGER_OM_ARBEIDSSOEKER.UTDANNING_NUS_KODE},
                    ${OPPLYSNINGER_OM_ARBEIDSSOEKER.UTDANNING_BESTATT},
                    ${OPPLYSNINGER_OM_ARBEIDSSOEKER.UTDANNING_GODKJENT}
                )
                VALUES (?, ?, ?, ?, ?, ?)"""
        db.update(
            sqlString,
            opplysningerOmArbeidssoekerEntity.opplysningerOmArbeidssoekerId,
            opplysningerOmArbeidssoekerEntity.periodeId,
            opplysningerOmArbeidssoekerEntity.sendtInnTidspunkt,
            opplysningerOmArbeidssoekerEntity.utdanningNusKode,
            opplysningerOmArbeidssoekerEntity.utdanningBestatt,
            opplysningerOmArbeidssoekerEntity.utdanningGodkjent
        )
    }

    private fun insertOpplysningerOmArbeidssoekerJobbsituasjon(opplysningerOmArbeidssoekerJobbsituasjonEntity: OpplysningerOmArbeidssoekerJobbsituasjonEntity) {
        val sqlString = """INSERT INTO ${OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON.TABLE_NAME} ( 
                    ${OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON.OPPLYSNINGER_OM_ARBEIDSSOEKER_ID}, 
                    ${OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON.JOBBSITUASJON}
                )
                VALUES (?, ?)"""

        db.batchUpdate(sqlString, object : BatchPreparedStatementSetter {
            @Throws(SQLException::class)
            override fun setValues(ps: PreparedStatement, i: Int) {
                ps.setObject(1, opplysningerOmArbeidssoekerJobbsituasjonEntity.opplysningerOmArbeidssoekerId, Types.OTHER)
                ps.setString(2, opplysningerOmArbeidssoekerJobbsituasjonEntity.jobbsituasjon[i])
            }

            override fun getBatchSize(): Int {
                return opplysningerOmArbeidssoekerJobbsituasjonEntity.jobbsituasjon.size
            }
        })
    }
}