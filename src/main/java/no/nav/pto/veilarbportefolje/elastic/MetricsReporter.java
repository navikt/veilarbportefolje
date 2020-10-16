package no.nav.pto.veilarbportefolje.elastic;

import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.Arrays.asList;
import static no.nav.pto.veilarbportefolje.arenafiler.FilmottakFileUtils.hoursSinceLastChanged;

@Component
public class MetricsReporter {

    private final ElasticIndexer elasticIndexer;
    private final MetricsClient metricsClient;

    public MetricsReporter(ElasticIndexer elasticIndexer, MetricsClient metricsClient) {
        this.elasticIndexer = elasticIndexer;
        this.metricsClient = metricsClient;

    }

    //Hourly
    @Scheduled(cron = "0 0 * * * *")
    private void sjekkIndeksSistOpprettet() {
        String indeksNavn = elasticIndexer.hentGammeltIndeksNavn().orElseThrow(IllegalStateException::new);
        LocalDateTime tidspunktForSisteHovedIndeksering = hentIndekseringsdato(indeksNavn);
        final long timerSiden = hoursSinceLastChanged(tidspunktForSisteHovedIndeksering);
        Event event = new Event("portefolje.sist.opprettet");
        event.addFieldToReport("timerSiden", timerSiden);
        metricsClient.report(event);
    }

    static LocalDateTime hentIndekseringsdato(String indeksNavn) {
        String[] split = indeksNavn.split("_");
        String klokkeslett = asList(split).get(split.length - 1);
        String dato = asList(split).get(split.length - 2);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        return LocalDateTime.parse(dato + "_" + klokkeslett, formatter);
    }
}
