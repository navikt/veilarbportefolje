package no.nav.fo.veilarbportefolje.indeksering;

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
public class MetricsReporter {

    private UnleashService unleash;

    @Inject
    public MetricsReporter(UnleashService unleash) {
        this.unleash = unleash;

        if (isLeader()) {
            log.info("logger metrikker for antall dokumenter i elastic");
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
            scheduler.scheduleAtFixedRate(new ReportNumberOfDocuments(), 1, 1, MINUTES);
        }

    }

    class ReportNumberOfDocuments implements Runnable {

        @Override
        public void run() {
            if (unleash.isEnabled("veilarbportefolje.elasticsearch")) {
                long numberOfDocs = getNumberOfDocs();
                Event event = MetricsFactory.createEvent("veilarbelastic.numberofdocs");
                event.addFieldToReport("value", numberOfDocs);
                event.report();
            } else {
                log.info("Unleash disabled, not reporting veilarbelastic_number_of_docs");
            }
        }
    }

    @SneakyThrows
    private static long getNumberOfDocs() {
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
