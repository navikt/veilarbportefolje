package no.nav.pto.veilarbportefolje.util;

import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import static java.lang.Math.abs;
import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static java.time.temporal.ChronoUnit.DAYS;

@Slf4j
public class DateUtils {

    public static final String FAR_IN_THE_FUTURE_DATE = "3017-10-07T00:00:00Z";
    private static final String EPOCH_0 = "1970-01-01T00:00:00Z";
    private static final String DATO_POSTFIX = "T00:00:00Z";

    public static Duration calculateTimeElapsed(Instant instant) {
        return Duration.between(instant, Instant.now());
    }

    public static Timestamp timestampFromISO8601(String date) {
        Instant instant = ZonedDateTime.parse(date).toInstant();
        return Timestamp.from(instant);
    }


    static String iso8601FromTimestamp(Timestamp timestamp, ZoneId zoneId) {
        if (timestamp == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(timestamp.toInstant(), zoneId).toString();
    }

    public static String toIsoUTC(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
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
        return ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public static ZonedDateTime toZonedDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault());
    }

    public static ZonedDateTime toZonedDateTime(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return localDate.atStartOfDay(ZoneOffset.UTC);
    }

    public static Timestamp toTimestamp(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return Timestamp.from(zonedDateTime.toInstant());
    }

    public static ZonedDateTime toZonedDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public static Timestamp toTimestamp(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return Timestamp.valueOf(localDate.atStartOfDay());
    }

    public static Timestamp toTimestamp(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Timestamp.valueOf(localDateTime);
    }

    public static String toIsoUTC(ZonedDateTime zonedDateTime) {
        if (Objects.isNull(zonedDateTime)) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        return zonedDateTime.format(formatter);
    }

    public static String toIsoUTC(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        ZonedDateTime zonedDateTime = ZonedDateTime.of(dateTime, ZoneId.of("UTC"));
        return zonedDateTime.format(formatter);
    }

    public static Timestamp dateToTimestamp(Date date) {
        return Optional.ofNullable(date).map(Date::toInstant).map(Timestamp::from).orElse(null);
    }

    public static Timestamp dateToTimestamp(String date) {
        return Optional.ofNullable(date).map(x -> ZonedDateTime.parse(x).toInstant()).map(Timestamp::from).orElse(null);
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
        return LocalDateTime.ofInstant(Instant.parse(date), ZoneId.systemDefault());
    }

    public static LocalDateTime toLocalDateTimeOrNull(Timestamp date) {
        if (date == null) {
            return null;
        }
        return date.toLocalDateTime();
    }

    public static LocalDate toLocalDate(Timestamp date) {
        if (date == null) {
            return null;
        }
        return date.toLocalDateTime().toLocalDate();
    }

    public static LocalDate toLocalDateOrNull(java.sql.Date date) {
        if (date == null) {
            return null;
        }
        return date.toLocalDate();
    }

    public static LocalDate toLocalDateOrNull(String date) {
        if (date == null) {
            return null;
        }
        return getTimestampFromSimpleISODate(date).toLocalDateTime().toLocalDate();
    }

    public static String nowToStr() {
        return ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS).format(ISO_ZONED_DATE_TIME);
    }

    public static LocalDate fnrToDate(String fnr) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyy");
        return LocalDate.ofInstant(formatter.parse(fnr.substring(0, 6)).toInstant(), ZoneId.systemDefault());
    }

    public static ZonedDateTime now() {
        return ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }

    public static LocalDate addWeeksToTodayAndGetNthDay(Timestamp initialDay, Integer weeksNumber, Integer dayNumber) {
        if (initialDay == null || weeksNumber == null || dayNumber == null) {
            return null;
        }

        WeekFields weekFields = WeekFields.ISO;
        TemporalField dayOfWeek = weekFields.dayOfWeek();
        return (DateUtils.toLocalDate(initialDay).plusWeeks(weeksNumber)).with(dayOfWeek, 1).plusDays(dayNumber);
    }

    public static Comparator<LocalDate> closestToTodayComparator() {
        LocalDate today = LocalDate.now();
        return Comparator.comparing(dato -> abs(DAYS.between(today, dato)));
    }

    public static boolean erUnder18Aar(LocalDate fodselsdato) {
        return (Period.between(fodselsdato, LocalDate.now()).getYears() < 18);
    }

    public static Integer alderFraFodselsdato(LocalDate date) {
        if (date == null) {
            log.warn("Input data i alderFraFodselsdato er null!");
            return null;
        }
        LocalDate now = LocalDate.now();
        return Period.between(date, now).getYears();
    }

    public static boolean isEqualOrAfterWithNullCheck(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.equals(date2) || date1.isAfter(date2);
    }

    public static String lagFodselsdato(LocalDate foedselsDato) {
        return foedselsDato.format(DateTimeFormatter.ofPattern("uuuu-MM-dd")) + DATO_POSTFIX;
    }
}
