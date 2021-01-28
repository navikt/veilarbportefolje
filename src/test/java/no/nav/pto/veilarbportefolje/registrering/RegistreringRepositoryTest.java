package no.nav.pto.veilarbportefolje.registrering;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.arbeid.soker.registrering.UtdanningBestattSvar;
import no.nav.arbeid.soker.registrering.UtdanningGodkjentSvar;
import no.nav.arbeid.soker.registrering.UtdanningSvar;
import no.nav.common.types.identer.AktorId;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static no.nav.pto.veilarbportefolje.TestUtil.setupInMemoryDatabase;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class RegistreringRepositoryTest {

    private RegistreringRepository registreringRepository;
    private static String AKTORID = "123456789";

    @Before
    public void setup() {
        JdbcTemplate db = new JdbcTemplate(setupInMemoryDatabase());
        this.registreringRepository = new RegistreringRepository(db);
        registreringRepository.slettBrukerRegistrering(AktorId.of(AKTORID));
    }

    @Test
    public void skallSetteInBrukerSituasjon() {
        ArbeidssokerRegistrertEvent event = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Permittert")
                .setUtdanning(UtdanningSvar.GRUNNSKOLE)
                .setUtdanningBestatt(UtdanningBestattSvar.INGEN_SVAR)
                .setUtdanningGodkjent(UtdanningGodkjentSvar.JA)
                .setRegistreringOpprettet(ZonedDateTime.now(ZoneId.of("Europe/Oslo")).format(ISO_ZONED_DATE_TIME))
                .build();

        registreringRepository.upsertBrukerRegistrering(event);

        Optional<ArbeidssokerRegistrertEvent> registrering = registreringRepository.hentBrukerRegistrering(AktorId.of(AKTORID));

        assertThat(registrering.isPresent()).isTrue();
        assertThat(registrering.orElseThrow(IllegalStateException::new)).isEqualTo(event);
    }

    @Test
    public void skallOppdatereBrukerSituasjon() {
        ArbeidssokerRegistrertEvent event1 = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Permittert")
                .setUtdanning(UtdanningSvar.GRUNNSKOLE)
                .setUtdanningBestatt(UtdanningBestattSvar.INGEN_SVAR)
                .setUtdanningGodkjent(UtdanningGodkjentSvar.JA)
                .setRegistreringOpprettet(ZonedDateTime.of(LocalDateTime.now().minusDays(4), ZoneId.of("Europe/Oslo")).format(ISO_ZONED_DATE_TIME))
                .build();

        registreringRepository.upsertBrukerRegistrering(event1);

        ArbeidssokerRegistrertEvent event2 = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Hjemmekontor")
                .setUtdanning(UtdanningSvar.HOYERE_UTDANNING_1_TIL_4)
                .setUtdanningBestatt(UtdanningBestattSvar.INGEN_SVAR)
                .setUtdanningGodkjent(UtdanningGodkjentSvar.NEI)
                .setRegistreringOpprettet(ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Oslo")).format(ISO_ZONED_DATE_TIME))
                .build();

        registreringRepository.upsertBrukerRegistrering(event2);

        Optional<ArbeidssokerRegistrertEvent> registrering = registreringRepository.hentBrukerRegistrering(AktorId.of(AKTORID));

        assertThat(registrering.isPresent()).isTrue();
        assertThat(registrering.orElseThrow(IllegalStateException::new)).isEqualTo(event2);
    }

    @Test
    public void skallOppdatereUtdanning() {
        ArbeidssokerRegistrertEvent event1 = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Permittert")
                .setUtdanning(UtdanningSvar.GRUNNSKOLE)
                .setUtdanningBestatt(UtdanningBestattSvar.JA)
                .setUtdanningGodkjent(UtdanningGodkjentSvar.JA)
                .setRegistreringOpprettet(ZonedDateTime.of(LocalDateTime.now().minusDays(4), ZoneId.of("Europe/Oslo")).format(ISO_ZONED_DATE_TIME))
                .build();
        registreringRepository.upsertBrukerRegistrering(event1);

        ArbeidssokerRegistrertEvent event2 = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Permittert")
                .setUtdanning(UtdanningSvar.HOYERE_UTDANNING_5_ELLER_MER)
                .setUtdanningBestatt(UtdanningBestattSvar.NEI)
                .setUtdanningGodkjent(UtdanningGodkjentSvar.INGEN_SVAR)
                .setRegistreringOpprettet(ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Oslo")).format(ISO_ZONED_DATE_TIME))
                .build();
        registreringRepository.upsertBrukerRegistrering(event2);

        Optional<ArbeidssokerRegistrertEvent> registrering = registreringRepository.hentBrukerRegistrering(AktorId.of(AKTORID));

        assertThat(registrering.orElseThrow(IllegalStateException::new)).isEqualTo(event2);
    }


}
