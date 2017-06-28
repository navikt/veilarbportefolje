package no.nav.fo.util;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtils {

    public static Timestamp timestampFromISO8601(String date) {
        Instant instant =  ZonedDateTime.parse(date).toInstant();
        return Timestamp.from(instant);
    }

    static String iso8601FromTimestamp(Timestamp timestamp, ZoneId zoneId) {
        if(timestamp == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(timestamp.toInstant(), zoneId).toString();
    }

    static String toIsoUTC(Timestamp timestamp) {
        if(timestamp == null) {
            return null;
        }
        DateTimeFormatter formatter =  DateTimeFormatter.ISO_INSTANT;
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneId.of("UTC"));
        return zonedDateTime.format(formatter);
    }

    public static ZonedDateTime toZonedDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
    }

    public static String toUtcString(ZonedDateTime zonedDateTime) {
        DateTimeFormatter formatter =  DateTimeFormatter.ISO_INSTANT;
        return zonedDateTime.format(formatter);
    }

    public static String toIsoUTC(LocalDateTime dateTime) {
        if(dateTime == null) {
            return null;
        }
        DateTimeFormatter formatter =  DateTimeFormatter.ISO_INSTANT;
        ZonedDateTime zonedDateTime = ZonedDateTime.of(dateTime, ZoneId.of("UTC"));
        return zonedDateTime.format(formatter);
    }

    public static LocalDateTime toLocalDateTime(Date dato) {
        if (dato == null) {
            return null;
        }
        return LocalDateTime.ofInstant(dato.toInstant(), ZoneId.systemDefault());
    }
}
