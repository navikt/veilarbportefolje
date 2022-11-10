package no.nav.pto.veilarbportefolje.util;

import no.nav.common.types.identer.Id;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.time.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import static java.time.Duration.ofSeconds;
import static java.time.temporal.ChronoUnit.SECONDS;
import static no.nav.pto.veilarbportefolje.util.DateUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class DateUtilsTest {

    @Test
    public void skal_kalkulere_riktig_tidsintervall_basert_paa_timestamp() {
        Instant twoHoursAgo = Instant.now().minus(30, SECONDS);
        Duration duration = DateUtils.calculateTimeElapsed(twoHoursAgo);
        assertThat(duration.toMillis()).isLessThanOrEqualTo(ofSeconds(31).toMillis());
        assertThat(duration.toMillis()).isGreaterThanOrEqualTo(ofSeconds(29).toMillis());
    }

    @Test
    public void should_return_correct_date() {
        String original = "2010-12-03T10:15:30.100+02:00";
        Timestamp timestampFromString = timestampFromISO8601(original);
        String fromTimestamp = iso8601FromTimestamp(timestampFromString, ZoneId.of("+02:00"));

        assertThat(fromTimestamp).isEqualTo(original);
    }

    @Test
    public void should_return_utc_from_zonedDateTime() {
        String original = "2010-12-03T10:15:30.100+02:00";
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(original);
        String fromTimestamp = DateUtils.toIsoUTC(zonedDateTime);

        assertThat(fromTimestamp).isEqualTo("2010-12-03T08:15:30.100Z");
    }

    @Test
    public void should_return_utc_timestring_with_seconds() {
        Timestamp timestamp = timestampFromISO8601("2010-01-01T12:00+01:00");
        String utcString = toIsoUTC(timestamp);

        assertThat(utcString).isEqualTo("2010-01-01T11:00:00Z");
    }

    @Test
    public void shouldBeEqualToEpoch0() {
        assertTrue(isEpoch0(new Timestamp(0)));
    }

    @Test
    public void kort_datostring_skal_ikke_vaere_null() {
        Timestamp godkjentTimestamp = getTimestampFromSimpleISODate("2019-10-28");
        Timestamp feilendeTimestamp = getTimestampFromSimpleISODate("20191028");
        assertThat(godkjentTimestamp.toString()).isEqualTo("2019-10-28 00:00:00.0");
        assertThat(feilendeTimestamp).isNull();
    }

    @Test(expected = ParseException.class)
    public void ugyldig_dato_skal_kaste_exception() throws ParseException {
        getISODateFormatter().parse("2019-20-20");
    }

    @Test
    public void tekst_skal_returnere_null() {
        Timestamp hipp_hurra = getTimestampFromSimpleISODate("hipp hurra");
        assertThat(hipp_hurra).isNull();
    }

    @Test(expected = NullPointerException.class)
    public void null_skal_kaste_exception() {
        getTimestampFromSimpleISODate(null);
    }

    @Test
    public void testerValgAvDatoNaermestIDag() {
        LocalDate Idag = LocalDate.now();
        LocalDate Imorgen = Idag.plusDays(1);
        LocalDate Igaar = Idag.minusDays(1);
        LocalDate TreDagerSiden = Idag.minusDays(3);
        LocalDate FireDagerFremITid = Idag.plusDays(4);
        LocalDate TiDagerSiden = Idag.minusDays(10);
        LocalDate ElleveDagerSiden = Idag.minusDays(50);
        LocalDate FjortenDagerFremITid = Idag.plusDays(50);
        LocalDate TjuefemagerFremITid = Idag.plusDays(25);
        LocalDate TjuefemnDagerBakITid = Idag.minusDays(25);
        LocalDate TrettifireDagerBkITid = Idag.minusDays(34);

        ArrayList<LocalDate> treDagerSidenNaermest = new ArrayList<>( Arrays.asList(TreDagerSiden, FireDagerFremITid, TiDagerSiden, FjortenDagerFremITid, TjuefemagerFremITid, TrettifireDagerBkITid, TjuefemnDagerBakITid, ElleveDagerSiden));
        ArrayList<LocalDate> fireDagerFremNaermest = new ArrayList<>( Arrays.asList(FireDagerFremITid, TiDagerSiden, FjortenDagerFremITid, TjuefemagerFremITid, TrettifireDagerBkITid, TjuefemnDagerBakITid));
        ArrayList<LocalDate> toDatoerLikeNaerme = new ArrayList<>( Arrays.asList(TreDagerSiden, Igaar, FireDagerFremITid, TiDagerSiden, Imorgen, FjortenDagerFremITid));
        ArrayList<LocalDate> kunEnDato = new ArrayList<>( Arrays.asList(Igaar));
        ArrayList<LocalDate> inneholderDagensDato = new ArrayList<>( Arrays.asList(TreDagerSiden, Igaar, FireDagerFremITid, Idag, TiDagerSiden, Imorgen, FjortenDagerFremITid));

        LocalDate closest1 = treDagerSidenNaermest.stream().min(Comparator.comparing(dato -> dato, closestToTodayComparator())).get();
        LocalDate closest2 = fireDagerFremNaermest.stream().min(Comparator.comparing(dato -> dato, closestToTodayComparator())).get();
        LocalDate closest3 = toDatoerLikeNaerme.stream().min(Comparator.comparing(dato -> dato, closestToTodayComparator())).get();
        LocalDate closest4 = kunEnDato.stream().min(Comparator.comparing(dato -> dato, closestToTodayComparator())).get();
        LocalDate closest5 = inneholderDagensDato.stream().min(Comparator.comparing(dato -> dato, closestToTodayComparator())).get();
        LocalDate closest6 = treDagerSidenNaermest.stream().min(Comparator.comparing(dato -> closestToTodayComparatorDate(dato))).get();
        LocalDate closest7 = fireDagerFremNaermest.stream().min(Comparator.comparing(dato -> closestToTodayComparatorDate(dato))).get();
        LocalDate closest8 = toDatoerLikeNaerme.stream().min(Comparator.comparing(dato -> closestToTodayComparatorDate(dato))).get();
        LocalDate closest9 = kunEnDato.stream().min(Comparator.comparing(dato -> closestToTodayComparatorDate(dato))).get();
        LocalDate closest10 = inneholderDagensDato.stream().min(Comparator.comparing(dato -> closestToTodayComparatorDate(dato))).get();



        AssertionsForClassTypes.assertThat(closest1).isEqualTo(TreDagerSiden);
        AssertionsForClassTypes.assertThat(closest2).isEqualTo(FireDagerFremITid);
        AssertionsForClassTypes.assertThat(closest3).isEqualTo(Igaar);
        AssertionsForClassTypes.assertThat(closest4).isEqualTo(Igaar);
        AssertionsForClassTypes.assertThat(closest5).isEqualTo(Idag);
        AssertionsForClassTypes.assertThat(closest6).isEqualTo(TreDagerSiden);
        AssertionsForClassTypes.assertThat(closest7).isEqualTo(FireDagerFremITid);
        AssertionsForClassTypes.assertThat(closest8).isEqualTo(Igaar);
        AssertionsForClassTypes.assertThat(closest9).isEqualTo(Igaar);
        AssertionsForClassTypes.assertThat(closest10).isEqualTo(Idag);
        //AssertionsForClassTypes.assertThat(fireDagerFremNaermest.stream().min(Comparator.comparing(dato -> dato, closestToTodayComparator())).get()).isEqualTo(FireDagerFremITid);
        //AssertionsForClassTypes.assertThat(toDatoerLikeNaerme.stream().min(Comparator.comparing(dato -> dato, closestToTodayComparator())).get()).isEqualTo(Imorgen);
        //AssertionsForClassTypes.assertThat(kunEnDato.stream().min(Comparator.comparing(dato -> dato, closestToTodayComparator())).get()).isEqualTo(FjortenDagerFremITid);
        //AssertionsForClassTypes.assertThat(inneholderDagensDato.stream().min(Comparator.comparing(dato -> dato, closestToTodayComparator())).get()).isEqualTo(Idag);
    }
}