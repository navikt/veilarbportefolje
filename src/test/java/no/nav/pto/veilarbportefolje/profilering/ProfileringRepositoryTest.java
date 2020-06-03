package no.nav.pto.veilarbportefolje.profilering;

import no.nav.arbeid.soker.profilering.ArbeidssokerProfilertEvent;
import no.nav.arbeid.soker.profilering.ProfilertTil;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static no.nav.pto.veilarbportefolje.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static org.assertj.core.api.Assertions.assertThat;

public class ProfileringRepositoryTest {

    private ProfileringRepository profileringRepository ;
    private static String AKTORID = "123456789";
    private static String AKTORID1 = "1234567892";


    @Before
    public void setup() {
        JdbcTemplate jdbc = new JdbcTemplate(setupInMemoryDatabase());
        jdbc.execute("TRUNCATE TABLE BRUKER_PROFILERING");
        this.profileringRepository = new ProfileringRepository(jdbc);
    }

    @Test
    public void skal_inserte_bruker_profilering() {
        ArbeidssokerProfilertEvent event1 = ArbeidssokerProfilertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setProfilertTil(ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING)
                .setProfileringGjennomfort(ZonedDateTime.of(LocalDateTime.now().minusDays(30), ZoneId.systemDefault()).format(ISO_ZONED_DATE_TIME))
                .build();

        profileringRepository.upsertBrukerProfilering(event1);
        assertThat(profileringRepository.hentBrukerProfilering(AktoerId.of(AKTORID))).isEqualTo(event1);

    }

    @Test
    public void skal_oppdatere_profilering() {
        ArbeidssokerProfilertEvent event1 = ArbeidssokerProfilertEvent.newBuilder()
                .setAktorid(AKTORID1)
                .setProfilertTil(ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING)
                .setProfileringGjennomfort(ZonedDateTime.of(LocalDateTime.now().minusDays(30), ZoneId.systemDefault()).format(ISO_ZONED_DATE_TIME))
                .build();

        profileringRepository.upsertBrukerProfilering(event1);

        ArbeidssokerProfilertEvent event2 = ArbeidssokerProfilertEvent.newBuilder()
                .setAktorid(AKTORID1)
                .setProfilertTil(ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING)
                .setProfileringGjennomfort(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()).format(ISO_ZONED_DATE_TIME))
                .build();

        profileringRepository.upsertBrukerProfilering(event2);
        assertThat(profileringRepository.hentBrukerProfilering(AktoerId.of(AKTORID1))).isEqualTo(event2);

    }
}
