package no.nav.pto.veilarbportefolje.util;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;
import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1;

import java.time.*;
import java.util.Random;
import java.util.UUID;

import static java.lang.String.valueOf;
import static java.time.Duration.ofDays;
import static java.time.Month.DECEMBER;
import static java.time.Month.JANUARY;
import static java.util.concurrent.ThreadLocalRandom.current;
import static org.apache.commons.lang3.RandomUtils.nextLong;

public class TestDataUtils {

    private final static Random random = new Random();

    public static Fnr randomFnr() {
        return Fnr.ofValidFnr("010101" + randomDigits(5));
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

    public static ZonedDateTime tilfeldigDatoTilbakeITid() {
        return ZonedDateTime.now().minus(ofDays(nextLong(30, 1000))).withNano(0);
    }

    public static ZonedDateTime tilfeldigSenereDato(ZonedDateTime zonedDateTime) {
        return zonedDateTime.plus(ofDays(nextLong(1, 10)));
    }

    public static SisteOppfolgingsperiodeV1 genererStartetOppfolgingsperiode(AktorId aktorId, ZonedDateTime startDato) {
        return new SisteOppfolgingsperiodeV1(UUID.randomUUID(), aktorId.get(), startDato, null);
    }

    public static SisteOppfolgingsperiodeV1 genererStartetOppfolgingsperiode(AktorId aktorId) {
        return genererStartetOppfolgingsperiode(aktorId, tilfeldigDatoTilbakeITid());
    }

    public static SisteOppfolgingsperiodeV1 genererAvsluttetOppfolgingsperiode(AktorId aktorId) {
        SisteOppfolgingsperiodeV1 periode = genererStartetOppfolgingsperiode(aktorId);
        return new SisteOppfolgingsperiodeV1(
                periode.getUuid(),
                aktorId.get(),
                periode.getStartDato(),
                tilfeldigSenereDato(periode.getStartDato())
        );
    }

    public static SisteOppfolgingsperiodeV1 genererSluttdatoForOppfolgingsperiode(
            SisteOppfolgingsperiodeV1 periode,
            ZonedDateTime sluttDato
    ) {
        return new SisteOppfolgingsperiodeV1(
                periode.getUuid(),
                periode.getAktorId(),
                periode.getStartDato(),
                sluttDato
        );
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

    public static SisteOppfolgingsperiodeV1 genererSluttdatoForOppfolgingsperiode(SisteOppfolgingsperiodeV1 periode) {
        return genererSluttdatoForOppfolgingsperiode(periode, tilfeldigSenereDato(periode.getStartDato()));
    }

    public static Innsatsgruppe randomInnsatsgruppe() {
        return Innsatsgruppe.values()[random.nextInt(Innsatsgruppe.values().length)];
    }

    public static Hovedmal randomHovedmal() {
        return Hovedmal.values()[random.nextInt(Hovedmal.values().length)];
    }
}
