package no.nav.fo.filmottak.tiltak;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import no.nav.fo.domene.AktivitetStatus;
import no.nav.fo.domene.PersonId;
import no.nav.fo.domene.aktivitet.UtdanningaktivitetTyper;
import no.nav.fo.util.AktivitetUtils;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class TiltakUtilsTest {


    @Test
    public void skalFinneNesteUtlopsdatoForTiltak() {
        Bruker bruker = new Bruker();
        Timestamp past = Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS));
        Timestamp compareTime = Timestamp.from(Instant.now());
        Timestamp future = Timestamp.from(Instant.now().plus(10, ChronoUnit.DAYS));

        bruker.getTiltaksaktivitetListe().add(tiltaksaktivitetWithTiltakTOM(past));
        bruker.getTiltaksaktivitetListe().add(tiltaksaktivitetWithTiltakTOM(future));

        AktivitetStatus aktivitetStatus = TiltakUtils.utledAktivitetstatusForTiltak(bruker, PersonId.of("personid"), AktivitetUtils.parseDato("1999-01-01"));
        assertThat(aktivitetStatus.getNesteUtlop()).isAfter(compareTime);
    }

    @Test
    public void skalFinneNesteUtlopsdatoForGruppeaktivitet() {
        Bruker bruker = new Bruker();
        Timestamp past = Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS));
        Timestamp compareTime = Timestamp.from(Instant.now());
        Timestamp future = Timestamp.from(Instant.now().plus(10, ChronoUnit.DAYS));

        bruker.getGruppeaktivitetListe().add(getGruppeaktivitetWithTOM(past));
        bruker.getGruppeaktivitetListe().add(getGruppeaktivitetWithTOM(future));

        AktivitetStatus aktivitetStatus = TiltakUtils.utledGruppeaktivitetstatus(bruker, PersonId.of("personid"), AktivitetUtils.parseDato("1999-01-01"));
        assertThat(aktivitetStatus.getNesteUtlop()).isAfter(compareTime);
    }

    @Test
    public void skalFinneNesteUtlopsdatoForUtdanningsaktivitet() {
        Bruker bruker = new Bruker();
        Timestamp past = Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS));
        Timestamp compareTime = Timestamp.from(Instant.now());
        Timestamp future = Timestamp.from(Instant.now().plus(10, ChronoUnit.DAYS));

        bruker.getUtdanningsaktivitetListe().add(getUtdanningsaktivitetWithTOM(past));
        bruker.getUtdanningsaktivitetListe().add(getUtdanningsaktivitetWithTOM(future));

        AktivitetStatus aktivitetStatus = TiltakUtils.utledUtdanningsaktivitetstatus(bruker, PersonId.of("personid"), AktivitetUtils.parseDato("1999-01-01"));
        assertThat(aktivitetStatus.getNesteUtlop()).isAfter(compareTime);
    }

    @Test
    public void skalFinneNyesteUtlopteAktivitetForBruker() {
        Bruker bruker = new Bruker();

        Timestamp veryLongTimeago = Timestamp.from(Instant.now().minus(100, ChronoUnit.DAYS));
        Timestamp longTimeAgo = Timestamp.from(Instant.now().minus(70, ChronoUnit.DAYS));
        Timestamp someTimeAgo = Timestamp.from(Instant.now().minus(50, ChronoUnit.DAYS));
        Timestamp comparingTime = Timestamp.from(Instant.now().minus(60, ChronoUnit.DAYS));
        Timestamp future = Timestamp.from(Instant.now().plus(10, ChronoUnit.DAYS));

        bruker.getUtdanningsaktivitetListe().add(getUtdanningsaktivitetWithTOM(veryLongTimeago));
        bruker.getGruppeaktivitetListe().add(getGruppeaktivitetWithTOM(longTimeAgo));
        bruker.getTiltaksaktivitetListe().add(tiltaksaktivitetWithTiltakTOM(someTimeAgo));
        bruker.getTiltaksaktivitetListe().add(tiltaksaktivitetWithTiltakTOM(future));

        Timestamp nyesteUtlopsdato = TiltakUtils.finnNysteUtlopsdatoForBruker(bruker).get();
        assertThat(nyesteUtlopsdato).isAfter(comparingTime);
        assertThat(future).isAfter(comparingTime);
    }

    @Test
    public void skalIkkeTryneOmDetIkkeFinnesDatoer() {
        Optional<Timestamp> nyesteUtlopsdato = TiltakUtils.finnNysteUtlopsdatoForBruker(new Bruker());
        assertThat(nyesteUtlopsdato.isPresent()).isFalse();
    }

    private Utdanningsaktivitet getUtdanningsaktivitetWithTOM(Timestamp timestamp) {
        LocalDate localDate = timestamp.toLocalDateTime().toLocalDate();
        XMLGregorianCalendar calendar = calendarOf(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());

        Utdanningsaktivitet utdanningsaktivitet = new Utdanningsaktivitet();
        utdanningsaktivitet.setAktivitetstype(UtdanningaktivitetTyper.EUTD.toString());
        Periode periode = new Periode();
        periode.setTom(calendar);
        utdanningsaktivitet.setAktivitetPeriode(periode);
        return utdanningsaktivitet;
    }

    private Gruppeaktivitet getGruppeaktivitetWithTOM(Timestamp timestamp) {
        LocalDate localDate = timestamp.toLocalDateTime().toLocalDate();

        Gruppeaktivitet gruppeaktivitet =  new Gruppeaktivitet();
        Moeteplan moeteplan = new Moeteplan();
        moeteplan.setSluttDato(calendarOf(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth()));
        gruppeaktivitet.getMoeteplanListe().add(moeteplan);

        return gruppeaktivitet;
    }

    private Tiltaksaktivitet tiltaksaktivitetWithTiltakTOM(Timestamp timestamp) {
        LocalDate localDate = timestamp.toLocalDateTime().toLocalDate();
        XMLGregorianCalendar calendar = calendarOf(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
        Periode periode =  new Periode();
        periode.setTom(calendar);
        Tiltaksaktivitet t = new Tiltaksaktivitet();
        t.setDeltakelsePeriode(periode);
        return t;
    }

    private XMLGregorianCalendar calendarOf(int year, int month, int day) {
        XMLGregorianCalendar calendar = new XMLGregorianCalendarImpl();
        calendar.setDay(day);
        calendar.setMonth(month);
        calendar.setYear(year);

        return calendar;
    }


}