package no.nav.fo.veilarbportefolje.indeksering;

import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static no.nav.metrics.MetricsFactory.getMeterRegistry;

@Component
@Slf4j
public class ElasticMetricsReporter {

    private ElasticIndexer elasticIndexer;

    @Inject
    public ElasticMetricsReporter(ElasticIndexer elasticIndexer) {
        this.elasticIndexer = elasticIndexer;

        log.info("logger metrikker for antall dokumenter i elastic");
        Gauge.builder("veilarbelastic_number_of_docs", ElasticUtils::getCount).register(getMeterRegistry());

        ScheduledFuture<?> future = new ScheduledThreadPoolExecutor(2).scheduleAtFixedRate(this::sjekkAlderPaaIndeks, 10, 10, MINUTES);

    }

    private void sjekkAlderPaaIndeks() {
        String indeksNavn = elasticIndexer.hentGammeltIndeksNavn().orElseThrow(IllegalStateException::new);
        Event event = MetricsFactory.createEvent("portefolje.indeks.gammel");
        if (erOver26TimerGammel(indeksNavn)) {
            event.report();
        }
    }

    static boolean erOver26TimerGammel(String indeksNavn) {
        String[] split = indeksNavn.split("_");
        String klokkeslett = asList(split).get(split.length - 1);
        String dato = asList(split).get(split.length - 2);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        LocalDateTime indeksDato = LocalDateTime.parse(dato + "_" + klokkeslett, formatter);

        return indeksDato.isBefore(LocalDateTime.now().minusHours(26));
    }
}
