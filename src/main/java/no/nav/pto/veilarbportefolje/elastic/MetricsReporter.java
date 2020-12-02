package no.nav.pto.veilarbportefolje.elastic;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static io.micrometer.prometheus.PrometheusConfig.DEFAULT;
import static java.util.Arrays.asList;
import static no.nav.pto.veilarbportefolje.arenafiler.FilmottakFileUtils.hoursSinceLastChanged;

@Slf4j
@Component
public class MetricsReporter {

    private static final MeterRegistry prometheusMeterRegistry = new ProtectedPrometheusMeterRegistry();

    private final ElasticIndexer elasticIndexer;

    @Autowired
    public MetricsReporter(ElasticIndexer elasticIndexer, ElasticCountService elasticCountService) {
        this.elasticIndexer = elasticIndexer;

        Gauge.builder("veilarbelastic_number_of_docs", elasticCountService::getCount).register(getMeterRegistry());
        Gauge.builder("portefolje_indeks_sist_opprettet", this::sjekkIndeksSistOpprettet).register(getMeterRegistry());
    }

    private Number sjekkIndeksSistOpprettet() {
        String indeksNavn = elasticIndexer.hentGammeltIndeksNavn().orElseThrow(IllegalStateException::new);
        LocalDateTime tidspunktForSisteHovedIndeksering = hentIndekseringsdato(indeksNavn);
        return hoursSinceLastChanged(tidspunktForSisteHovedIndeksering);
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
