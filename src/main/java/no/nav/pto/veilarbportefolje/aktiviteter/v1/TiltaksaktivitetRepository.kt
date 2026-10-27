package no.nav.pto.veilarbportefolje.aktiviteter.v1

import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVE_IDENTER.AKTORID
import no.nav.pto.veilarbportefolje.database.PostgresTable.AO_KONTOR.KONTOR_ID
import no.nav.pto.veilarbportefolje.database.PostgresTable.KAFKA_AKTIVITET_MELDING.*
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingPeriodeService
import no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.util.*
import no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVE_IDENTER.TABLE_NAME as AKTIVE_IDENTER_TABLE
import no.nav.pto.veilarbportefolje.database.PostgresTable.AO_KONTOR.TABLE_NAME as AO_KONTOR_TABLE
import no.nav.pto.veilarbportefolje.database.PostgresTable.KAFKA_AKTIVITET_MELDING.TABLE_NAME as KAFKA_AKTIVITET_MELDING_TABLE

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
                SELECT *
                FROM tiltakkodeverket WHERE
                kode IN (
                     SELECT DISTINCT aktiviteter.$TILTAKSKODE 
                     FROM $KAFKA_AKTIVITET_MELDING_TABLE aktiviteter
                     INNER JOIN $AKTIVE_IDENTER_TABLE aktiv_ident 
                     ON aktiv_ident.$AKTORID = aktiviteter.$AKTOR_ID
                     INNER JOIN $AO_KONTOR_TABLE ao_kontor 
                     ON ao_kontor.$AKTORID = aktiv_ident.$AKTORID
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

    fun leggTilTiltaksAktiviteter(
        aktorIder: String,
        avtalt: Boolean,
        result: HashMap<AktorId, List<AktivitetEntityDto>>
    ) {
        //language=postgresql
        val sql = """
        SELECT $AKTOR_ID,
               CAST($TIL_DATO AS TIMESTAMP) AS TIL_DATO,
               CAST($FRA_DATO AS TIMESTAMP) AS FRA_DATO,
               $TILTAKSKODE FROM $KAFKA_AKTIVITET_MELDING_TABLE
        WHERE $AKTOR_ID = ANY (:aktorIder::varchar[])
        AND $AKTIVITET_TYPE = :aktivitetstype::varchar
        AND $AVTALT = :avtalt::boolean
        AND NOT $AKTIVITET_STATUS = ANY (string_to_array(:inaktivAktivitetStatus, ',')::text[])
        
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("aktorIder", aktorIder)
            .addValue("avtalt", avtalt)
            .addValue("aktivitetstype", "TILTAK")
            .addValue("inaktivAktivitetStatus", InaktivAktivitetStatus.entries.joinToString(",") { it.name })

        jdbc.query(sql, params) { rs ->
            val aktoerId = AktorId.of(rs.getString("AKTOR_ID"))
            val aktivitet = AktivitetEntityDto.mapTiltaksAktivitetTilEntity(rs)

            result.compute(aktoerId) { _, list ->
                AktivitetEntityDto.leggTilAktivitetPaResultat(aktivitet, list)
            }
        }
    }
}

data class  TiltakskodeMapping (
    val tiltak: MutableMap<String, String>
)