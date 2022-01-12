package no.nav.pto.veilarbportefolje.elastic;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static io.micrometer.prometheus.PrometheusConfig.DEFAULT;
import static java.util.Arrays.asList;

@Component
@Slf4j
public class MetricsReporter {

    private ElasticIndexer elasticIndexer;
    private static MeterRegistry prometheusMeterRegistry = new ProtectedPrometheusMeterRegistry();

    public MetricsReporter(ElasticIndexer elasticIndexer) {
        this.elasticIndexer = elasticIndexer;

        Gauge.builder("veilarbelastic_number_of_docs", ElasticUtils::getCount).register(getMeterRegistry());
    }

    static LocalDateTime hentIndekseringsdato(String indeksNavn) {
        String[] split = indeksNavn.split("_");
        String klokkeslett = asList(split).get(split.length - 1);
        String dato = asList(split).get(split.length - 2);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        return LocalDateTime.parse(dato + "_" + klokkeslett, formatter);
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
