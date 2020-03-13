package no.nav.pto.veilarbportefolje.elastic;

import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.arenafiler.FilmottakFileUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static java.util.Arrays.asList;
import static no.nav.metrics.MetricsFactory.getMeterRegistry;
import static no.nav.pto.veilarbportefolje.arenafiler.FilmottakConfig.AKTIVITETER_SFTP;
import static no.nav.pto.veilarbportefolje.arenafiler.FilmottakConfig.LOPENDEYTELSER_SFTP;
import static no.nav.pto.veilarbportefolje.arenafiler.FilmottakFileUtils.getLastModifiedTimeInMillis;
import static no.nav.pto.veilarbportefolje.arenafiler.FilmottakFileUtils.hoursSinceLastChanged;
import static no.nav.pto.veilarbportefolje.elastic.ElasticConfig.*;

@Component
@Slf4j
public class MetricsReporter {

    private ElasticIndexer elasticIndexer;

    @Inject
    public MetricsReporter(ElasticIndexer elasticIndexer) {
        this.elasticIndexer = elasticIndexer;

        Gauge.builder("veilarbelastic_number_of_docs", () -> ElasticUtils.getCount(VEILARBELASTIC_HOSTNAME, VEILARBELASTIC_PASSWORD, VEILARBELASTIC_PASSWORD)).register(getMeterRegistry());
        Gauge.builder("veilarb_opendistro_elasticsearch_number_of_docs", () -> ElasticUtils.getCount(VEILARB_OPENDISTRO_ELASTICSEARCH_HOSTNAME, VEILARB_OPENDISTRO_ELASTICSEARCH_USERNAME, VEILARB_OPENDISTRO_ELASTICSEARCH_PASSWORD)).register(getMeterRegistry());

        Gauge.builder("portefolje_arena_fil_ytelser_sist_oppdatert", this::sjekkArenaYtelserSistOppdatert).register(getMeterRegistry());
        Gauge.builder("portefolje_arena_fil_aktiviteter_sist_oppdatert", this::sjekkArenaAktiviteterSistOppdatert).register(getMeterRegistry());
        Gauge.builder("portefolje_indeks_sist_opprettet", this::sjekkIndeksSistOpprettet).register(getMeterRegistry());
    }

    private Number sjekkArenaYtelserSistOppdatert() {
        Long millis = getLastModifiedTimeInMillis(LOPENDEYTELSER_SFTP).getOrElseThrow(() -> new RuntimeException());
        return hoursSinceLastChanged(LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()));
    }

    private Number sjekkArenaAktiviteterSistOppdatert() {
        Long millis = getLastModifiedTimeInMillis(AKTIVITETER_SFTP).getOrElseThrow(() -> new RuntimeException());
        return hoursSinceLastChanged(LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()));
    }

    private Number sjekkIndeksSistOpprettet() {
        return elasticIndexer
                .hentGammeltIndeksNavn()
                .map(MetricsReporter::hentIndekseringsdato)
                .map(FilmottakFileUtils::hoursSinceLastChanged)
                .orElse(Long.MAX_VALUE);
    }

    static LocalDateTime hentIndekseringsdato(String indeksNavn) {
        String[] split = indeksNavn.split("_");
        String klokkeslett = asList(split).get(split.length - 1);
        String dato = asList(split).get(split.length - 2);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        return LocalDateTime.parse(dato + "_" + klokkeslett, formatter);
    }
}
