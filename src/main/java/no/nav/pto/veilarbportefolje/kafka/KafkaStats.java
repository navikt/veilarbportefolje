package no.nav.pto.veilarbportefolje.kafka;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.lang.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.database.Table;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaStats implements MeterBinder {
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        log.info("Reporting Kafka stats");

        List<Integer> retriesStats = getRetriesStats();

        Gauge.builder("veilarbportefolje.kafka.retries.messages_count", retriesStats, (rs) -> retriesStats.size()).description("Number of failed messages").register(registry);
        Gauge.builder("veilarbportefolje.kafka.retries.min", retriesStats, (rs) -> retriesStats.stream().mapToInt(v -> v).min().orElse(0)).description("Minimal number of retries for failed messages").register(registry);
        Gauge.builder("veilarbportefolje.kafka.retries.max", retriesStats, (rs) -> retriesStats.stream().mapToInt(v -> v).max().orElse(0)).description("Maximal number of retries for failed messages").register(registry);
        Gauge.builder("veilarbportefolje.kafka.retries.avg", retriesStats, (rs) -> retriesStats.stream().mapToInt(v -> v).average().orElse(0)).description("Average number of retries for failed messages").register(registry);
    }

    private List<Integer> getRetriesStats() {
        log.info("Gathering Kafka stats for failed messages..");

        return this.jdbcTemplate.queryForList("SELECT " + Table.KAFKA_CONSUMER_RECORD.RETRIES + " FROM " +
                Table.KAFKA_CONSUMER_RECORD.TABLE_NAME, Integer.class);
    }
}
