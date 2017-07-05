package no.nav.fo.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class UnderOppfolgingRegler {

    private static final Set<String> IKKE_UNDER_OPPFOLGING_SERVICEGRUPPEKODER = new HashSet<>(Arrays.asList(
            "BKART", "IVURD", "KAP11", "VARIG", "VURDI"));
    
    // Logikken som utleder om en bruker er under oppfolging kjøres også i VeilArbSituasjon.
    // Endringer i logikken må implementeres begge steder
    public static boolean erUnderOppfolging(String formidlingsgruppekode, String servicegruppekode) {
        return !(formidlingsgruppekode.equals("ISERV") ||
                (formidlingsgruppekode.equals("IARBS") && IKKE_UNDER_OPPFOLGING_SERVICEGRUPPEKODER.contains(servicegruppekode)));
    }

}
