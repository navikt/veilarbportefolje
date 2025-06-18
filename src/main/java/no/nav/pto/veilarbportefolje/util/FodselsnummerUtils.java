package no.nav.pto.veilarbportefolje.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FodselsnummerUtils {
    private static final String DATO_POSTFIX = "T00:00:00Z";

    public static String lagFodselsdato(LocalDate foedselsDato) {
        return foedselsDato.format(DateTimeFormatter.ofPattern("uuuu-MM-dd")) + DATO_POSTFIX;
    }
}
