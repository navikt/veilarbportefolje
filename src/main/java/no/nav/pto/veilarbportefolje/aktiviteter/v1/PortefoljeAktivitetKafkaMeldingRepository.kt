package no.nav.pto.veilarbportefolje.aktiviteter.v1

import io.getunleash.DefaultUnleash
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.AktorId
import no.nav.pto.veilarbportefolje.config.FeatureToggle
import no.nav.pto.veilarbportefolje.database.PostgresTable.KAFKA_AKTIVITET_MELDING.*
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.lang.Boolean.TRUE
import java.sql.Statement

@Slf4j
@Repository
@RequiredArgsConstructor
class PortefoljeAktivitetKafkaMeldingRepository(
    private val db: NamedParameterJdbcTemplate,
    private val opensearchIndexer: OpensearchIndexer,
    private val defaultUnleash: DefaultUnleash,
) {
    @Transactional
    fun behandleAktivitetsKafkaMeldinger(aktiviteter: List<KafkaAktivitetMeldingEntity>): PortefoljeAktivitetBatchResult {
        if (aktiviteter.isEmpty()) {
            return PortefoljeAktivitetBatchResult(0, 0, 0, 0)
        }

        /*
        Vi skiller mellom tiltaksaktiviteter og øvrige aktiviteter fordi vi i første omgang ønsker å indeksere kun tiltaksaktiviteter fra `kafka_aktivitet_melding`-tabellen.
        Dette gjør vi for å redusere omfanget av endringen og kunne ta i bruk den nye datakilden stegvis.
        På den måten reduserer vi risikoen ved overgangen til den nye datakilden.
        */
        val endeligeAktiviteter = beholdSisteMeldingPerAktivitet(aktiviteter)
        val endeligeAktiviteterEkskludertTiltak = beholdSisteMeldingPerAktivitet(aktiviteter).filterNot { it.aktivitetType == Aktivitetstype.TILTAK.name && it.avtalt == TRUE }

        val tiltaksaktiviteter = endeligeAktiviteter.filter { it.aktivitetType == Aktivitetstype.TILTAK.name && it.avtalt == TRUE }
        val antallTiltaksAktiviteterBehandlet = behandleTiltakaktivitetsKafkaMeldinger(tiltaksaktiviteter)

        val inaktivAktiviteter = endeligeAktiviteterEkskludertTiltak.filter { it.historisk }
        val aktivAktiviteter = endeligeAktiviteterEkskludertTiltak.filterNot { it.historisk }

        val antallAktiviteterSlettet = batchDeleteById(inaktivAktiviteter).sumOf(::normaliserUpdateCount)
        val antallAktiviteterOppdatert = batchUpsertAktivitet(aktivAktiviteter).sumOf(::normaliserUpdateCount)

        val antallMeldingerBehandlet = antallAktiviteterSlettet + antallAktiviteterOppdatert + antallTiltaksAktiviteterBehandlet

        return PortefoljeAktivitetBatchResult(
            mottatte = aktiviteter.size,
            dedupliserte = aktiviteter.size - endeligeAktiviteter.size,
            prosesserte = antallMeldingerBehandlet,
            ignorert = endeligeAktiviteter.size - antallMeldingerBehandlet,
        )
    }

    fun behandleTiltakaktivitetsKafkaMeldinger(tiltaksaktiviteter: List<KafkaAktivitetMeldingEntity>): Int {
        val inaktivTiltaksAktiviteter = tiltaksaktiviteter.filter { it.historisk }
        val aktivTiltaksAktiviteter = tiltaksaktiviteter.filterNot { it.historisk }

        val antallTiltaksAktiviteterSlettet = batchDeleteById(inaktivTiltaksAktiviteter).sumOf(::normaliserUpdateCount)
        val antallTiltaksAktiviteterOppdatert = batchUpsertAktivitet(aktivTiltaksAktiviteter).sumOf(::normaliserUpdateCount)

        val antallTiltaksMeldinger = antallTiltaksAktiviteterSlettet + antallTiltaksAktiviteterOppdatert

        if(defaultUnleash.isEnabled(FeatureToggle.BRUK_TILTAKSAKTIVITET_FRA_AKTIVITETSPLAN) && tiltaksaktiviteter.isNotEmpty()) {
            if (antallTiltaksAktiviteterOppdatert == 1) {
                indekserAktivitet(AktorId.of(aktivTiltaksAktiviteter.first().aktorId))
            } else if (antallTiltaksAktiviteterSlettet == 1) {
                indekserAktivitet(AktorId.of(inaktivTiltaksAktiviteter.first().aktorId))
            } else if (antallTiltaksAktiviteterOppdatert > 1 || antallTiltaksAktiviteterSlettet > 1) {
                indekserAktiviteter(tiltaksaktiviteter)
            }
        }
         return antallTiltaksMeldinger;
    }

    private fun batchUpsertAktivitet(aktiviteter: List<KafkaAktivitetMeldingEntity>): IntArray {
        if (aktiviteter.isEmpty()) {
            return intArrayOf()
        }

        val sql = """
            INSERT INTO $TABLE_NAME (
                $AKTIVITET_ID,
                $AKTOR_ID,
                $AKTIVITET_TYPE,
                $AKTIVITET_STATUS,
                $ENDRINGS_TYPE,
                $FRA_DATO,
                $TIL_DATO,
                $ENDRET_DATO,
                $TILTAKSKODE,
                $LAGT_INN_AV,
                $AVTALT,
                $VERSION,
                $HISTORISK,
                $CV_KAN_DELES_STATUS,
                $SVARFRIST_STILLING_FRA_NAV,
                $RECORD_OFFSET,
                $RECORD_PARTITION,
                $RECORD_KEY,
                $RAD_OPPRETTET,
                $RAD_OPPDATERT
            )
            VALUES (
                :aktivitetId, :aktorId, :aktivitetType, :aktivitetStatus, :endringsType,
                :fraDato, :tilDato, :endretDato, :tiltakskode, :lagtInnAv,
                :avtalt, :version, :historisk, :cvKanDelesStatus, :svarfristStillingFraNav,
                :recordOffset, :recordPartition, :recordKey,
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT ($AKTIVITET_ID) DO UPDATE
            SET $AKTOR_ID = excluded.$AKTOR_ID,
                $AKTIVITET_TYPE = excluded.$AKTIVITET_TYPE,
                $AKTIVITET_STATUS = excluded.$AKTIVITET_STATUS,
                $ENDRINGS_TYPE = excluded.$ENDRINGS_TYPE,
                $FRA_DATO = excluded.$FRA_DATO,
                $TIL_DATO = excluded.$TIL_DATO,
                $ENDRET_DATO = excluded.$ENDRET_DATO,
                $TILTAKSKODE = excluded.$TILTAKSKODE,
                $LAGT_INN_AV = excluded.$LAGT_INN_AV,
                $AVTALT = excluded.$AVTALT,
                $VERSION = excluded.$VERSION,
                $HISTORISK = excluded.$HISTORISK,
                $CV_KAN_DELES_STATUS = excluded.$CV_KAN_DELES_STATUS,
                $SVARFRIST_STILLING_FRA_NAV = excluded.$SVARFRIST_STILLING_FRA_NAV,
                $RECORD_OFFSET = excluded.$RECORD_OFFSET,
                $RECORD_PARTITION = excluded.$RECORD_PARTITION,
                $RECORD_KEY = excluded.$RECORD_KEY,
                $RAD_OPPDATERT = CURRENT_TIMESTAMP
            WHERE $TABLE_NAME.$VERSION <= excluded.$VERSION
        """.trimIndent()

        val params = aktiviteter.map { it.toSqlParams() }.toTypedArray()

        return db.batchUpdate(sql, params)
    }

    private fun batchDeleteById(aktiviteter: List<KafkaAktivitetMeldingEntity>): IntArray {
        if (aktiviteter.isEmpty()) {
            return intArrayOf()
        }

        val sql = """
            DELETE FROM $TABLE_NAME
            WHERE $AKTIVITET_ID = :aktivitetId
              AND $VERSION <= :version
        """.trimIndent()

        val params = aktiviteter.map {
            MapSqlParameterSource()
                .addValue("aktivitetId", it.aktivitetId)
                .addValue("version", it.version)
        }.toTypedArray()

        return db.batchUpdate(sql, params)
    }

    private fun indekserAktiviteter(aktiviteter: List<KafkaAktivitetMeldingEntity>) {
        if (aktiviteter.isEmpty()) {
            return
        }
        opensearchIndexer.indekserBolk(aktiviteter.map { AktorId.of(it.aktorId) })
        secureLog.info("IndekserBolk: Indekserte {} aktivitetdmeldingne", aktiviteter.size)
    }

    private fun indekserAktivitet(aktorId: AktorId) {
        opensearchIndexer.indekser(aktorId)
        secureLog.info("Indekserte en aktivitetdmelding med aktorId", aktorId)
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
