package no.nav.fo.veilarbportefolje.filmottak;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Date;

import static no.nav.fo.veilarbportefolje.filmottak.FilmottakConfig.AKTIVITETER_SFTP;
import static no.nav.fo.veilarbportefolje.filmottak.FilmottakFileUtils.getLastModifiedTimeInMillis;
import static no.nav.fo.veilarbportefolje.filmottak.FilmottakFileUtils.hoursSinceLastChanged;
import static no.nav.fo.veilarbportefolje.util.DateUtils.toLocalDateTime;

@Slf4j
@Component
public class ArenaAktiviteterHelsesjekk implements Helsesjekk {

    UnleashService unleashService;

    @Inject
    public ArenaAktiviteterHelsesjekk(UnleashService unleashService) {
        this.unleashService = unleashService;
    }

    @Override
    public void helsesjekk() {
        final Try<Long> aktiviteterLastModifiedTimeInMillis = getLastModifiedTimeInMillis(AKTIVITETER_SFTP);

        long aktiviteterHoursSinceChanged = hoursSinceLastChanged(toLocalDateTime(new Date(aktiviteterLastModifiedTimeInMillis.get())));

        if (aktiviteterHoursSinceChanged > 30) {
            String message = String.format("Aktivitetsfilen er mer enn 30 timer gammel, sist endret for %d timer siden", aktiviteterHoursSinceChanged);

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
                "Filmottak arena-aktiviteter helsesjekk",
                "N/A",
                "Sjekker om arena-aktiviteter er eldre enn 30 timer",
                false
        );
    }
}
