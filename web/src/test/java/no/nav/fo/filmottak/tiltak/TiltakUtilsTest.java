package no.nav.fo.filmottak.tiltak;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import no.nav.fo.domene.AktivitetStatus;
import no.nav.fo.domene.PersonId;
import no.nav.fo.util.DateUtils;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Timestamp;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class TiltakUtilsTest {


    @Test
    public void skalFinneNesteUtlopsdatoForTiltak() {
        Bruker bruker = new Bruker();
        Timestamp compareTime = DateUtils.timestampFromISO8601("2005-01-01T10:10:10Z");

        bruker.getTiltaksaktivitetListe().add(tiltaksaktivitetWithTiltakTOM(1,1,2000));
        bruker.getTiltaksaktivitetListe().add(tiltaksaktivitetWithTiltakTOM(1,1,2010));

        AktivitetStatus aktivitetStatus = TiltakUtils.utledAktivitetstatusForTiltak(bruker, PersonId.of("personid"));
        assertThat(aktivitetStatus.getNesteUtlop()).isBefore(compareTime);
    }

    @Test
    public void skalFinneNesteUtlopsdatoForGruppeaktivitet() {
        Bruker bruker = new Bruker();
        Timestamp compareTime = DateUtils.timestampFromISO8601("2005-01-01T10:10:10Z");

        bruker.getGruppeaktivitetListe().add(getGruppeaktivitetWithTOM(1,1,2000));
        bruker.getGruppeaktivitetListe().add(getGruppeaktivitetWithTOM(1,1,2010));

        AktivitetStatus aktivitetStatus = TiltakUtils.utledGruppeaktivitetstatus(bruker, PersonId.of("personid"));
        assertThat(aktivitetStatus.getNesteUtlop()).isBefore(compareTime);
    }

    private Gruppeaktivitet getGruppeaktivitetWithTOM(int day, int month, int year) {
        XMLGregorianCalendar calendar1 = new XMLGregorianCalendarImpl();
        calendar1.setDay(day);
        calendar1.setMonth(month);
        calendar1.setYear(year);

        Gruppeaktivitet gruppeaktivitet =  new Gruppeaktivitet();
        Moeteplan moeteplan = new Moeteplan();
        moeteplan.setSluttDato(calendarOf(day, month, year));
        gruppeaktivitet.getMoeteplanListe().add(moeteplan);

        return gruppeaktivitet;
    }

    private Tiltaksaktivitet tiltaksaktivitetWithTiltakTOM(int day, int month, int year) {
        XMLGregorianCalendar calendar = calendarOf(day, month, year);
        Periode periode =  new Periode();
        periode.setTom(calendar);
        Tiltaksaktivitet t = new Tiltaksaktivitet();
        t.setDeltakelsePeriode(periode);
        return t;
    }

    private XMLGregorianCalendar calendarOf(int day, int month, int year) {
        XMLGregorianCalendar calendar = new XMLGregorianCalendarImpl();
        calendar.setDay(day);
        calendar.setMonth(month);
        calendar.setYear(year);

        return calendar;

    }
}