package no.nav.pto.veilarbportefolje.elastic;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static io.micrometer.prometheus.PrometheusConfig.DEFAULT;

@Slf4j
@Component
public class MetricsReporter {

    private static final MeterRegistry prometheusMeterRegistry = new ProtectedPrometheusMeterRegistry();

    @Autowired
    public MetricsReporter(ElasticCountService elasticCountService) {
        Gauge.builder("veilarbelastic_number_of_docs", elasticCountService::getCount).register(getMeterRegistry());
    }

    public static MeterRegistry getMeterRegistry() {
        return prometheusMeterRegistry;
    }

    public static class ProtectedPrometheusMeterRegistry extends PrometheusMeterRegistry {
        public ProtectedPrometheusMeterRegistry() {
            super(DEFAULT);
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException();
        }
    }
}
