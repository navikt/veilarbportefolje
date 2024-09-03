package no.nav.pto.veilarbportefolje.kafka;

import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class KafkaStats implements MeterBinder {
    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate jdbcTemplate;

    private final AtomicInteger kafkaRetriesMessageCount = new AtomicInteger(0);
    private final AtomicInteger kafkaRetriesMax = new AtomicInteger(0);
    private final AtomicDouble kafkaRetriesAvg = new AtomicDouble(0.0);

    @Override
    public void bindTo(@NonNull MeterRegistry meterRegistry) {
        Gauge.builder("veilarbportefolje_kafka_retries_messages_count", kafkaRetriesMessageCount, AtomicInteger::get).description("Number of failed messages").register(meterRegistry);
        Gauge.builder("veilarbportefolje_kafka_retries_max", kafkaRetriesMax, AtomicInteger::get).description("Maximal number of retries for failed messages").register(meterRegistry);
        Gauge.builder("veilarbportefolje_kafka_retries_avg", kafkaRetriesAvg, AtomicDouble::get).description("Average number of retries for failed messages").register(meterRegistry);
    }

    @Scheduled(cron = "0 */10 * * * ?")
    public void oppdaterMetrikk() {
        try {
            List<Integer> retries = this.jdbcTemplate.query("SELECT retries FROM KAFKA_CONSUMER_RECORD WHERE retries > 0", (rs, rowNum) -> rs.getInt("retries"));
            kafkaRetriesMessageCount.set(retries.size());
            kafkaRetriesMax.set(retries.stream().mapToInt(v -> v).max().orElse(0));
            kafkaRetriesAvg.set(retries.stream().mapToInt(v -> v).average().orElse(0.0));
        }
        catch (Exception e){
            kafkaRetriesMessageCount.set(0);
            kafkaRetriesMax.set(0);
            kafkaRetriesAvg.set(0);
        }
    }
}
