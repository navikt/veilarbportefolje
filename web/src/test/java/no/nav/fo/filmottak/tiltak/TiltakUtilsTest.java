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

import static no.nav.fo.service.SolrServiceImpl.DATOFILTER_PROPERTY;
import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class TiltakUtilsTest {


    @Test
    public void skalFinneNesteUtlopsdatoForTiltak() {
        System.setProperty(DATOFILTER_PROPERTY, "1999-01-01");
        Bruker bruker = new Bruker();
        Timestamp past = Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS));
        Timestamp compareTime = Timestamp.from(Instant.now());
        Timestamp future = Timestamp.from(Instant.now().plus(10, ChronoUnit.DAYS));

        bruker.getTiltaksaktivitetListe().add(tiltaksaktivitetWithTiltakTOM(past));
        bruker.getTiltaksaktivitetListe().add(tiltaksaktivitetWithTiltakTOM(future));

        AktivitetStatus aktivitetStatus = TiltakUtils.utledAktivitetstatusForTiltak(bruker, PersonId.of("personid"));
        assertThat(aktivitetStatus.getNesteUtlop()).isAfter(compareTime);
    }

    @Test
    public void skalFinneNesteUtlopsdatoForGruppeaktivitet() {
        System.setProperty(DATOFILTER_PROPERTY, "1999-01-01");
        Bruker bruker = new Bruker();
        Timestamp past = Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS));
        Timestamp compareTime = Timestamp.from(Instant.now());
        Timestamp future = Timestamp.from(Instant.now().plus(10, ChronoUnit.DAYS));

        bruker.getGruppeaktivitetListe().add(getGruppeaktivitetWithTOM(past));
        bruker.getGruppeaktivitetListe().add(getGruppeaktivitetWithTOM(future));

        AktivitetStatus aktivitetStatus = TiltakUtils.utledGruppeaktivitetstatus(bruker, PersonId.of("personid"));
        assertThat(aktivitetStatus.getNesteUtlop()).isAfter(compareTime);
    }

    @Test
    public void skalFinneNesteUtlopsdatoForUtdanningsaktivitet() {
        System.setProperty(DATOFILTER_PROPERTY, "1999-01-01");

        Bruker bruker = new Bruker();
        Timestamp past = Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS));
        Timestamp compareTime = Timestamp.from(Instant.now());
        Timestamp future = Timestamp.from(Instant.now().plus(10, ChronoUnit.DAYS));

        bruker.getUtdanningsaktivitetListe().add(getUtdanningsaktivitetWithTOM(past));
        bruker.getUtdanningsaktivitetListe().add(getUtdanningsaktivitetWithTOM(future));

        AktivitetStatus aktivitetStatus = TiltakUtils.utledUtdanningsaktivitetstatus(bruker, PersonId.of("personid"));
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

        //TODO: More tests
        Timestamp nyesteUtlopsdato = TiltakUtils.finnOppdateringForBruker(bruker).getNyesteUtlopteAktivitet();
        assertThat(nyesteUtlopsdato).isAfter(comparingTime);
        assertThat(future).isAfter(comparingTime);
    }

    @Test
    public void skalBareTaHensynTilNyesteMoteplan() {
        Bruker bruker = new Bruker();
        Timestamp past = Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS));
        Timestamp future = Timestamp.from(Instant.now().plus(10, ChronoUnit.DAYS));

        bruker.getGruppeaktivitetListe().add(getGruppeaktivitetWithTwoTOM(past,future));
        assertThat(TiltakUtils.finnOppdateringForBruker(bruker).getNyesteUtlopteAktivitet() == null).isTrue();
    }

    @Test
    public void skalIkkeTryneOmDetIkkeFinnesDatoer() {
        Timestamp nyesteUtlopsdato = TiltakUtils.finnOppdateringForBruker(new Bruker()).getNyesteUtlopteAktivitet();
        assertThat(nyesteUtlopsdato == null).isTrue();
    }

    @Test
    public void skalIkkeTaHensynTilDatoerForDatoFilter() {
        System.setProperty(DATOFILTER_PROPERTY, "2000-01-01");

        Bruker bruker = new Bruker();
        Timestamp beforeFilter = AktivitetUtils.parseDato("1999-01-01");
        bruker.getGruppeaktivitetListe().add(getGruppeaktivitetWithTOM(beforeFilter));
        assertThat(TiltakUtils.finnOppdateringForBruker(bruker).getNyesteUtlopteAktivitet() == null).isTrue();
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

    private Gruppeaktivitet getGruppeaktivitetWithTwoTOM(Timestamp t1, Timestamp t2) {
        LocalDate localDate1 = t1.toLocalDateTime().toLocalDate();
        LocalDate localDate2 = t2.toLocalDateTime().toLocalDate();

        Gruppeaktivitet gruppeaktivitet =  new Gruppeaktivitet();
        Moeteplan moeteplan1 = new Moeteplan();
        moeteplan1.setSluttDato(calendarOf(localDate1.getYear(), localDate1.getMonthValue(), localDate1.getDayOfMonth()));
        Moeteplan moeteplan2 = new Moeteplan();
        moeteplan2.setSluttDato(calendarOf(localDate2.getYear(), localDate2.getMonthValue(), localDate2.getDayOfMonth()));
        gruppeaktivitet.getMoeteplanListe().add(moeteplan1);
        gruppeaktivitet.getMoeteplanListe().add(moeteplan2);

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