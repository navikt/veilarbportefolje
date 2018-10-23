package no.nav.fo.veilarbportefolje.util;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

public class DateUtils {

    private static final String RANDOM_FUTURE_DATE = "3017-10-07T00:00:00Z";

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

    public static String toIsoUTC(Timestamp timestamp) {
        if(timestamp == null) {
            return null;
        }
        DateTimeFormatter formatter =  DateTimeFormatter.ISO_INSTANT;
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneId.of("UTC"));
        return zonedDateTime.format(formatter);
    }

    public static boolean isEpoch0(Timestamp timestamp) {
        return "1970-01-01T00:00:00Z".equals(toIsoUTC(timestamp));
    }

    public static ZonedDateTime toZonedDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
    }

    public static String toUtcString(ZonedDateTime zonedDateTime) {
        if(Objects.isNull(zonedDateTime)) {
            return null;
        }
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

    public static Timestamp dateToTimestamp(Date date) {
        return Optional.ofNullable(date).map(Date::toInstant).map(Timestamp::from).orElse(null);
    }

    public static boolean isRandomFutureDate(Timestamp utlopsdato) {
        return timestampFromISO8601(RANDOM_FUTURE_DATE).equals(utlopsdato);
    }

    public static boolean isRandomFutureDate(Date date) {
        return isRandomFutureDate(Optional.ofNullable(date).map(Date::toInstant).map(Instant::toEpochMilli).map(Timestamp::new).orElse(null));
    }

    public static Timestamp getRandomFutureDate() {
        return timestampFromISO8601(RANDOM_FUTURE_DATE);
    }

    public static String getSolrMaxAsIsoUtc() {
        return toIsoUTC(getRandomFutureDate());
    }
}
