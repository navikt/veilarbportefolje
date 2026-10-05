package no.nav.pto.veilarbportefolje.aktiviteter.v1

import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.EnhetId
import no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVE_IDENTER.AKTORID
import no.nav.pto.veilarbportefolje.database.PostgresTable.AO_KONTOR.KONTOR_ID
import no.nav.pto.veilarbportefolje.database.PostgresTable.KAFKA_AKTIVITET_MELDING.*
import no.nav.pto.veilarbportefolje.database.PostgresTable.TILTAKKODEVERK.KODE
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingPeriodeService
import org.slf4j.LoggerFactory
import no.nav.pto.veilarbportefolje.database.PostgresTable.KAFKA_AKTIVITET_MELDING.TILTAKSKODE as TILTAKSKODE
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import no.nav.pto.veilarbportefolje.database.PostgresTable.AO_KONTOR.TABLE_NAME as AO_KONTOR_TABLE
import no.nav.pto.veilarbportefolje.database.PostgresTable.KAFKA_AKTIVITET_MELDING.TABLE_NAME as KAFKA_AKTIVITET_MELDING_TABLE
import no.nav.pto.veilarbportefolje.database.PostgresTable.TILTAKKODEVERK.TABLE_NAME as TILTAKKODEVERK_TABLE

@Slf4j
@Repository
@Component
class TiltaksaktivitetRepository(
    private val jdbc: NamedParameterJdbcTemplate
) {
    private val log = LoggerFactory.getLogger(OppfolgingPeriodeService::class.java)

    fun hentTiltakstyperForEnhet(
        enhetId: EnhetId,
    ): TiltakskodeMapping {
        //language=postgresql
        val sql = """
                SELECT tk.*
                FROM $TILTAKKODEVERK_TABLE tk 
                WHERE EXISTS (
                     SELECT 1 
                     FROM $KAFKA_AKTIVITET_MELDING_TABLE aktiviteter
                     JOIN $AO_KONTOR_TABLE ao_kontor 
                       ON ao_kontor.$AKTORID = aktiviteter.$AKTOR_ID
                     WHERE aktiviteter.$TILTAKSKODE = tk.$KODE
                       AND ao_kontor.$KONTOR_ID = :enhetId
                       AND aktiviteter.$AKTIVITET_STATUS <> ALL(string_to_array(:inaktivAktivitetStatus, ',')::text[])
                       AND aktiviteter.$AVTALT = true
                )
            """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("enhetId", enhetId.get())
            // pass comma-separated statuses and convert to SQL array with string_to_array in the query
            .addValue("inaktivAktivitetStatus", InaktivAktivitetStatus.entries.joinToString(",") { it.name })

        val tiltak: Map<String, String> = jdbc.query(
            sql,
            params,
        ) { rs, _ ->
            val kode = rs.getString("kode")
            val verdi = rs.getString("verdi")
            if (kode != null && verdi != null)
                kode to verdi
            else
                null
        }.filterNotNull().toMap()

        return TiltakskodeMapping(tiltak = tiltak.toMutableMap())
    }

    fun lagreTiltakskodenavn(tiltakskode: String, tiltaksnavn: String) {
        //language=postgresql
        val sql = """
            INSERT INTO tiltakkodeverket (kode, verdi)
            VALUES (:tiltakskode, :tiltaksnavn)
            ON CONFLICT (kode) DO UPDATE SET verdi = EXCLUDED.verdi
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("tiltakskode", tiltakskode)
            .addValue("tiltaksnavn", tiltaksnavn)

        jdbc.update(sql, params)
        log.info("Lagret eller oppdatert tiltakskode: $tiltakskode with name: $tiltaksnavn")
    }
}

data class  TiltakskodeMapping (
    val tiltak: MutableMap<String, String>
)