package no.nav.pto.veilarbportefolje.opensearch

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import no.nav.pto.veilarbportefolje.config.SchedulConfig
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Oppfolging.ENHET_ID
import no.nav.pto.veilarbportefolje.util.DateUtils
import org.opensearch.action.search.SearchRequest
import org.opensearch.client.OpenSearchClient
import org.opensearch.client.RequestOptions
import org.opensearch.client.RestHighLevelClient
import org.opensearch.index.query.QueryBuilders
import org.opensearch.search.builder.SearchSourceBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.util.concurrent.TimeUnit

@Component
class StatsReporter(
    @field:Qualifier("PostgresNamedJdbcReadOnly") private val namedDb: NamedParameterJdbcTemplate,
    private val restHighLevelClient: RestHighLevelClient
) : MeterBinder {
    override fun bindTo(meterRegistry: MeterRegistry) {
        Gauge.builder("veilarbportefolje.hovedindeksering.indekserer_aktivitet_endringer.last_run")
        { indeksererAktivitetEndringerLastRun() }
            .register(meterRegistry)

        Gauge.builder("veilarbportefolje.hovedindeksering.deaktiver_utgatte_utdannings_aktivteter.last_run")
        { deaktiverUtgatteUtdanningsAktivteterLastRun() }
            .register(meterRegistry)

        Gauge.builder("veilarbportefolje.hovedindeksering.indekserer_ytelse_endringer.last_run")
        { indeksererYtelseEndringerLastRun() }
            .register(meterRegistry)

        Gauge.builder("veilarbportefolje.opensearch.difference_in_versions")
        { compareOpensearchVersions() }
            .register(meterRegistry)

        Gauge.builder("veilarbportefolje.hovedindeksering.slett_data_for_barn_over_18.last_run")
        { slettDataForBarnOver18LastRun() }
            .register(meterRegistry)

        Gauge.builder("veilarbportefolje.opensearch.antall_personer_uten_enhet_id")
        { antallPersonerUtenEnhetId() }
            .register(meterRegistry)
    }

    private fun indeksererAktivitetEndringerLastRun(): Long {
        val sql = "SELECT last_success FROM SCHEDULED_TASKS WHERE task_name = :taskName"
        val sisteKjorte: Timestamp? = namedDb.queryForObject(
            sql,
            MapSqlParameterSource("taskName", SchedulConfig.indeksererAktivitetEndringer),
            Timestamp::class.java
        )

        if (sisteKjorte != null) {
            return TimeUnit.MILLISECONDS.toHours(DateUtils.calculateTimeElapsed(sisteKjorte.toInstant()).toMillis())
        }
        return -1L
    }

    private fun deaktiverUtgatteUtdanningsAktivteterLastRun(): Long {
        val sql = "SELECT last_success FROM SCHEDULED_TASKS WHERE task_name = :taskName"
        val sisteKjorte: Timestamp? = namedDb.queryForObject(
            sql,
            MapSqlParameterSource("taskName", SchedulConfig.deaktiverUtgatteUtdanningsAktivteter),
            Timestamp::class.java
        )

        if (sisteKjorte != null) {
            return TimeUnit.MILLISECONDS.toHours(DateUtils.calculateTimeElapsed(sisteKjorte.toInstant()).toMillis())
        }

        return -1L
    }

    private fun indeksererYtelseEndringerLastRun(): Long {
        val sql = "SELECT last_success FROM SCHEDULED_TASKS WHERE task_name = :taskName"
        val sisteKjorte: Timestamp? = namedDb.queryForObject(
            sql,
            MapSqlParameterSource("taskName", SchedulConfig.indeksererYtelseEndringer),
            Timestamp::class.java
        )

        if (sisteKjorte != null) {
            return TimeUnit.MILLISECONDS.toHours(DateUtils.calculateTimeElapsed(sisteKjorte.toInstant()).toMillis())
        }

        return -1L
    }

    private fun slettDataForBarnOver18LastRun(): Long {
        val sql = "SELECT last_success FROM SCHEDULED_TASKS WHERE task_name = :taskName"
        val sisteKjorte: Timestamp? = namedDb.queryForObject(
            sql,
            MapSqlParameterSource("taskName", SchedulConfig.slettDataForBarnSomErOver18),
            Timestamp::class.java
        )

        if (sisteKjorte != null) {
            return TimeUnit.MILLISECONDS.toHours(DateUtils.calculateTimeElapsed(sisteKjorte.toInstant()).toMillis())
        }

        return -1L
    }

    private fun compareOpensearchVersions(): Int {
        try {
            val serverVersion = restHighLevelClient.info(RequestOptions.DEFAULT).version.number
            val libraryVersion = OpenSearchClient::class.java.getPackage().implementationVersion

            if (serverVersion[0] != libraryVersion[0]) {
                log.error(
                    String.format(
                        "Differanse mellom major-versjoner Opensearch og Opensearch klientbibliotek. Opensearch: version: %s, opensearch lib version: %s",
                        serverVersion,
                        libraryVersion
                    )
                )
            }

            if (serverVersion == libraryVersion) {
                return 1
            }
            return 0
        } catch (_: Exception) {
            return 0
        }
    }

    private fun antallPersonerUtenEnhetId(): Long {
        try {
            val query = SearchSourceBuilder()
                .size(0)
                .trackTotalHits(true)
                .query(
                    QueryBuilders.boolQuery()
                        .mustNot(QueryBuilders.existsQuery(ENHET_ID))
                )

            val request = SearchRequest()
                .indices(OpensearchConfig.BRUKERINDEKS_ALIAS)
                .source(query)

            val response = restHighLevelClient.search(request, RequestOptions.DEFAULT)
            return response.hits.totalHits?.value ?: -1L
        } catch (e: Exception) {
            log.error("Klarte ikke hente antall personer uten enhet_id fra OpenSearch", e)
            return -1L
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(StatsReporter::class.java)
    }
}
