package no.nav.fo.veilarbportefolje.filmottak.tiltak;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import no.nav.fo.veilarbportefolje.domene.AktivitetStatus;
import no.nav.fo.veilarbportefolje.domene.PersonId;
import no.nav.fo.veilarbportefolje.domene.aktivitet.UtdanningaktivitetTyper;
import no.nav.fo.veilarbportefolje.util.AktivitetUtils;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static junit.framework.TestCase.assertTrue;
import static no.nav.fo.veilarbportefolje.service.SolrServiceImpl.DATOFILTER_PROPERTY;
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

        bruker.getTiltaksaktivitetListe().add(tiltaksaktivitetWithTiltakTOM(past, past));
        bruker.getTiltaksaktivitetListe().add(tiltaksaktivitetWithTiltakTOM(past, future));

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

        bruker.getGruppeaktivitetListe().add(getGruppeaktivitet(past, past));
        bruker.getGruppeaktivitetListe().add(getGruppeaktivitet(past, future));

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

        bruker.getUtdanningsaktivitetListe().add(getUtdanningsaktivitet(past, past));
        bruker.getUtdanningsaktivitetListe().add(getUtdanningsaktivitet(future, future));

        AktivitetStatus aktivitetStatus = TiltakUtils.utledUtdanningsaktivitetstatus(bruker, PersonId.of("personid"));
        assertThat(aktivitetStatus.getNesteUtlop()).isAfter(compareTime);
    }

    @Test
    public void skalFinneNyesteTiltaksDatoerForBruker() {
        Bruker bruker = new Bruker();

        Timestamp ekstraVeryLongTimeago = Timestamp.from(Instant.now().minus(200, ChronoUnit.DAYS));
        Timestamp veryLongTimeago = Timestamp.from(Instant.now().minus(100, ChronoUnit.DAYS));
        Timestamp longTimeAgo = Timestamp.from(Instant.now().minus(70, ChronoUnit.DAYS));
        Timestamp someTimeAgo = Timestamp.from(Instant.now().minus(50, ChronoUnit.DAYS));
        Timestamp future = Timestamp.from(Instant.now().plus(10, ChronoUnit.DAYS));
        Timestamp fartherInTheFuture = Timestamp.from(Instant.now().plus(20, ChronoUnit.DAYS));

        bruker.getUtdanningsaktivitetListe().add(getUtdanningsaktivitet(ekstraVeryLongTimeago, veryLongTimeago));
        bruker.getGruppeaktivitetListe().add(getGruppeaktivitet(veryLongTimeago, longTimeAgo));
        bruker.getTiltaksaktivitetListe().add(tiltaksaktivitetWithTiltakTOM(someTimeAgo, someTimeAgo));
        bruker.getTiltaksaktivitetListe().add(tiltaksaktivitetWithTiltakTOM(someTimeAgo, future));
        bruker.getTiltaksaktivitetListe().add(tiltaksaktivitetWithTiltakTOM(future, future));
        bruker.getTiltaksaktivitetListe().add(tiltaksaktivitetWithTiltakTOM(future, future));
        bruker.getTiltaksaktivitetListe().add(tiltaksaktivitetWithTiltakTOM(fartherInTheFuture, fartherInTheFuture));

        TiltakOppdateringer tiltakOppdateringer = TiltakUtils.finnOppdateringForBruker(bruker);
        assertTrue(calendarEqual(tiltakOppdateringer.getNyesteUtlopteAktivitet(), someTimeAgo));
        assertTrue(calendarEqual(tiltakOppdateringer.getAktivitetStart(), future));
        assertTrue(calendarEqual(tiltakOppdateringer.getNesteAktivitetStart(), fartherInTheFuture));
        assertTrue(calendarEqual(tiltakOppdateringer.getForrigeAktivitetStart(), someTimeAgo));
    }

    @Test
    public void skalBareTaHensynTilNyesteMoteplanForUtlopsdato() {
        Bruker bruker = new Bruker();
        Timestamp past = Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS));
        Timestamp future = Timestamp.from(Instant.now().plus(10, ChronoUnit.DAYS));

        bruker.getGruppeaktivitetListe().add(getGruppeaktivitetWithTwoMoeteplan(past, past, past, future));
        TiltakOppdateringer tiltakOppdateringer = TiltakUtils.finnOppdateringForBruker(bruker);

        assertThat(tiltakOppdateringer.getNyesteUtlopteAktivitet() == null).isTrue();
    }


    @Test
    public void skalBareTaHensynTilEldsteMoteplanForStartDato() {
        Bruker bruker = new Bruker();
        Timestamp past = Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS));
        Timestamp future = Timestamp.from(Instant.now().plus(10, ChronoUnit.DAYS));

        bruker.getGruppeaktivitetListe().add(getGruppeaktivitetWithTwoMoeteplan(past, future, future, future));
        TiltakOppdateringer tiltakOppdateringer = TiltakUtils.finnOppdateringForBruker(bruker);

        assertThat(tiltakOppdateringer.getAktivitetStart() == null).isTrue();
        assertThat(tiltakOppdateringer.getForrigeAktivitetStart() != null).isTrue();
    }

    @Test
    public void skalIkkeTryneOmDetIkkeFinnesDatoer() {
        TiltakOppdateringer tiltakOppdateringer = TiltakUtils.finnOppdateringForBruker(new Bruker());

        assertThat(tiltakOppdateringer.getNyesteUtlopteAktivitet() == null).isTrue();
        assertThat(tiltakOppdateringer.getAktivitetStart() == null).isTrue();
        assertThat(tiltakOppdateringer.getForrigeAktivitetStart() == null).isTrue();
        assertThat(tiltakOppdateringer.getNesteAktivitetStart() == null).isTrue();
    }

    @Test
    public void skalIkkeTaHensynTilDatoerForDatoFilter() {
        System.setProperty(DATOFILTER_PROPERTY, "2000-01-01");

        Bruker bruker = new Bruker();
        Timestamp beforeFilter = AktivitetUtils.parseDato("1999-01-01");
        bruker.getGruppeaktivitetListe().add(getGruppeaktivitet(beforeFilter, beforeFilter));
        TiltakOppdateringer tiltakOppdateringer = TiltakUtils.finnOppdateringForBruker(bruker);
        assertThat(tiltakOppdateringer.getNyesteUtlopteAktivitet() == null).isTrue();
        assertThat(tiltakOppdateringer.getAktivitetStart() == null).isTrue();
    }

    private Utdanningsaktivitet getUtdanningsaktivitet(Timestamp fra, Timestamp til) {
        LocalDate fraLocalDate = fra.toLocalDateTime().toLocalDate();
        XMLGregorianCalendar FOM = calendarOf(fraLocalDate.getYear(), fraLocalDate.getMonthValue(), fraLocalDate.getDayOfMonth());

        LocalDate tilLocalDate = til.toLocalDateTime().toLocalDate();
        XMLGregorianCalendar TOM = calendarOf(tilLocalDate.getYear(), tilLocalDate.getMonthValue(), tilLocalDate.getDayOfMonth());

        Utdanningsaktivitet utdanningsaktivitet = new Utdanningsaktivitet();
        utdanningsaktivitet.setAktivitetstype(UtdanningaktivitetTyper.EUTD.toString());
        Periode periode = new Periode();
        periode.setTom(TOM);
        periode.setFom(FOM);
        utdanningsaktivitet.setAktivitetPeriode(periode);
        return utdanningsaktivitet;
    }

    private Gruppeaktivitet getGruppeaktivitet(Timestamp fra, Timestamp til) {
        LocalDate FOM = fra.toLocalDateTime().toLocalDate();
        LocalDate TOM = til.toLocalDateTime().toLocalDate();

        Gruppeaktivitet gruppeaktivitet =  new Gruppeaktivitet();
        Moeteplan moeteplan = new Moeteplan();
        moeteplan.setSluttDato(calendarOf(TOM.getYear(), TOM.getMonthValue(), TOM.getDayOfMonth()));
        moeteplan.setStartDato(calendarOf(FOM.getYear(), FOM.getMonthValue(), FOM.getDayOfMonth()));
        gruppeaktivitet.getMoeteplanListe().add(moeteplan);

        return gruppeaktivitet;
    }

    private Gruppeaktivitet getGruppeaktivitetWithTwoMoeteplan(Timestamp fra1, Timestamp fra2, Timestamp til1, Timestamp til2) {
        LocalDate fraLocalDate1 = fra1.toLocalDateTime().toLocalDate();
        LocalDate fraLocalDate2 = fra2.toLocalDateTime().toLocalDate();
        LocalDate tilLocalDate1 = til1.toLocalDateTime().toLocalDate();
        LocalDate tilLocalDate2 = til2.toLocalDateTime().toLocalDate();

        Gruppeaktivitet gruppeaktivitet =  new Gruppeaktivitet();
        Moeteplan moeteplan1 = new Moeteplan();
        moeteplan1.setSluttDato(calendarOf(tilLocalDate1.getYear(), tilLocalDate1.getMonthValue(), tilLocalDate1.getDayOfMonth()));
        moeteplan1.setStartDato(calendarOf(fraLocalDate1.getYear(), fraLocalDate1.getMonthValue(), fraLocalDate1.getDayOfMonth()));
        Moeteplan moeteplan2 = new Moeteplan();
        moeteplan2.setSluttDato(calendarOf(tilLocalDate2.getYear(), tilLocalDate2.getMonthValue(), tilLocalDate2.getDayOfMonth()));
        moeteplan2.setStartDato(calendarOf(fraLocalDate2.getYear(), fraLocalDate2.getMonthValue(), fraLocalDate2.getDayOfMonth()));
        gruppeaktivitet.getMoeteplanListe().add(moeteplan1);
        gruppeaktivitet.getMoeteplanListe().add(moeteplan2);

        return gruppeaktivitet;
    }

    private Tiltaksaktivitet tiltaksaktivitetWithTiltakTOM(Timestamp fra, Timestamp til) {
        LocalDate fraLocalDate = fra.toLocalDateTime().toLocalDate();
        LocalDate tilLocalDate = til.toLocalDateTime().toLocalDate();
        Periode periode =  new Periode();
        periode.setTom(calendarOf(tilLocalDate.getYear(), tilLocalDate.getMonthValue(), tilLocalDate.getDayOfMonth()));
        periode.setFom(calendarOf(fraLocalDate.getYear(), fraLocalDate.getMonthValue(), fraLocalDate.getDayOfMonth()));
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

    private boolean calendarEqual(Timestamp t1, Timestamp t2) {
        return t1.toLocalDateTime().toLocalDate().isEqual(t2.toLocalDateTime().toLocalDate());
    }


}
