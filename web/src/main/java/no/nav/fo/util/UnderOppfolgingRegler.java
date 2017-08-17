package no.nav.fo.util;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;

public class UnderOppfolgingRegler {

    static final Set<String> ARBEIDSOKERKODER = new HashSet<>(asList("ARBS", "RARBS", "PARBS"));
    static final Set<String> OPPFOLGINGKODER = new HashSet<>(asList("BATT", "BFORM", "IKVAL", "VURDU", "OPPFI", "VARIG"));
    static final String IKKE_ARBEIDSSOKER = "IARBS";

    // Logikken som utleder om en bruker er under oppfolging kjøres også i VeilArbSituasjon.
    // Endringer i logikken må implementeres begge steder
    public static boolean erUnderOppfolging(String formidlingsgruppeKode, String servicegruppeKode) {
        return erArbeidssoker(formidlingsgruppeKode) || erIArbeidOgHarInnsatsbehov(formidlingsgruppeKode, servicegruppeKode);
    }

    private static boolean erArbeidssoker(String formidlingsgruppeKode) {
        return ARBEIDSOKERKODER.contains(formidlingsgruppeKode);
    }

    private static boolean erIArbeidOgHarInnsatsbehov(String formidlingsgruppeKode, String servicegruppeKode) {
        return IKKE_ARBEIDSSOKER.equals(formidlingsgruppeKode) && OPPFOLGINGKODER.contains(servicegruppeKode);
    }

}
