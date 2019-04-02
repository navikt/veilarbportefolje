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
import static no.nav.common.leaderelection.LeaderElection.isNotLeader;
import static no.nav.metrics.MetricsFactory.getMeterRegistry;

@Component
@Slf4j
public class ElasticMetricsReporter {

    private UnleashService unleashService;

    @Inject
    public ElasticMetricsReporter(UnleashService unleash) {
        this.unleashService = unleash;

        if (isNotLeader()) {
            return;
        }

        if (!unleashService.isEnabled("veilarbportefolje.elasticsearch")) {
            log.info("Unleash disabled, not reporting veilarbelastic_number_of_docs");
            return;
        }

        log.info("logger metrikker for antall dokumenter i elastic");
        Gauge.builder("veilarbelastic_number_of_docs", this::getCount).register(getMeterRegistry());
    }

    private long getCount() {
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
