package no.nav.pto.veilarbportefolje.aktiviteter.v1

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Statement

@Repository
class PortefoljeAktivitetKafkaMeldingRepository(
    private val db: NamedParameterJdbcTemplate,
) {
    @Transactional
    fun tryLagreAktivitetDataBatch(aktiviteter: List<KafkaAktivitetMeldingEntity>): PortefoljeAktivitetBatchResult {
        if (aktiviteter.isEmpty()) {
            return PortefoljeAktivitetBatchResult(0, 0, 0, 0)
        }

        val endeligeAktiviteter = beholdSisteMeldingPerAktivitet(aktiviteter)
        val slettinger = endeligeAktiviteter.filter { it.historisk }
        val upserts = endeligeAktiviteter.filterNot { it.historisk }

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
            VALUES (
                :aktivitetId, :aktorId, :aktivitetType, :aktivitetStatus, :endringsType,
                :fraDato, :tilDato, :endretDato, :tiltakskode, :lagtInnAv,
                :avtalt, :version, :historisk, :cvKanDelesStatus, :svarfristStillingFraNav,
                :recordOffset, :recordPartition, :recordKey,
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
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

        val params = aktiviteter.map { it.toSqlParams() }.toTypedArray()

        return db.batchUpdate(sql, params)
    }

    private fun batchDeleteById(aktiviteter: List<KafkaAktivitetMeldingEntity>): IntArray {
        if (aktiviteter.isEmpty()) {
            return intArrayOf()
        }

        val sql = """
            DELETE FROM KAFKA_AKTIVITET_MELDING
            WHERE AKTIVITET_ID = :aktivitetId
              AND VERSION <= :version
        """.trimIndent()

        val params = aktiviteter.map {
            MapSqlParameterSource()
                .addValue("aktivitetId", it.aktivitetId)
                .addValue("version", it.version)
        }.toTypedArray()

        return db.batchUpdate(sql, params)
    }

    private fun beholdSisteMeldingPerAktivitet(aktiviteter: List<KafkaAktivitetMeldingEntity>): List<KafkaAktivitetMeldingEntity> {
        val sisteMeldingPerAktivitet = linkedMapOf<String, KafkaAktivitetMeldingEntity>()

        aktiviteter.forEach { aktivitet ->
            sisteMeldingPerAktivitet.remove(aktivitet.aktivitetId)
            sisteMeldingPerAktivitet[aktivitet.aktivitetId] = aktivitet
        }

        return sisteMeldingPerAktivitet.values.toList()
    }

    private fun normaliserUpdateCount(updateCount: Int): Int =
        when (updateCount) {
            Statement.SUCCESS_NO_INFO -> 1
            Statement.EXECUTE_FAILED -> error("Batch-operasjon mot aktivitet-tabellen feilet.")
            else -> maxOf(updateCount, 0)
        }

    private fun KafkaAktivitetMeldingEntity.toSqlParams() = MapSqlParameterSource()
        .addValue("aktivitetId", aktivitetId)
        .addValue("aktorId", aktorId)
        .addValue("aktivitetType", aktivitetType)
        .addValue("aktivitetStatus", aktivitetStatus)
        .addValue("endringsType", endringsType)
        .addValue("fraDato", fraDato)
        .addValue("tilDato", tilDato)
        .addValue("endretDato", endretDato)
        .addValue("tiltakskode", tiltakskode)
        .addValue("lagtInnAv", lagtInnAv)
        .addValue("avtalt", avtalt)
        .addValue("version", version)
        .addValue("historisk", historisk)
        .addValue("cvKanDelesStatus", cvKanDelesStatus)
        .addValue("svarfristStillingFraNav", svarfristStillingFraNav)
        .addValue("recordOffset", recordOffset)
        .addValue("recordPartition", recordPartition)
        .addValue("recordKey", recordKey)
}

data class PortefoljeAktivitetBatchResult(
    val mottatte: Int,
    val dedupliserte: Int,
    val prosesserte: Int,
    val ignorert: Int,
)
