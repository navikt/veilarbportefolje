package no.nav.pto.veilarbportefolje.opensearch;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static io.micrometer.prometheusmetrics.PrometheusConfig.DEFAULT;

@Slf4j
@Component
public class MetricsReporter {


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
