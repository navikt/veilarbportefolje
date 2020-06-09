package no.nav.pto.veilarbportefolje.registrering;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static no.nav.pto.veilarbportefolje.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class RegistreringRepositoryTest {

    private RegistreringRepository registreringRepository;
    private static String AKTORID = "123456789";

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

        registreringRepository.upsertBrukerRegistrering(event);

        Optional<ArbeidssokerRegistrertEvent> registrering = registreringRepository.hentBrukerRegistrering(AktoerId.of(AKTORID));

        assertThat(registrering.isPresent());
        assertThat(registrering.orElseThrow(IllegalStateException::new)).isEqualTo(event);
    }

    @Test
    public void skallOppdatereBrukerSituasjon() {
        ArbeidssokerRegistrertEvent event1 = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Permittert")
                .setRegistreringOpprettet(ZonedDateTime.of(LocalDateTime.now().minusDays(4), ZoneId.systemDefault()).format(ISO_ZONED_DATE_TIME))
                .build();

        registreringRepository.upsertBrukerRegistrering(event1);

        ArbeidssokerRegistrertEvent event2 = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Hjemmekontor")
                .setRegistreringOpprettet(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()).format(ISO_ZONED_DATE_TIME))
                .build();

        registreringRepository.upsertBrukerRegistrering(event2);

        Optional<ArbeidssokerRegistrertEvent> registrering = registreringRepository.hentBrukerRegistrering(AktoerId.of(AKTORID));

        assertThat(registrering.isPresent());
        assertThat(registrering.orElseThrow(IllegalStateException::new)).isEqualTo(event2);
    }


}
