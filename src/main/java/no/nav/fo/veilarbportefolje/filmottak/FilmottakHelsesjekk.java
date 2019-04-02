package no.nav.fo.veilarbportefolje.filmottak;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static no.nav.fo.veilarbportefolje.filmottak.FilmottakConfig.LOPENDEYTELSER_SFTP;
import static no.nav.fo.veilarbportefolje.filmottak.FilmottakFileUtils.hentFil;
import static no.nav.fo.veilarbportefolje.util.DateUtils.toLocalDateTime;

@Slf4j
public class FilmottakHelsesjekk implements Helsesjekk {
    @Override
    public void helsesjekk() throws Throwable {
        final Try<Long> lastModifiedTimeInMillis = Try.of(
                () -> hentFil(LOPENDEYTELSER_SFTP)
                        .get()
                        .getContent()
                        .getLastModifiedTime())
                .onFailure(e -> log.warn("Kunne ikke hente ut fil med ytelser via nfs"));

        LocalDateTime fileLastChanged = toLocalDateTime(new Date(lastModifiedTimeInMillis.get()));
        long hoursSinceChanged = ChronoUnit.HOURS.between(fileLastChanged, LocalDateTime.now());

        if (hoursSinceChanged > 30) {
            throw new RuntimeException(String.format("Ytelsesfilen er mer enn 30 timer gammel, sist endret for %d timer siden", hoursSinceChanged));
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(
                "Filmottak helsesjekk",
                "N/A",
                "Sjekk på om ytelsesfilen er eldre enn 30 timer",
                false
        );
    }
}
