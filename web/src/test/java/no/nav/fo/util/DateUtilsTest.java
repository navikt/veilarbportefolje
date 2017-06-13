package no.nav.fo.util;

import org.junit.Test;

import java.sql.Timestamp;
import java.time.ZoneId;

import static no.nav.fo.util.DateUtils.iso8601Fromtimestamp;
import static no.nav.fo.util.DateUtils.timestampFromISO8601;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class DateUtilsTest {

    @Test
    public void should_return_correct_date() {
        String original = "2010-12-03T10:15:30.100+02:00";
        Timestamp timestampFromString = timestampFromISO8601(original);
        String fromTimestamp = iso8601Fromtimestamp(timestampFromString, ZoneId.of("+02:00"));

        assertThat(fromTimestamp, is(original));
    }


}