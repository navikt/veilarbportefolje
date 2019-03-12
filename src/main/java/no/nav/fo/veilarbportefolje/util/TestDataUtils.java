package no.nav.fo.veilarbportefolje.util;

import java.util.UUID;

public class TestDataUtils {
    public static String randomFnr() {
        return UUID.randomUUID().toString().substring(0, 11);
    }
}
