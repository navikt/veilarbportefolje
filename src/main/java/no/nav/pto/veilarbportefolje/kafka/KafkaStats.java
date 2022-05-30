package no.nav.pto.veilarbportefolje.kafka;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaStats {
    private final JdbcTemplate jdbcTemplate;
    private final PrometheusMeterRegistry registry;

    @Scheduled(fixedRate = 30000)
    public void reportStats() {
        List<Integer> retriesStats = getRetriesStats();
        Gauge.builder("veilarbportefolje_kafka_retries_messages_count", retriesStats, (rs) -> retriesStats.size()).description("Number of failed messages").register(this.registry);
        Gauge.builder("veilarbportefolje_kafka_retries_max", retriesStats, (rs) -> retriesStats.stream().mapToInt(v -> v).max().orElse(0)).description("Maximal number of retries for failed messages").register(this.registry);
        Gauge.builder("veilarbportefolje_kafka_retries_avg", retriesStats, (rs) -> retriesStats.stream().mapToInt(v -> v).average().orElse(0)).description("Average number of retries for failed messages").register(this.registry);
    }

    private List<Integer> getRetriesStats() {
        return this.jdbcTemplate.queryForList("""
                        SELECT retries FROM KAFKA_CONSUMER_RECORD WHERE retries > 0
                        AND LAST_RETRY > 'now'::timestamp - '1 hour'::interval;
                """, Integer.class);
    }
}
