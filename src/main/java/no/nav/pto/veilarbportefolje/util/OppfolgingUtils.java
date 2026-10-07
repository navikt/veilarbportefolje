package no.nav.pto.veilarbportefolje.util;

import java.util.List;

import static java.util.Arrays.asList;

public class OppfolgingUtils {
    public static final List<String> INNSATSGRUPPEKODER = asList("IKVAL", "BFORM", "BATT", "VARIG");

    public static boolean erSykmeldtMedArbeidsgiver(String formidlingsgruppekode, String kvalifiseringsgruppekode) {
        return "IARBS".equals(formidlingsgruppekode) && kvalifiseringsgruppekode.equals("VURDI");
    }

}
