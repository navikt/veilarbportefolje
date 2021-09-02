package no.nav.pto.veilarbportefolje.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.elastic.MetricsReporter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class KafkaStats implements MeterBinder {
    @Qualifier("PostgresJdbc")
    JdbcTemplate jdbcTemplate;

    private static MeterRegistry prometheusMeterRegistry = new MetricsReporter.ProtectedPrometheusMeterRegistry();

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
    }

    private List<Integer> getRetriesStats() {
        return jdbcTemplate.queryForList("SELECT " + Table.KAFKA_CONSUMER_RECORD.RETRIES + " FROM " +
                Table.KAFKA_CONSUMER_RECORD.TABLE_NAME, Integer.class);
    }
}
