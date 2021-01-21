package no.nav.pto.veilarbportefolje.util;

import org.junit.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

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
}
