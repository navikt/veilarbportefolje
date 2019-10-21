package no.nav.fo.veilarbportefolje.indeksering;

import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static java.util.Arrays.asList;
import static no.nav.fo.veilarbportefolje.filmottak.FilmottakConfig.AKTIVITETER_SFTP;
import static no.nav.fo.veilarbportefolje.filmottak.FilmottakConfig.LOPENDEYTELSER_SFTP;
import static no.nav.fo.veilarbportefolje.filmottak.FilmottakFileUtils.getLastModifiedTimeInMillis;
import static no.nav.fo.veilarbportefolje.filmottak.FilmottakFileUtils.hoursSinceLastChanged;
import static no.nav.metrics.MetricsFactory.getMeterRegistry;

@Component
@Slf4j
public class MetricsReporter {

    private ElasticIndexer elasticIndexer;

    @Inject
    public MetricsReporter(ElasticIndexer elasticIndexer) {
        this.elasticIndexer = elasticIndexer;

        Gauge.builder("veilarbelastic_number_of_docs", ElasticUtils::getCount).register(getMeterRegistry());
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
}
