package no.nav.fo.veilarbportefolje.filmottak;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.springframework.stereotype.Component;

import java.util.Date;

import static no.nav.fo.veilarbportefolje.filmottak.FilmottakConfig.LOPENDEYTELSER_SFTP;
import static no.nav.fo.veilarbportefolje.filmottak.FilmottakFileUtils.getLastModifiedTimeInMillis;
import static no.nav.fo.veilarbportefolje.filmottak.FilmottakFileUtils.hoursSinceLastChanged;
import static no.nav.fo.veilarbportefolje.util.DateUtils.toLocalDateTime;

@Slf4j
@Component
public class ArenaYtleserHelsesjekk implements Helsesjekk {

    @Override
    public void helsesjekk() throws Throwable {
        final Try<Long> ytelserLastModifiedTimeInMillis = getLastModifiedTimeInMillis(LOPENDEYTELSER_SFTP);

        long ytelserHoursSinceChanged = hoursSinceLastChanged(toLocalDateTime(new Date(ytelserLastModifiedTimeInMillis.get())));

        if (ytelserHoursSinceChanged > 30) {
            throw new RuntimeException(String.format("Ytelsesfilen er mer enn 30 timer gammel, sist endret for %d timer siden", ytelserHoursSinceChanged));
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(
                "Filmottak arena-ytelser helsesjekk",
                "N/A",
                "Sjekker om arena-ytelser er eldre enn 30 timer",
                false
        );
    }
}
