package no.nav.pto.veilarbportefolje.aktiviteter.v1

import no.nav.common.types.identer.EnhetId
import no.nav.pto.veilarbportefolje.aktiviteter.domene.InaktivAktivitetStatus
import no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVE_IDENTER.AKTORID
import no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVE_IDENTER.FNR
import no.nav.pto.veilarbportefolje.database.PostgresTable.AO_KONTOR.IDENT
import no.nav.pto.veilarbportefolje.database.PostgresTable.AO_KONTOR.KONTOR_ID
import no.nav.pto.veilarbportefolje.database.PostgresTable.KAFKA_AKTIVITET_MELDING.*
import no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.FODSELSNR
import no.nav.pto.veilarbportefolje.database.PostgresTable.TILTAKKODEVERK.KODE as TILTAKSKODE
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVE_IDENTER.TABLE_NAME as AKTIVE_IDENTER_TABLE
import no.nav.pto.veilarbportefolje.database.PostgresTable.AO_KONTOR.TABLE_NAME as AO_KONTOR_TABLE
import no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.TABLE_NAME as ARENA_OPPFOLGINGSBRUKER_TABLE
import no.nav.pto.veilarbportefolje.database.PostgresTable.KAFKA_AKTIVITET_MELDING.TABLE_NAME as KAFKA_AKTIVITET_MELDING_TABLE

@Component
class BrukertiltakRepository(
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
                     SELECT DISTINCT $TILTAKSKODE FROM $KAFKA_AKTIVITET_MELDING_TABLE aktiviteter
                     INNER JOIN $AKTIVE_IDENTER_TABLE ai on ai.$AKTORID = aktiviteter.$AKTOR_ID
                     INNER JOIN $ARENA_OPPFOLGINGSBRUKER_TABLE OP ON OP.$FODSELSNR = ai.$FNR
                     LEFT JOIN $AO_KONTOR_TABLE ao_kontor ON ao_kontor.$IDENT = ai.$FNR
                     WHERE ao_kontor.$KONTOR_ID = :enhetId
                     AND NOT ($AKTIVITET_STATUS = ANY (:inaktivAktivitetStatus))
                     AND $AVTALT = true
                )
            """.trimIndent()

        val params = MapSqlParameterSource(
            mapOf(
                "enhetId" to enhetId,
                "inaktivAktivitetStatus" to InaktivAktivitetStatus.entries.joinToString(",", prefix = "{", postfix = "}")
            )
        )

        val tiltak: Map<String, String> = jdbc.query(
            sql,
            params,
        ) { rs, _ ->
            rs.getString("kode") to rs.getString("verdi")
        }.toMap()

        return TiltakskodeMapping(tiltak = tiltak as MutableMap<String, String>)
    }
}

data class  TiltakskodeMapping (
    val tiltak: MutableMap<String, String>
)