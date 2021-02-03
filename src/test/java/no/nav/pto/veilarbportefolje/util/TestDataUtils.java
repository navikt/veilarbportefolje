package no.nav.pto.veilarbportefolje.util;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.*;

import static java.lang.String.valueOf;
import static java.util.concurrent.ThreadLocalRandom.current;

public class TestDataUtils {

    public static Fnr randomFnr() {
        return Fnr.ofValidFnr(randomDigits(11));
    }


    public static AktorId randomAktorId() {
        return AktorId.of(valueOf(current().nextInt()));
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
}
