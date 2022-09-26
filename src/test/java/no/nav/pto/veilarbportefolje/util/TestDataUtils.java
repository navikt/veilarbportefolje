package no.nav.pto.veilarbportefolje.util;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.*;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakKafkaDTO;

import java.time.*;
import java.util.Random;

import static java.lang.String.valueOf;
import static java.time.Month.DECEMBER;
import static java.time.Month.JANUARY;
import static java.util.concurrent.ThreadLocalRandom.current;

public class TestDataUtils {

    private final static Random random = new Random();

    public static Fnr randomFnr() {
        return Fnr.ofValidFnr("010101" + randomDigits(5));
    }

    public static LocalDate randomLocalDate() {
        YearMonth yearMonth = YearMonth.of(random.nextInt(2000, Year.now().getValue() + 10),
                random.nextInt(JANUARY.getValue(), DECEMBER.getValue() + 1));
        return LocalDate.of(
                yearMonth.getYear(),
                yearMonth.getMonth(),
                random.nextInt(1, yearMonth.atEndOfMonth().getDayOfMonth() + 1));
    }

    public static LocalTime randomLocalTime() {
        return LocalTime.of(random.nextInt(0, 24), random.nextInt(0, 60));
    }

    public static ZonedDateTime randomZonedDate() {
        return ZonedDateTime.of(
                randomLocalDate(),
                randomLocalTime(),
                ZoneId.systemDefault());
    }

    public static AktorId randomAktorId() {
        return AktorId.of(randomDigits(13));
    }

    public static PersonId randomPersonId() {
        return PersonId.of(valueOf(current().nextInt()));
    }

    public static VeilederId randomVeilederId() {
        final String zIdent = "Z" + randomDigits(6);
        return VeilederId.of(zIdent);
    }

    public static NavKontor randomNavKontor() {
        return NavKontor.of(randomDigits(4));
    }

    private static String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('0' + current().nextInt(10)));
        }
        return sb.toString();
    }

    public static Siste14aVedtakKafkaDTO.Innsatsgruppe randomInnsatsgruppe() {
        return Siste14aVedtakKafkaDTO.Innsatsgruppe.values()[random.nextInt(Siste14aVedtakKafkaDTO.Innsatsgruppe.values().length)];
    }

    public static  Siste14aVedtakKafkaDTO.Hovedmal randomHovedmal() {
        return Siste14aVedtakKafkaDTO.Hovedmal.values()[random.nextInt(Siste14aVedtakKafkaDTO.Hovedmal.values().length)];
    }
}
