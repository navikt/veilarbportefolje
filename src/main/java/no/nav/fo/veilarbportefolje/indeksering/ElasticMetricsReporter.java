package no.nav.fo.veilarbportefolje.indeksering;

import io.micrometer.core.instrument.Gauge;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.indeksering.domene.CountResponse;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.sbl.rest.RestUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.MINUTES;
import static no.nav.common.leaderelection.LeaderElection.isLeader;

@Component
@Slf4j
public class ElasticMetricsReporter {

    private UnleashService unleash;

    @Inject
    public ElasticMetricsReporter(UnleashService unleash) {
        this.unleash = unleash;

        if (isLeader()) {
            if (unleash.isEnabled("veilarbportefolje.elasticsearch")) {
                log.info("logger metrikker for antall dokumenter i elastic");
                Gauge.builder("veilarbelastic_number_of_docs", this::getNumberOfDocs)
                        .register(MetricsFactory.getMeterRegistry());
            } else {
                log.info("Unleash disabled, not reporting veilarbelastic_number_of_docs");
            }
        }
    }

    @SneakyThrows
    private long getNumberOfDocs() {
        String url = ElasticUtils.getAbsoluteUrl() + "_doc/_count";

        return RestUtils.withClient(client ->
                client
                        .target(url)
                        .request()
                        .header("Authorization", ElasticUtils.getAuthHeaderValue())
                        .get(CountResponse.class)
                        .getCount()
        );
    }
}
