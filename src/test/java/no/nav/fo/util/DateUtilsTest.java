package no.nav.fo.util;

import org.junit.Test;

import java.sql.Timestamp;
import java.time.ZoneId;

import static no.nav.fo.util.DateUtils.*;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class DateUtilsTest {

    @Test
    public void should_return_correct_date() {
        String original = "2010-12-03T10:15:30.100+02:00";
        Timestamp timestampFromString = timestampFromISO8601(original);
        String fromTimestamp = iso8601FromTimestamp(timestampFromString, ZoneId.of("+02:00"));

        assertThat(fromTimestamp).isEqualTo(original);
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
}