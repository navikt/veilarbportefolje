package no.nav.pto.veilarbportefolje.elastic;

import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.leaderelection.LeaderElection;
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

@Component
@Slf4j
public class MetricsReporter {

    private ElasticIndexer elasticIndexer;

    @Inject
    public MetricsReporter(ElasticIndexer elasticIndexer) {
        this.elasticIndexer = elasticIndexer;

        Gauge.builder("veilarbelastic_number_of_docs", ElasticUtils::getCount).register(getMeterRegistry());
        Gauge.builder("portefolje_indeks_sist_opprettet", this::sjekkIndeksSistOpprettet).register(getMeterRegistry());
        Gauge.builder("portefolje_arena_fil_ytelser_sist_oppdatert", MetricsReporter::sjekkArenaYtelserSistOppdatert).register(getMeterRegistry());
        Gauge.builder("portefolje_arena_fil_aktiviteter_sist_oppdatert", MetricsReporter::sjekkArenaAktiviteterSistOppdatert).register(getMeterRegistry());
    }

    public static long sjekkArenaYtelserSistOppdatert() {
        Long millis = getLastModifiedTimeInMillis(LOPENDEYTELSER_SFTP).getOrElseThrow(() -> new RuntimeException());
        return hoursSinceLastChanged(LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()));
    }

    public static long sjekkArenaAktiviteterSistOppdatert() {
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
