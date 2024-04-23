package no.nav.pto.veilarbportefolje.arbeidssokerprofilering;

import no.nav.arbeid.soker.profilering.ArbeidssokerProfilertEvent;
import no.nav.arbeid.soker.profilering.ProfilertTil;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arbeidssoker.v1.profilering.ArbeidssokerProfileringRepositoryV2;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static no.nav.pto.veilarbportefolje.util.DateUtils.now;
import static no.nav.pto.veilarbportefolje.util.DateUtils.nowToStr;
import static org.assertj.core.api.Assertions.assertThat;

public class ArbeidssokerProfileringRepositoryV2Test {
    private JdbcTemplate db;
    private ArbeidssokerProfileringRepositoryV2 arbeidssokerProfileringRepositoryV2;

    private static String AKTORID = "123456789";
    private static String AKTORID1 = "1234567892";

    @Before
    public void setup() {
        db = SingletonPostgresContainer.init().createJdbcTemplate();
        arbeidssokerProfileringRepositoryV2 = new ArbeidssokerProfileringRepositoryV2(db);
    }

    @Test
    public void skal_inserte_bruker_profilering() {
        ArbeidssokerProfilertEvent arbeidssokerProfilertEvent = ArbeidssokerProfilertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setProfilertTil(ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING)
                .setProfileringGjennomfort(nowToStr())
                .build();

        arbeidssokerProfileringRepositoryV2.upsertBrukerProfilering(arbeidssokerProfilertEvent);
        assertThat(arbeidssokerProfileringRepositoryV2.hentBrukerProfilering(AktorId.of(AKTORID)).get()).isEqualTo(arbeidssokerProfilertEvent);
    }

    @Test
    public void skal_oppdatere_profilering() {
        ArbeidssokerProfilertEvent event1 = ArbeidssokerProfilertEvent.newBuilder()
                .setAktorid(AKTORID1)
                .setProfilertTil(ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING)
                .setProfileringGjennomfort(now().minusDays(30).format(ISO_ZONED_DATE_TIME))
                .build();

        arbeidssokerProfileringRepositoryV2.upsertBrukerProfilering(event1);

        ArbeidssokerProfilertEvent event2 = ArbeidssokerProfilertEvent.newBuilder()
                .setAktorid(AKTORID1)
                .setProfilertTil(ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING)
                .setProfileringGjennomfort(nowToStr())
                .build();

        arbeidssokerProfileringRepositoryV2.upsertBrukerProfilering(event2);
        assertThat(arbeidssokerProfileringRepositoryV2.hentBrukerProfilering(AktorId.of(AKTORID1)).get()).isEqualTo(event2);

    }
}
