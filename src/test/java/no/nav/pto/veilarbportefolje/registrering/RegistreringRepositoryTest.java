package no.nav.pto.veilarbportefolje.registrering;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.registrering.RegistreringRepository;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static no.nav.pto.veilarbportefolje.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class RegistreringRepositoryTest {

    private RegistreringRepository registreringRepository;
    private static String AKTORID = "123456789";
    private static String AKTORID_1 = "123456789_1";

    @Before
    public void setup() {
        JdbcTemplate db = new JdbcTemplate(setupInMemoryDatabase());
        this.registreringRepository = new RegistreringRepository(db);
        registreringRepository.slettBrukerRegistrering(AktoerId.of(AKTORID));
    }

    @Test
    public void skallSetteInBrukerSituasjon() {
        ArbeidssokerRegistrertEvent event = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Permittert")
                .setRegistreringOpprettet(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()).format(ISO_ZONED_DATE_TIME))
                .build();

        registreringRepository.insertBrukerRegistrering(event);

        assertThat(registreringRepository.hentBrukerRegistrering(AktoerId.of(AKTORID))).isEqualTo(event);
    }

    @Test
    public void skallOppdatereBrukerSituasjon() {
        ArbeidssokerRegistrertEvent event1 = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Permittert")
                .setRegistreringOpprettet(ZonedDateTime.of(LocalDateTime.now().minusDays(4), ZoneId.systemDefault()).format(ISO_ZONED_DATE_TIME))
                .build();

        registreringRepository.insertBrukerRegistrering(event1);

        ArbeidssokerRegistrertEvent event2 = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Hjemmekontor")
                .setRegistreringOpprettet(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()).format(ISO_ZONED_DATE_TIME))
                .build();

        registreringRepository.uppdaterBrukerRegistring(event2);

        assertThat(registreringRepository.hentBrukerRegistrering(AktoerId.of(AKTORID))).isEqualTo(event2);
    }


}
