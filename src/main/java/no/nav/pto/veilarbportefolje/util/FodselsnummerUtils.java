package no.nav.pto.veilarbportefolje.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FodselsnummerUtils {
    private static final String DATO_POSTFIX = "T00:00:00Z";

    public static String lagFodselsdagIMnd(String fnr) {
        if (erDNummer(fnr)) {
            fnr = konverterDNummerTilFodselsnummer(fnr);
        }
        return fnr.substring(0, 2);
    }

    public static String lagFodselsdato(LocalDate foedselsDato) {
        return foedselsDato.format(DateTimeFormatter.ofPattern("uuuu-MM-dd")) + DATO_POSTFIX;
    }

    public static String lagKjonn(String fnr) {
        if (fnr == null) {
            return null;
        }
        int kjonnsNr = Integer.parseInt(fnr.substring(8, 9));
        return kjonnsNr % 2 == 0 ? "K" : "M";
    }

    static boolean erDNummer(String fnr) {
        return Integer.parseInt(fnr.substring(0, 1)) > 3;
    }

    static String konverterDNummerTilFodselsnummer(String dnr) {
        int forsteSiffer = Integer.parseInt(dnr.substring(0, 1));
        forsteSiffer -= 4;
        return forsteSiffer + dnr.substring(1);
    }
}
