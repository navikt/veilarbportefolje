package no.nav.pto.veilarbportefolje.util;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

public class DateUtils {

    private static final String FAR_IN_THE_FUTURE_DATE = "3017-10-07T00:00:00Z";
    private static final String EPOCH_0 = "1970-01-01T00:00:00Z";
    private static final String DATO_POSTFIX = "T00:00:00Z";

    public static Duration calculateTimeElapsed(Instant instant) {
        return Duration.between(instant, Instant.now());
    }

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
        return EPOCH_0.equals(toIsoUTC(timestamp));
    }

    public static ZonedDateTime toZonedDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("Europe/Oslo"));
    }

    public static ZonedDateTime toZonedDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneId.of("Europe/Oslo"));
    }

    public static Timestamp toTimestamp(ZonedDateTime zonedDateTime) {
        if(zonedDateTime == null){
            return null;
        }
        return Timestamp.from(zonedDateTime.toInstant());
    }

    public static String toIsoUTC(ZonedDateTime zonedDateTime) {
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

    public static Timestamp dateToTimestamp(Date date) {
        return Optional.ofNullable(date).map(Date::toInstant).map(Timestamp::from).orElse(null);
    }

    public static Timestamp dateToTimestamp(String date) {
        return Optional.ofNullable(date).map(x -> ZonedDateTime.parse(x).toInstant()).map(Timestamp::from).orElse(null);
    }

    public static Time dateToTime(String date){
        return Optional.ofNullable(date).map(Time::valueOf).orElse(null);
    }

    public static boolean isFarInTheFutureDate(Timestamp utlopsdato) {
        return timestampFromISO8601(FAR_IN_THE_FUTURE_DATE).equals(utlopsdato);
    }

    public static boolean isFarInTheFutureDate(Instant instant) {
        return isFarInTheFutureDate(Optional.ofNullable(instant).map(Instant::toEpochMilli).map(Timestamp::new).orElse(null));
    }

    private static Timestamp getFarInTheFutureTimestamp() {
        return timestampFromISO8601(FAR_IN_THE_FUTURE_DATE);
    }

    public static String getFarInTheFutureDate() {
        return toIsoUTC(getFarInTheFutureTimestamp());
    }

    public static Timestamp getTimestampFromSimpleISODate(String simpleISODate) {
        try {
            return dateToTimestamp(getISODateFormatter().parse(simpleISODate));
        } catch (ParseException e) {
            return null;
        }
    }

    public static LocalDate getLocalDateFromSimpleISODate(String simpleISODate) {
        if (simpleISODate == null) {
            return null;
        }
        return LocalDate.parse(simpleISODate, DateTimeFormatter.ofPattern("uuuu-MM-dd"));
    }

    public static SimpleDateFormat getISODateFormatter() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        simpleDateFormat.setLenient(false);
        return simpleDateFormat;
    }

    public static Timestamp zonedDateStringToTimestamp(CharSequence zonedDateString) {
        return toTimestamp(ZonedDateTime.parse(zonedDateString));
    }

    public static LocalDateTime toLocalDateTimeOrNull(String date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.parse(date), ZoneId.of("Europe/Oslo"));
    }

    public static java.sql.Date toSqlDateOrNull(String date){
        if (date == null) {
            return null;
        }

        return java.sql.Date.valueOf(ZonedDateTime.parse(date).toLocalDate());
    }


    public static String lagISOFromSimpleDateFormate(java.sql.Date fodselsdag) {
        if(fodselsdag == null){
            return null;
        }
        LocalDate localDate = fodselsdag.toLocalDate();
        return localDate.getYear() + "-" + moreThanOneChar(localDate.getMonthValue()) + "-" + moreThanOneChar(localDate.getDayOfMonth()) + DATO_POSTFIX;
    }

    private static String moreThanOneChar(int number){
        if(number < 10){
            return "0"+number;
        }
        return String.valueOf(number);
    }

}
