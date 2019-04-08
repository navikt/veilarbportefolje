package no.nav.fo.veilarbportefolje.indeksering;

import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.metrics.MetricsFactory.getMeterRegistry;

@Component
@Slf4j
public class ElasticMetricsReporter {

    private UnleashService unleashService;

    @Inject
    public ElasticMetricsReporter(UnleashService unleash) {
        this.unleashService = unleash;

        if (!unleashService.isEnabled("veilarbportefolje.elasticsearch")) {
            log.info("Unleash disabled, not reporting veilarbelastic_number_of_docs");
            return;
        }

        log.info("logger metrikker for antall dokumenter i elastic");
        Gauge.builder("veilarbelastic_number_of_docs", ElasticUtils::getCount).register(getMeterRegistry());
    }

}
