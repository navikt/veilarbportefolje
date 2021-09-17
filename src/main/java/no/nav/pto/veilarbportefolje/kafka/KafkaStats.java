package no.nav.pto.veilarbportefolje.kafka;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.database.Table;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class KafkaStats {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    private PrometheusMeterRegistry registry;

    public KafkaStats(JdbcTemplate jdbcTemplate, PrometheusMeterRegistry registry) {
        this.jdbcTemplate = jdbcTemplate;
        this.registry = registry;
    }

    @Scheduled(cron = "*/5 * * * * ?")
    public void reportStats() {
        List<Integer> retriesStats = getRetriesStats();
        Gauge.builder("veilarbportefolje_kafka_retries_messages_count", retriesStats, (rs) -> retriesStats.size()).description("Number of failed messages").register(this.registry);
        Gauge.builder("veilarbportefolje_kafka_retries_max", retriesStats, (rs) -> retriesStats.stream().mapToInt(v -> v).max().orElse(0)).description("Maximal number of retries for failed messages").register(this.registry);
        Gauge.builder("veilarbportefolje_kafka_retries_avg", retriesStats, (rs) -> retriesStats.stream().mapToInt(v -> v).average().orElse(0)).description("Average number of retries for failed messages").register(this.registry);
    }

    private List<Integer> getRetriesStats() {
        return this.jdbcTemplate.queryForList("SELECT " + Table.KAFKA_CONSUMER_RECORD.RETRIES + " FROM " +
                        Table.KAFKA_CONSUMER_RECORD.TABLE_NAME + " WHERE " + Table.KAFKA_CONSUMER_RECORD.RETRIES + " > 0 " +
                        " AND " + Table.KAFKA_CONSUMER_RECORD.LAST_RETRY + " > '" +
                        Timestamp.valueOf(LocalDateTime.now().minusHours(1)) + "'"
                , Integer.class);
    }
}
