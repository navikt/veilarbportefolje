package no.nav.pto.veilarbportefolje.aktiviteter.v1

import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.PreparedStatement
import java.sql.Statement

@Repository
class PortefoljeAktivitetKafkaMeldingRepository(
    private val db: JdbcTemplate,
) {
    @Transactional
    fun tryLagreAktivitetDataBatch(aktiviteter: List<KafkaAktivitetMeldingEntity>): PortefoljeAktivitetBatchResult {
        if (aktiviteter.isEmpty()) {
            return PortefoljeAktivitetBatchResult(0, 0, 0, 0)
        }

        val endeligeAktiviteter = beholdSisteMeldingPerAktivitet(aktiviteter)
        val slettinger = endeligeAktiviteter.filter { it.value.historisk }
        val upserts = endeligeAktiviteter.filterNot { it.value.historisk }

        val prosesserteSlettinger = batchDeleteById(slettinger).sumOf(::normaliserUpdateCount)
        val prosesserteUpserts = batchUpsertAktivitet(upserts).sumOf(::normaliserUpdateCount)
        val prosesserte = prosesserteSlettinger + prosesserteUpserts

        return PortefoljeAktivitetBatchResult(
            mottatte = aktiviteter.size,
            dedupliserte = aktiviteter.size - endeligeAktiviteter.size,
            prosesserte = prosesserte,
            ignorert = endeligeAktiviteter.size - prosesserte,
        )
    }

    private fun batchUpsertAktivitet(aktiviteter: List<KafkaAktivitetMeldingEntity>): IntArray {
        if (aktiviteter.isEmpty()) {
            return intArrayOf()
        }

        val sql = """
            INSERT INTO KAFKA_AKTIVITET_MELDING (
                AKTIVITET_ID,
                AKTOR_ID,
                AKTIVITET_TYPE,
                AKTIVITET_STATUS,
                ENDRINGS_TYPE,
                FRA_DATO,
                TIL_DATO,
                ENDRET_DATO,
                TILTAKSKODE,
                LAGT_INN_AV,
                AVTALT,
                VERSION,
                HISTORISK,
                CV_KAN_DELES_STATUS,
                SVARFRIST_STILLING_FRA_NAV,
                RECORD_OFFSET,
                RECORD_PARTITION,
                RECORD_KEY,
                RAD_OPPRETTET,
                RAD_OPPDATERT
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (AKTIVITET_ID) DO UPDATE
            SET AKTOR_ID = excluded.AKTOR_ID,
                AKTIVITET_TYPE = excluded.AKTIVITET_TYPE,
                AKTIVITET_STATUS = excluded.AKTIVITET_STATUS,
                ENDRINGS_TYPE = excluded.ENDRINGS_TYPE,
                FRA_DATO = excluded.FRA_DATO,
                TIL_DATO = excluded.TIL_DATO,
                ENDRET_DATO = excluded.ENDRET_DATO,
                TILTAKSKODE = excluded.TILTAKSKODE,
                LAGT_INN_AV = excluded.LAGT_INN_AV,
                AVTALT = excluded.AVTALT,
                VERSION = excluded.VERSION,
                HISTORISK = excluded.HISTORISK,
                CV_KAN_DELES_STATUS = excluded.CV_KAN_DELES_STATUS,
                SVARFRIST_STILLING_FRA_NAV = excluded.SVARFRIST_STILLING_FRA_NAV,
                RECORD_OFFSET = excluded.RECORD_OFFSET,
                RECORD_PARTITION = excluded.RECORD_PARTITION,
                RECORD_KEY = excluded.RECORD_KEY,
                RAD_OPPDATERT = CURRENT_TIMESTAMP
            WHERE KAFKA_AKTIVITET_MELDING.VERSION <= excluded.VERSION
        """.trimIndent()

        return db.batchUpdate(sql, object : BatchPreparedStatementSetter {
            override fun setValues(ps: PreparedStatement, i: Int) {
                val aktivitet = aktiviteter[i]
                ps.setString(1, aktivitet.value.aktivitetId)
                ps.setString(2, aktivitet.value.aktorId)
                ps.setString(3, aktivitet.value.aktivitetType)
                ps.setString(4, aktivitet.value.aktivitetStatus)
                ps.setString(5, aktivitet.value.endringsType)
                ps.setTimestamp(6, aktivitet.value.fraDato)
                ps.setTimestamp(7, aktivitet.value.tilDato)
                ps.setTimestamp(8, aktivitet.value.endretDato)
                ps.setString(9, aktivitet.value.tiltakskode)
                ps.setString(10, aktivitet.value.lagtInnAv)
                ps.setBoolean(11, aktivitet.value.avtalt)
                ps.setLong(12, aktivitet.value.version)
                ps.setBoolean(13, aktivitet.value.historisk)
                ps.setString(14, aktivitet.value.cvKanDelesStatus)
                ps.setTimestamp(15, aktivitet.value.svarfristStillingFraNav)
                ps.setLong(16, aktivitet.metadata.recordOffset)
                ps.setInt(17, aktivitet.metadata.recordPartition)
                ps.setString(18, aktivitet.metadata.recordKey)
            }

            override fun getBatchSize(): Int = aktiviteter.size
        })
    }

    private fun batchDeleteById(aktiviteter: List<KafkaAktivitetMeldingEntity>): IntArray {
        if (aktiviteter.isEmpty()) {
            return intArrayOf()
        }

        val sql = """
            DELETE FROM KAFKA_AKTIVITET_MELDING
            WHERE AKTIVITET_ID = ?
              AND VERSION <= ?
        """.trimIndent()

        return db.batchUpdate(sql, object : BatchPreparedStatementSetter {
            override fun setValues(ps: PreparedStatement, i: Int) {
                val aktivitet = aktiviteter[i]
                ps.setString(1, aktivitet.value.aktivitetId)
                ps.setLong(2, aktivitet.value.version)
            }

            override fun getBatchSize(): Int = aktiviteter.size
        })
    }

    private fun beholdSisteMeldingPerAktivitet(aktiviteter: List<KafkaAktivitetMeldingEntity>): List<KafkaAktivitetMeldingEntity> {
        val sisteMeldingPerAktivitet = linkedMapOf<String, KafkaAktivitetMeldingEntity>()

        aktiviteter.forEach { aktivitet ->
            sisteMeldingPerAktivitet.remove(aktivitet.value.aktivitetId)
            sisteMeldingPerAktivitet[aktivitet.value.aktivitetId] = aktivitet
        }

        return sisteMeldingPerAktivitet.values.toList()
    }

    private fun normaliserUpdateCount(updateCount: Int): Int =
        when (updateCount) {
            Statement.SUCCESS_NO_INFO -> 1
            Statement.EXECUTE_FAILED -> error("Batch-operasjon mot aktivitet-tabellen feilet.")
            else -> maxOf(updateCount, 0)
        }
}

data class PortefoljeAktivitetBatchResult(
    val mottatte: Int,
    val dedupliserte: Int,
    val prosesserte: Int,
    val ignorert: Int,
)
