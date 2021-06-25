package no.nav.pto.veilarbportefolje.profilering;

import no.nav.arbeid.soker.profilering.ArbeidssokerProfilertEvent;
import no.nav.arbeid.soker.profilering.ProfilertTil;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;

public class ProfileringRepositoryV2Test {
    private JdbcTemplate db;
    private ProfileringRepositoryV2 profileringRepositoryV2;

    private static String AKTORID = "123456789";
    private static String AKTORID1 = "1234567892";

    @Before
    public void setup() {
        db = SingletonPostgresContainer.init().createJdbcTemplate();
        profileringRepositoryV2 = new ProfileringRepositoryV2(db);
    }

    @Test
    public void skal_inserte_bruker_profilering() {
        ArbeidssokerProfilertEvent arbeidssokerProfilertEvent = ArbeidssokerProfilertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setProfilertTil(ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING)
                .setProfileringGjennomfort(ZonedDateTime.now(ZoneId.of("Europe/Oslo")).format(ISO_ZONED_DATE_TIME))
                .build();

        profileringRepositoryV2.upsertBrukerProfilering(arbeidssokerProfilertEvent);
        assertThat(profileringRepositoryV2.hentBrukerProfilering(AktorId.of(AKTORID)).get()).isEqualTo(arbeidssokerProfilertEvent);
    }

    @Test
    public void skal_oppdatere_profilering() {
        ArbeidssokerProfilertEvent event1 = ArbeidssokerProfilertEvent.newBuilder()
                .setAktorid(AKTORID1)
                .setProfilertTil(ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING)
                .setProfileringGjennomfort(ZonedDateTime.of(LocalDateTime.now().minusDays(30), ZoneId.of("Europe/Oslo")).format(ISO_ZONED_DATE_TIME))
                .build();

        profileringRepositoryV2.upsertBrukerProfilering(event1);

        ArbeidssokerProfilertEvent event2 = ArbeidssokerProfilertEvent.newBuilder()
                .setAktorid(AKTORID1)
                .setProfilertTil(ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING)
                .setProfileringGjennomfort(ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Oslo")).format(ISO_ZONED_DATE_TIME))
                .build();

        profileringRepositoryV2.upsertBrukerProfilering(event2);
        assertThat(profileringRepositoryV2.hentBrukerProfilering(AktorId.of(AKTORID1)).get()).isEqualTo(event2);

    }
}
