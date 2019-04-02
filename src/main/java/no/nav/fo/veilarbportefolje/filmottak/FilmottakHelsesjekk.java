package no.nav.fo.veilarbportefolje.filmottak;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static no.nav.fo.veilarbportefolje.filmottak.FilmottakConfig.*;
import static no.nav.fo.veilarbportefolje.filmottak.FilmottakFileUtils.hentFil;
import static no.nav.fo.veilarbportefolje.util.DateUtils.toLocalDateTime;

@Slf4j
@Component
public class FilmottakHelsesjekk implements Helsesjekk {

    @Override
    public void helsesjekk() throws Throwable {
        final Try<Long> ytelserLastModifiedTimeInMillis = getLastModifiedTimeInMillis(LOPENDEYTELSER_SFTP);
        final Try<Long> aktiviteterLastModifiedTimeInMillis = getLastModifiedTimeInMillis(AKTIVITETER_SFTP);

        long ytelserHoursSinceChanged = hoursSinceLastChanged(toLocalDateTime(new Date(ytelserLastModifiedTimeInMillis.get())));
        long aktiviteterHoursSinceChanged = hoursSinceLastChanged(toLocalDateTime(new Date(aktiviteterLastModifiedTimeInMillis.get())));

        if (ytelserHoursSinceChanged > 30) {
            throw new RuntimeException(String.format("Ytelsesfilen er mer enn 30 timer gammel, sist endret for %d timer siden", ytelserHoursSinceChanged));
        } else if (aktiviteterHoursSinceChanged > 30) {
            throw new RuntimeException(String.format("Aktivitetsfilen er mer enn 30 timer gammel, sist endret for %d timer siden", aktiviteterHoursSinceChanged));
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(
                "Filmottak helsesjekk",
                "N/A",
                "Sjekker om filene fra arena er eldre enn 30 timer",
                false
        );
    }

    private Try<Long> getLastModifiedTimeInMillis(SftpConfig sftpConfig) {
        return Try.of(
                () -> hentFil(sftpConfig)
                        .get()
                        .getContent()
                        .getLastModifiedTime())
                .onFailure(e -> log.warn(String.format("Kunne ikke hente ut fil via nfs: %s", sftpConfig.getUrl())));
    }

    private long hoursSinceLastChanged(LocalDateTime lastChanged) {
        return ChronoUnit.HOURS.between(lastChanged, LocalDateTime.now());
    }
}
