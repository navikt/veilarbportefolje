package no.nav.pto.veilarbportefolje.opensearch;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.opensearch.client.OpenSearchClient;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class StatsReporter implements MeterBinder {
    @Qualifier("PostgresNamedJdbcReadOnly")
    private final NamedParameterJdbcTemplate namedDb;

    private final RestHighLevelClient restHighLevelClient;

    @Override
    public void bindTo(@NonNull MeterRegistry meterRegistry) {
        Gauge.builder("veilarbportefolje.hovedindeksering.indekserer_aktivitet_endringer.last_run", this::indeksererAktivitetEndringerLastRun)
                .register(meterRegistry);

        Gauge.builder("veilarbportefolje.hovedindeksering.deaktiver_utgatte_utdannings_aktivteter.last_run", this::deaktiverUtgatteUtdanningsAktivteterLastRun)
                .register(meterRegistry);

        Gauge.builder("veilarbportefolje.hovedindeksering.indekserer_ytelse_endringer.last_run", this::indeksererYtelseEndringerLastRun)
                .register(meterRegistry);

        Gauge.builder("veilarbportefolje.opensearch.difference_in_versions", this::compareOpensearchVersions)
                .register(meterRegistry);
    }

    private Long indeksererAktivitetEndringerLastRun() {
        String sql = "SELECT last_success FROM SCHEDULED_TASKS WHERE task_name = :taskName::varchar";
        Timestamp sisteKjorte = Optional.ofNullable(namedDb.queryForObject(sql, new MapSqlParameterSource("taskName", "indekserer_aktivitet_endringer"), Timestamp.class)).orElse(null);

        if (sisteKjorte != null) {
            return DateUtils.now().toInstant().toEpochMilli() - sisteKjorte.toInstant().toEpochMilli();
        }
        return null;
    }

    private Long deaktiverUtgatteUtdanningsAktivteterLastRun() {
        String sql = "SELECT last_success FROM SCHEDULED_TASKS WHERE task_name = :taskName::varchar";
        Timestamp sisteKjorte = Optional.ofNullable(namedDb.queryForObject(sql, new MapSqlParameterSource("taskName", "deaktiver_utgatte_utdannings_aktivteter"), Timestamp.class)).orElse(null);

        if (sisteKjorte != null) {
            return DateUtils.now().toInstant().toEpochMilli() - sisteKjorte.toInstant().toEpochMilli();
        }
        return null;
    }

    private Long indeksererYtelseEndringerLastRun() {
        String sql = "SELECT last_success FROM SCHEDULED_TASKS WHERE task_name = :taskName::varchar";
        Timestamp sisteKjorte = Optional.ofNullable(namedDb.queryForObject(sql, new MapSqlParameterSource("taskName", "indekserer_aktivitet_endringer"), Timestamp.class)).orElse(null);

        if (sisteKjorte != null) {
            return DateUtils.now().toInstant().toEpochMilli() - sisteKjorte.toInstant().toEpochMilli();
        }
        return null;
    }

    private Integer compareOpensearchVersions() {
        try {
            String serverVersion = restHighLevelClient.info(RequestOptions.DEFAULT).getVersion().getNumber();
            String libraryVersion = OpenSearchClient.class.getPackage().getImplementationVersion();

            log.info(String.format("Opensearch version: %s, opensearch lib version: %s", serverVersion, libraryVersion));

            if (serverVersion.equals(libraryVersion)) {
                return 1;
            }
            return 0;

        } catch (Exception e) {
            return 0;
        }

    }
}
