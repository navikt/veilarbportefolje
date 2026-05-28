package no.nav.pto.veilarbportefolje.aktiviteter.v1

import no.nav.pto.veilarbportefolje.aktiviteter.domene.AktivitetIkkeAktivStatuser
import no.nav.pto.veilarbportefolje.database.PostgresTable.KAFKA_AKTIVITET_MELDING
import no.nav.pto.veilarbportefolje.database.PostgresTable.KAFKA_AKTIVITET_MELDING.*
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

/**
 * Repository for [TABLE_NAME]-tabellen
 */
@Component
class KafkaAktivitetMeldingRepository(
    private val namedJdbcTemplate: NamedParameterJdbcTemplate
) {
    fun listKafkaAktivitetMeldingerBulk(
        personIdenter: Set<String>,
        avtalt: Boolean,
        aktivitetStatusFilter: Set<AktivitetIkkeAktivStatuser>
    ): Map<String, List<KafkaAktivitetMeldingEntity>> {
        val personIdenterArrayLiteral = personIdenter.joinToString(separator = ",", prefix = "{", postfix = "}")
        val aktivitetStatusFilterArrayLiteral = aktivitetStatusFilter.joinToString(",", prefix = "{", postfix = "}")

        val params = MapSqlParameterSource(
            mapOf(
                "identer" to personIdenterArrayLiteral,
                "avtalt" to avtalt,
                "status" to aktivitetStatusFilterArrayLiteral
            )
        )
        //language=postgresql
        val sql = """
            SELECT * FROM $TABLE_NAME
            WHERE $AKTOR_ID = ANY (:identer)
            AND $AVTALT = :avtalt
            AND NOT ($AKTIVITET_STATUS = ANY (:statusFilter))
        """.trimIndent()

        return namedJdbcTemplate.query(sql, params) { rs, _ ->
            KafkaAktivitetMeldingEntity(
                aktivitetId = rs.getString(AKTIVITET_ID),
                aktorId = rs.getString(AKTOR_ID),
                aktivitetType = rs.getString(AKTIVITET_TYPE),
                aktivitetStatus = rs.getString(AKTIVITET_STATUS),
                endringsType = rs.getString(ENDRINGS_TYPE),
                fraDato = rs.getString(FRA_DATO),
                tilDato = rs.getString(TIL_DATO),
                endretDato = rs.getString(ENDRET_DATO),
                tiltakskode = rs.getString(TILTAKSKODE),
                lagtInnAv = rs.getString(LAGT_INN_AV),
                avtalt = rs.getBoolean(AVTALT),
                version = rs.getLong(VERSION),
                historisk = rs.getBoolean(HISTORISK),
                cvKanDelesStatus = rs.getString(CV_KAN_DELES_STATUS),
                svarfristStillingFraNav = rs.getString(SVARFRIST_STILLING_FRA_NAV),
                recordOffset = rs.getLong(RECORD_OFFSET),
                recordPartition = rs.getInt(RECORD_PARTITION),
                recordKey = rs.getString(RECORD_KEY),
            )
        }.groupBy { it.aktorId }
    }
}
