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
        LocalDate FireDagerFrem = Idag.plusDays(4);
        LocalDate TiDagerSiden = Idag.minusDays(10);
        LocalDate FemtiDagerSiden = Idag.minusDays(50);
        LocalDate FemtiDagerFrem = Idag.plusDays(50);
        LocalDate TjuefemDagerFrem = Idag.plusDays(25);
        LocalDate TjuefemDagerSiden = Idag.minusDays(25);
        LocalDate TrettifireDagerSiden = Idag.minusDays(34);

        ArrayList<LocalDate> treDagerSidenNaermest = new ArrayList<>( Arrays.asList(TreDagerSiden, FireDagerFrem, TiDagerSiden, FemtiDagerSiden, FemtiDagerFrem, TjuefemDagerFrem, TjuefemDagerSiden, TrettifireDagerSiden));
        ArrayList<LocalDate> fireDagerFremNaermest = new ArrayList<>( Arrays.asList(FireDagerFrem, TiDagerSiden, FemtiDagerFrem, TjuefemDagerFrem, TrettifireDagerSiden, TjuefemDagerSiden));
        ArrayList<LocalDate> tiDagerSidenNaermest = new ArrayList<>( Arrays.asList(FemtiDagerSiden, FemtiDagerFrem, TjuefemDagerFrem, TjuefemDagerSiden, TiDagerSiden, TrettifireDagerSiden));
        ArrayList<LocalDate> toDatoerLikeNaermeImorgenSist = new ArrayList<>( Arrays.asList(TreDagerSiden, Igaar, TiDagerSiden, Imorgen, FemtiDagerFrem));
        ArrayList<LocalDate> toDatoerLikeNaermeIgaarSist = new ArrayList<>( Arrays.asList(TreDagerSiden, Imorgen, TiDagerSiden, Igaar, FemtiDagerFrem));
        ArrayList<LocalDate> kunEnDato = new ArrayList<>( Arrays.asList(FemtiDagerSiden));
        ArrayList<LocalDate> inneholderDagensDato = new ArrayList<>( Arrays.asList(TreDagerSiden, Igaar, FireDagerFrem, Idag, TiDagerSiden, Imorgen, FemtiDagerSiden));

        LocalDate expectTreDagerSiden = treDagerSidenNaermest.stream().min(Comparator.comparing(dato -> dato, closestToTodayComparator())).get();
        LocalDate expectFireDagerFrem = fireDagerFremNaermest.stream().min(Comparator.comparing(dato -> dato, closestToTodayComparator())).get();
        LocalDate expectImorgen = toDatoerLikeNaermeImorgenSist.stream().min(Comparator.comparing(dato -> dato, closestToTodayComparator())).get();
        LocalDate expectIgaar = toDatoerLikeNaermeIgaarSist.stream().min(Comparator.comparing(dato -> dato, closestToTodayComparator())).get();
        LocalDate expectFemtiDagerSiden = kunEnDato.stream().min(Comparator.comparing(dato -> dato, closestToTodayComparator())).get();
        LocalDate expectIdag = inneholderDagensDato.stream().min(Comparator.comparing(dato -> dato, closestToTodayComparator())).get();
        LocalDate expectTiDagerSidenNaermest = tiDagerSidenNaermest.stream().min(Comparator.comparing(dato -> dato, closestToTodayComparator())).get();

        LocalDate expectTreDagerSiden2 = treDagerSidenNaermest.stream().min(Comparator.comparing(dato -> closestToTodayComparatorDate(dato))).get();
        LocalDate expectFireDagerFrem2 = fireDagerFremNaermest.stream().min(Comparator.comparing(dato -> closestToTodayComparatorDate(dato))).get();
        LocalDate expectImorgen2 = toDatoerLikeNaermeImorgenSist.stream().min(Comparator.comparing(dato -> closestToTodayComparatorDate(dato))).get();
        LocalDate expectIgaar2 = toDatoerLikeNaermeIgaarSist.stream().min(Comparator.comparing(dato -> closestToTodayComparatorDate(dato))).get();
        LocalDate expectFemtiDagerSiden2 = kunEnDato.stream().min(Comparator.comparing(dato -> closestToTodayComparatorDate(dato))).get();
        LocalDate expectIdag2 = inneholderDagensDato.stream().min(Comparator.comparing(dato -> closestToTodayComparatorDate(dato))).get();
        LocalDate expectTiDagerSidenNaermest2 = tiDagerSidenNaermest.stream().min(Comparator.comparing(dato -> closestToTodayComparatorDate(dato))).get();


        AssertionsForClassTypes.assertThat(expectTreDagerSiden).isEqualTo(TreDagerSiden);
        AssertionsForClassTypes.assertThat(expectFireDagerFrem).isEqualTo(FireDagerFrem);
        AssertionsForClassTypes.assertThat(expectImorgen).isEqualTo(Igaar);
        AssertionsForClassTypes.assertThat(expectIgaar).isEqualTo(Imorgen);
        AssertionsForClassTypes.assertThat(expectFemtiDagerSiden).isEqualTo(FemtiDagerSiden);
        AssertionsForClassTypes.assertThat(expectIdag).isEqualTo(Idag);
        AssertionsForClassTypes.assertThat(expectTiDagerSidenNaermest).isEqualTo(TiDagerSiden);

        AssertionsForClassTypes.assertThat(expectTreDagerSiden2).isEqualTo(TreDagerSiden);
        AssertionsForClassTypes.assertThat(expectFireDagerFrem2).isEqualTo(FireDagerFrem);
        AssertionsForClassTypes.assertThat(expectImorgen2).isEqualTo(Igaar);
        AssertionsForClassTypes.assertThat(expectIgaar2).isEqualTo(Imorgen);
        AssertionsForClassTypes.assertThat(expectFemtiDagerSiden2).isEqualTo(FemtiDagerSiden);
        AssertionsForClassTypes.assertThat(expectIdag2).isEqualTo(Idag);
        AssertionsForClassTypes.assertThat(expectTiDagerSidenNaermest2).isEqualTo(TiDagerSiden);
    }
}