package no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak;

import no.nav.pto.veilarbportefolje.domene.Brukerdata;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Java6Assertions.assertThat;


public class TiltakHandlerTest {

    private Timestamp TODAY = Timestamp.valueOf(LocalDate.now().atStartOfDay());
    private Timestamp TOMORROW = Timestamp.valueOf(LocalDate.now().plusDays(1).atStartOfDay());
    private Timestamp YESTERDAY = Timestamp.valueOf(LocalDate.now().minusDays(1).atStartOfDay());
    private Timestamp DAY_BEFORE_YESTERDAY = Timestamp.valueOf(LocalDate.now().minusDays(2).atStartOfDay());

    private Timestamp LONGTIMEAGO = Timestamp.valueOf(LocalDate.now().minusDays(20).atStartOfDay());
    private Timestamp SOMETIMEAGO = Timestamp.valueOf(LocalDate.now().minusDays(10).atStartOfDay());

    @Test
    public void skalOppdatereUtlopsdato() {
        testUtlopsdato(SOMETIMEAGO, LONGTIMEAGO, SOMETIMEAGO);
    }

    @Test
    public void skalIkkeOppdatereUtlopsdato() {
        testUtlopsdato(LONGTIMEAGO, SOMETIMEAGO, SOMETIMEAGO);
    }

    private void testUtlopsdato(Timestamp utlopsdatoFraOppdatering, Timestamp gammelUtlopsdato, Timestamp testEqualTo){
        Map<PersonId, TiltakOppdateringer> utlopsdatoMap = new HashMap<>();
        utlopsdatoMap.put(PersonId.of("personid"), TiltakOppdateringer
                .builder()
                .nyesteUtlopteAktivitet(utlopsdatoFraOppdatering)
                .build()
        );
        Brukerdata brukerdata = new Brukerdata().setPersonid("personid").setNyesteUtlopteAktivitet(gammelUtlopsdato);

        Brukerdata afterUpdate = TiltakHandler.oppdaterBrukerDataOmNodvendig(brukerdata, utlopsdatoMap);
        assertThat(afterUpdate.getNyesteUtlopteAktivitet()).isEqualTo(testEqualTo);
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
        Map<PersonId, TiltakOppdateringer> utlopsdatoMap = new HashMap<>();
        utlopsdatoMap.put(
                PersonId.of("personid"),
                TiltakOppdateringer.builder()
                        .aktivitetStart(TODAY)
                        .nesteAktivitetStart(TOMORROW)
                        .forrigeAktivitetStart(YESTERDAY)
                        .build()
        );


        Brukerdata brukerdata = new Brukerdata()
                .setPersonid("personid")
                .setAktivitetStart(TOMORROW)
                .setNesteAktivitetStart(TOMORROW)
                .setForrigeAktivitetStart(DAY_BEFORE_YESTERDAY);

        Brukerdata afterUpdate = TiltakHandler.oppdaterBrukerDataOmNodvendig(brukerdata, utlopsdatoMap);
        assertThat(afterUpdate.getAktivitetStart()).isEqualTo(TODAY);
        assertThat(afterUpdate.getNesteAktivitetStart()).isEqualTo(TOMORROW);
        assertThat(afterUpdate.getForrigeAktivitetStart()).isEqualTo(YESTERDAY);
    }

    @Test
    public void skalOppdatereNesteAktivitetStartUtFraNyesteAvDeGamleAktivitetStartSettet() {
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
