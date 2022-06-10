package no.nav.pto.veilarbportefolje.kafka;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class KafkaStats implements MeterBinder {
    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        List<Integer> retriesStats = getRetriesStats();
        Gauge.builder("veilarbportefolje_kafka_retries_messages_count", retriesStats, (rs) -> retriesStats.size()).description("Number of failed messages").register(meterRegistry);
        Gauge.builder("veilarbportefolje_kafka_retries_max", retriesStats, (rs) -> retriesStats.stream().mapToInt(v -> v).max().orElse(0)).description("Maximal number of retries for failed messages").register(meterRegistry);
        Gauge.builder("veilarbportefolje_kafka_retries_avg", retriesStats, (rs) -> retriesStats.stream().mapToInt(v -> v).average().orElse(0)).description("Average number of retries for failed messages").register(meterRegistry);
    }

    private List<Integer> getRetriesStats() {
        return this.jdbcTemplate.queryForList("SELECT retries FROM KAFKA_CONSUMER_RECORD WHERE retries > 0", Integer.class);
    }
}
