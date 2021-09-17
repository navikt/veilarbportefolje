package no.nav.pto.veilarbportefolje.kafka;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.lang.NonNull;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.database.Table;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Slf4j
public class KafkaStats implements MeterBinder {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    private PrometheusMeterRegistry registry;

    public KafkaStats(JdbcTemplate jdbcTemplate, PrometheusMeterRegistry registry) {
        this.jdbcTemplate = jdbcTemplate;
        this.registry = registry;
    }

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        log.info("Reporting Kafka stats");

        List<Integer> retriesStats = getRetriesStats();
        Gauge.builder("veilarbportefolje.kafka.retries.messages_count", retriesStats, (rs) -> retriesStats.size()).description("Number of failed messages").register(this.registry);
        Gauge.builder("veilarbportefolje.kafka.retries.min", retriesStats, (rs) -> retriesStats.stream().mapToInt(v -> v).min().orElse(0)).description("Minimal number of retries for failed messages").register(this.registry);
        Gauge.builder("veilarbportefolje.kafka.retries.max", retriesStats, (rs) -> retriesStats.stream().mapToInt(v -> v).max().orElse(0)).description("Maximal number of retries for failed messages").register(this.registry);
        Gauge.builder("veilarbportefolje.kafka.retries.avg", retriesStats, (rs) -> retriesStats.stream().mapToInt(v -> v).average().orElse(0)).description("Average number of retries for failed messages").register(this.registry);
    }

    private List<Integer> getRetriesStats() {
        log.info("Gathering Kafka stats for failed messages..");

        return this.jdbcTemplate.queryForList("SELECT " + Table.KAFKA_CONSUMER_RECORD.RETRIES + " FROM " +
                Table.KAFKA_CONSUMER_RECORD.TABLE_NAME, Integer.class);
    }
}
