package no.nav.pto.veilarbportefolje.aktiviteter.v1

import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.EnhetId
import no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVE_IDENTER.AKTORID
import no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVE_IDENTER.FNR
import no.nav.pto.veilarbportefolje.database.PostgresTable.AO_KONTOR.IDENT
import no.nav.pto.veilarbportefolje.database.PostgresTable.AO_KONTOR.KONTOR_ID
import no.nav.pto.veilarbportefolje.database.PostgresTable.KAFKA_AKTIVITET_MELDING.*
import no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.FODSELSNR
import no.nav.pto.veilarbportefolje.database.PostgresTable.KAFKA_AKTIVITET_MELDING.TILTAKSKODE as TILTAKSKODE
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVE_IDENTER.TABLE_NAME as AKTIVE_IDENTER_TABLE
import no.nav.pto.veilarbportefolje.database.PostgresTable.AO_KONTOR.TABLE_NAME as AO_KONTOR_TABLE
import no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.TABLE_NAME as ARENA_OPPFOLGINGSBRUKER_TABLE
import no.nav.pto.veilarbportefolje.database.PostgresTable.KAFKA_AKTIVITET_MELDING.TABLE_NAME as KAFKA_AKTIVITET_MELDING_TABLE

@Slf4j
@Repository
@Component
class TiltaksaktivitetRepository(
    private val jdbc: NamedParameterJdbcTemplate
) {
    fun hentTiltakstyperForEnhet(
        enhetId: EnhetId,
    ): TiltakskodeMapping {
        //language=postgresql
        val sql = """
                SELECT *
                FROM tiltakkodeverket WHERE
                kode IN (
                     SELECT DISTINCT aktiviteter.$TILTAKSKODE FROM $KAFKA_AKTIVITET_MELDING_TABLE aktiviteter
                     INNER JOIN $AKTIVE_IDENTER_TABLE ai on ai.$AKTORID = aktiviteter.$AKTOR_ID
                     INNER JOIN $ARENA_OPPFOLGINGSBRUKER_TABLE OP ON OP.$FODSELSNR = ai.$FNR
                     INNER JOIN $AO_KONTOR_TABLE ao_kontor ON ao_kontor.$IDENT = ai.$FNR
                     WHERE ao_kontor.$KONTOR_ID = :enhetId
                     AND NOT aktiviteter.$AKTIVITET_STATUS = ANY (string_to_array(:inaktivAktivitetStatus, ',')::text[])
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
}

data class  TiltakskodeMapping (
    val tiltak: MutableMap<String, String>
)