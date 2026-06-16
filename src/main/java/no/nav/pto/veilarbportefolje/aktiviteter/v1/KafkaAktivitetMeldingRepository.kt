package no.nav.pto.veilarbportefolje.aktiviteter.v1

import io.getunleash.DefaultUnleash
import no.nav.common.types.identer.EnhetId
import no.nav.pto.veilarbportefolje.aktiviteter.domene.AktivitetIkkeAktivStatuser
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.EnhetTiltak
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV3
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.Tiltakkodeverk
import no.nav.pto.veilarbportefolje.config.FeatureToggle
import no.nav.pto.veilarbportefolje.database.PostgresTable.KAFKA_AKTIVITET_MELDING.*;
import org.opensearch.common.inject.util.Types.mapOf
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.function.Function
import java.util.stream.Collectors
import no.nav.pto.veilarbportefolje.database.PostgresTable.AO_KONTOR.TABLE_NAME as AO_KONTOR_TABLE
import no.nav.pto.veilarbportefolje.database.PostgresTable.KAFKA_AKTIVITET_MELDING.TABLE_NAME as KAFKA_AKTIVITET_MELDING_TABLE

/**
 * Repository for [TABLE_NAME]-tabellen
 */
@Component
class KafkaAktivitetMeldingRepository(
    private val namedJdbcTemplate: NamedParameterJdbcTemplate,
    private val defaultUnleash: DefaultUnleash? = null
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
            SELECT * FROM $KAFKA_AKTIVITET_MELDING_TABLE
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

    fun hentTiltakskoderForBrukerePaaEnhet(
        EnhetId: EnhetId,
    ): EnhetTiltak {
        val brukAoKontor = FeatureToggle.brukKontorFraAoKontor(defaultUnleash)
            //language=postgresql
            val hentTiltakPaaEnhetSql = """
                SELECT *
                FROM tiltakkodeverket WHERE
                kode IN (
                     SELECT DISTINCT $TILTAKSKODE FROM $KAFKA_AKTIVITET_MELDING_TABLE aktiviteter
                     INNER JOIN aktive_identer ai on ai.aktorid = aktiviteter.aktor_id
                     INNER JOIN oppfolgingsbruker_arena_v2 OP ON OP.fodselsnr = ai.fnr
                     LEFT JOIN ${AO_KONTOR_TABLE} ao_kontor ON ao_kontor.ident = ai.fnr
                     WHERE coalesce(CASE WHEN :brukAoKontor::boolean THEN ao_kontor.kontor_id ELSE NULL END, OP.nav_kontor) = :enhetId
                     AND $AVTALT = true
                     AND NOT ($AKTIVITET_STATUS = ANY (:aktivitetIkkeAktivStatus))
                     AND $TILTAKSKODE IS NOT NULL
                )
            """.trimIndent()

            val params = MapSqlParameterSource(
                mapOf(
                    "brukAoKontor" to brukAoKontor,
                    "enhetId" to EnhetId,
                    "aktivitetIkkeAktivStatus" to AktivitetIkkeAktivStatuser.entries.joinToString(",", prefix = "{", postfix = "}")
                )
            )

        val tiltak: Map<String, String> = namedJdbcTemplate.query(
                hentTiltakPaaEnhetSql,
                params,
            ) { rs, _ ->
                    rs.getString("kode") to rs.getString("verdi")
            }.toMap()

        return EnhetTiltak().setTiltak(tiltak)
    }
}
