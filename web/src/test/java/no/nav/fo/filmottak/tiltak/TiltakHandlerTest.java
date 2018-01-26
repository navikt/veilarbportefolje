package no.nav.fo.filmottak.tiltak;

import no.nav.fo.domene.Brukerdata;
import no.nav.fo.domene.PersonId;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Java6Assertions.assertThat;


public class TiltakHandlerTest {
    private Timestamp TODAY = Timestamp.from(Instant.now());
    private Timestamp TOMORROW = Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS));
    private Timestamp DAY_BEFORE_YESTERDAY = Timestamp.from(Instant.now().minus(2, ChronoUnit.DAYS));

    private Timestamp FAR_IN_THE_FUTURE = Timestamp.from(Instant.now().plus(200, ChronoUnit.DAYS));
    private Timestamp LONGTIMEAGO = Timestamp.from(Instant.now().minus(20, ChronoUnit.DAYS));
    private Timestamp SOMETIMEAGO = Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS));

    @Test
    public void skalOppdatereUtlopsdato() {
        Map<PersonId, TiltakOppdateringer> utlopsdatoMap = new HashMap<>();
        utlopsdatoMap.put(PersonId.of("personid"), TiltakOppdateringer
                .builder()
                .nyesteUtlopteAktivitet(SOMETIMEAGO)
                .build()
        );


        Brukerdata brukerdata = new Brukerdata().setPersonid("personid").setNyesteUtlopteAktivitet(LONGTIMEAGO);

        Brukerdata afterUpdate = TiltakHandler.oppdaterBrukerDataOmNodvendig(brukerdata, utlopsdatoMap);
        assertThat(afterUpdate.getNyesteUtlopteAktivitet()).isEqualTo(SOMETIMEAGO);
    }

    @Test
    public void skalIkkeOppdatereUtlopsdato() {
        Map<PersonId, TiltakOppdateringer> utlopsdatoMap = new HashMap<>();
        utlopsdatoMap.put(PersonId.of("personid"), TiltakOppdateringer
                .builder()
                .nyesteUtlopteAktivitet(LONGTIMEAGO)
                .build()
        );
        Brukerdata brukerdata = new Brukerdata().setPersonid("personid").setNyesteUtlopteAktivitet(SOMETIMEAGO);

        Brukerdata afterUpdate = TiltakHandler.oppdaterBrukerDataOmNodvendig(brukerdata, utlopsdatoMap);
        assertThat(afterUpdate.getNyesteUtlopteAktivitet()).isEqualTo(SOMETIMEAGO);
    }

    @Test
    public void skalOppdatereDatoerNaarNullFraBrukerdata() {

        Map<PersonId, TiltakOppdateringer> utlopsdatoMap = new HashMap<>();
        utlopsdatoMap.put(PersonId.of("personid"), TiltakOppdateringer
                .builder()
                .nyesteUtlopteAktivitet(SOMETIMEAGO)
                .aktivitetStart(LONGTIMEAGO)
                .nesteAktivitetStart(SOMETIMEAGO)
                .forrigeAktivitetStart(SOMETIMEAGO)
                .build()
        );


        Brukerdata brukerdata = new Brukerdata().setPersonid("personid");

        Brukerdata afterUpdate = TiltakHandler.oppdaterBrukerDataOmNodvendig(brukerdata, utlopsdatoMap);
        assertThat(afterUpdate.getNyesteUtlopteAktivitet()).isEqualTo(SOMETIMEAGO);
        assertThat(afterUpdate.getAktivitetStart()).isEqualTo(LONGTIMEAGO);
        assertThat(afterUpdate.getNesteAktivitetStart()).isEqualTo(SOMETIMEAGO);
        assertThat(afterUpdate.getForrigeAktivitetStart()).isEqualTo(SOMETIMEAGO);
    }

    @Test
    public void skalIkkeOppdatereAktivitetDatoerNaarNullFraTiltak() {
        Map<PersonId, TiltakOppdateringer> utlopsdatoMap = new HashMap<>();
        utlopsdatoMap.put(PersonId.of("personid"), TiltakOppdateringer
                .builder()
                .build()
        );

        Brukerdata brukerdata = new Brukerdata()
                .setPersonid("personid")
                .setNyesteUtlopteAktivitet(SOMETIMEAGO)
                .setAktivitetStart(LONGTIMEAGO)
                .setNesteAktivitetStart(SOMETIMEAGO)
                .setForrigeAktivitetStart(SOMETIMEAGO);

        Brukerdata afterUpdate = TiltakHandler.oppdaterBrukerDataOmNodvendig(brukerdata, utlopsdatoMap);
        assertThat(afterUpdate.getNyesteUtlopteAktivitet()).isEqualTo(SOMETIMEAGO);
        assertThat(afterUpdate.getAktivitetStart()).isEqualTo(LONGTIMEAGO);
        assertThat(afterUpdate.getNesteAktivitetStart()).isEqualTo(SOMETIMEAGO);
        assertThat(afterUpdate.getForrigeAktivitetStart()).isEqualTo(SOMETIMEAGO);
    }

    @Test
    public void skalOppdatereAktivitetStart() {
        Timestamp today = Timestamp.from(Instant.now());
        Timestamp yesterday = Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS));
        Timestamp tomorrow = Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS));

        Map<PersonId, TiltakOppdateringer> utlopsdatoMap = new HashMap<>();
        utlopsdatoMap.put(
                PersonId.of("personid"),
                TiltakOppdateringer.builder()
                        .aktivitetStart(today)
                        .nesteAktivitetStart(tomorrow)
                        .forrigeAktivitetStart(yesterday)
                        .build()
        );


        Brukerdata brukerdata = new Brukerdata()
                .setPersonid("personid")
                .setAktivitetStart(tomorrow)
                .setNesteAktivitetStart(tomorrow)
                .setForrigeAktivitetStart(DAY_BEFORE_YESTERDAY);

        Brukerdata afterUpdate = TiltakHandler.oppdaterBrukerDataOmNodvendig(brukerdata, utlopsdatoMap);
        assertThat(afterUpdate.getAktivitetStart()).isEqualTo(today);
        assertThat(afterUpdate.getNesteAktivitetStart()).isEqualTo(tomorrow);
        assertThat(afterUpdate.getForrigeAktivitetStart()).isEqualTo(yesterday);
    }

    @Test
    public void skalOppdatereNesteAktivitetStartUtFraNyesteAvDeGamleAktivitetStart() {
        Map<PersonId, TiltakOppdateringer> utlopsdatoMap = new HashMap<>();
        utlopsdatoMap.put(
                PersonId.of("personid"),
                TiltakOppdateringer.builder()
                        .aktivitetStart(TODAY)
                        .nesteAktivitetStart(FAR_IN_THE_FUTURE)
                        .build()
        );

        Brukerdata brukerdata = new Brukerdata()
                .setPersonid("personid")
                .setAktivitetStart(TOMORROW)
                .setNesteAktivitetStart(FAR_IN_THE_FUTURE);

        Brukerdata afterUpdate = TiltakHandler.oppdaterBrukerDataOmNodvendig(brukerdata, utlopsdatoMap);
        assertThat(afterUpdate.getAktivitetStart()).isEqualTo(TODAY);
        assertThat(afterUpdate.getNesteAktivitetStart()).isEqualTo(TOMORROW);
    }

    @Test
    public void skalIkkeFaaSammeStartOgNesteStart() {
        Map<PersonId, TiltakOppdateringer> utlopsdatoMap = new HashMap<>();
        utlopsdatoMap.put(
                PersonId.of("personid"),
                TiltakOppdateringer.builder()
                        .aktivitetStart(TODAY)
                        .nesteAktivitetStart(TODAY)
                        .build()
        );

        Brukerdata brukerdata = new Brukerdata()
                .setPersonid("personid")
                .setAktivitetStart(TOMORROW)
                .setNesteAktivitetStart(TOMORROW);

        Brukerdata afterUpdate = TiltakHandler.oppdaterBrukerDataOmNodvendig(brukerdata, utlopsdatoMap);
        assertThat(afterUpdate.getAktivitetStart()).isEqualTo(TODAY);
        assertThat(afterUpdate.getNesteAktivitetStart()).isEqualTo(TOMORROW);
    }
}