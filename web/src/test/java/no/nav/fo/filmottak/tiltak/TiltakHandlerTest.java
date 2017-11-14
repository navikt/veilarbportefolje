package no.nav.fo.filmottak.tiltak;

import no.nav.fo.domene.Brukerdata;
import no.nav.fo.domene.PersonId;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Java6Assertions.assertThat;


public class TiltakHandlerTest {


    @Test
    public void skalOppdatereUtlopsdato() {
       Timestamp longTimeAgo = Timestamp.from(Instant.now().minus(20, ChronoUnit.DAYS));
       Timestamp someTimeAgo = Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS));

        Map<PersonId, Optional<Timestamp>> utlopsdatoMap = new HashMap<>();
        utlopsdatoMap.put(PersonId.of("personid"), Optional.of(someTimeAgo));

        Brukerdata brukerdata = new Brukerdata().setPersonid("personid").setNyesteUtlopteAktivitet(longTimeAgo);

        Brukerdata afterUpdate = TiltakHandler.oppdaterUtlopsdatoOmNodvendig(brukerdata, utlopsdatoMap);
        assertThat(afterUpdate.getNyesteUtlopteAktivitet()).isEqualTo(someTimeAgo);
    }

    @Test
    public void skalIkkeOppdatereUtlopsdato() {
        Timestamp longTimeAgo = Timestamp.from(Instant.now().minus(30, ChronoUnit.DAYS));
        Timestamp someTimeAgo = Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS));

        Map<PersonId, Optional<Timestamp>> utlopsdatoMap = new HashMap<>();
        utlopsdatoMap.put(PersonId.of("personid"), Optional.of(longTimeAgo));

        Brukerdata brukerdata = new Brukerdata().setPersonid("personid").setNyesteUtlopteAktivitet(someTimeAgo);

        Brukerdata afterUpdate = TiltakHandler.oppdaterUtlopsdatoOmNodvendig(brukerdata, utlopsdatoMap);
        assertThat(afterUpdate.getNyesteUtlopteAktivitet()).isEqualTo(someTimeAgo);
    }

    @Test
    public void skalOppdatereUtlopsdatoNaarNullFraBrukerdata() {
        Timestamp past = Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS));

        Map<PersonId, Optional<Timestamp>> utlopsdatoMap = new HashMap<>();
        utlopsdatoMap.put(PersonId.of("personid"), Optional.of(past));

        Brukerdata brukerdata = new Brukerdata().setPersonid("personid");

        Brukerdata afterUpdate = TiltakHandler.oppdaterUtlopsdatoOmNodvendig(brukerdata, utlopsdatoMap);
        assertThat(afterUpdate.getNyesteUtlopteAktivitet()).isEqualTo(past);
    }

    @Test
    public void skalOppdatereUtlopsdatoNaarNullFraTiltak() {
        Timestamp past = Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS));

        Map<PersonId, Optional<Timestamp>> utlopsdatoMap = new HashMap<>();

        Brukerdata brukerdata = new Brukerdata().setPersonid("personid").setNyesteUtlopteAktivitet(past);

        Brukerdata afterUpdate = TiltakHandler.oppdaterUtlopsdatoOmNodvendig(brukerdata, utlopsdatoMap);
        assertThat(afterUpdate.getNyesteUtlopteAktivitet()).isEqualTo(past);
    }
}