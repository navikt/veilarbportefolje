package no.nav.fo.veilarbportefolje.filmottak;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Date;

import static no.nav.fo.veilarbportefolje.filmottak.FilmottakConfig.LOPENDEYTELSER_SFTP;
import static no.nav.fo.veilarbportefolje.filmottak.FilmottakFileUtils.getLastModifiedTimeInMillis;
import static no.nav.fo.veilarbportefolje.filmottak.FilmottakFileUtils.hoursSinceLastChanged;
import static no.nav.fo.veilarbportefolje.util.DateUtils.toLocalDateTime;

@Slf4j
@Component
public class ArenaYtleserHelsesjekk implements Helsesjekk {

    private UnleashService unleashService;

    @Inject
    public ArenaYtleserHelsesjekk(UnleashService unleashService) {
        this.unleashService = unleashService;
    }

    @Override
    public void helsesjekk() {
        final Try<Long> ytelserLastModifiedTimeInMillis = getLastModifiedTimeInMillis(LOPENDEYTELSER_SFTP);

        long ytelserHoursSinceChanged = hoursSinceLastChanged(toLocalDateTime(new Date(ytelserLastModifiedTimeInMillis.get())));

        if (ytelserHoursSinceChanged > 30) {
            String message = String.format("Ytelsesfilen er mer enn 30 timer gammel, sist endret for %d timer siden", ytelserHoursSinceChanged);
            if (unleashService.isEnabled("portefolje.arena_filer.selftest")) {
                throw new RuntimeException(message);
            } else {
                log.error(message);
            }
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
