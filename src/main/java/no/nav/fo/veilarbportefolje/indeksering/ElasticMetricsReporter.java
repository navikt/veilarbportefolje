package no.nav.fo.veilarbportefolje.indeksering;

import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.metrics.MetricsFactory.getMeterRegistry;

@Component
@Slf4j
public class ElasticMetricsReporter {

    @Inject
    public ElasticMetricsReporter() {
        log.info("logger metrikker for antall dokumenter i elastic");
        Gauge.builder("veilarbelastic_number_of_docs", ElasticUtils::getCount).register(getMeterRegistry());
    }
}
