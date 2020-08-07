package no.nav.pto.veilarbportefolje.util;


import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;

public class UnderOppfolgingRegler {

    static final String ARBEIDSOKER = "ARBS";
    static final Set<String> OPPFOLGINGKODER = new HashSet<>(asList("BATT", "BFORM", "IKVAL", "VURDU", "OPPFI", "VARIG"));
    static final String IKKE_ARBEIDSSOKER = "IARBS";

    // Logikken som utleder om en bruker er under oppfolging kjøres også i VeilArbOppfolging.
    // Endringer i logikken må implementeres begge steder
    public static boolean erUnderOppfolging(String formidlingsgruppeKode, String servicegruppeKode) {
        return erArbeidssoker(formidlingsgruppeKode) || erIArbeidOgHarInnsatsbehov(formidlingsgruppeKode, servicegruppeKode);
    }

    public static boolean erUnderOppfolging(OppfolgingsBruker bruker) {
        Optional<String> formidlingsgruppekode = Optional.ofNullable(bruker.getFormidlingsgruppekode());
        Optional<String> servicegruppeKode = Optional.ofNullable(bruker.getKvalifiseringsgruppekode());

        boolean erUnderOppfolgingIArena = false;
        if(servicegruppeKode.isPresent() && formidlingsgruppekode.isPresent()) {
            erUnderOppfolgingIArena = erUnderOppfolging(formidlingsgruppekode.get(), servicegruppeKode.get());
        }
        return oppfolgingsFlaggErSatt(bruker) || erUnderOppfolgingIArena;
    }

    public static boolean erUnderOppfolging(Result<OppfolgingsBruker> bruker) {
        return bruker
                .mapOk(UnderOppfolgingRegler::erUnderOppfolging)
                .orElse(false);
    }

    private static boolean erArbeidssoker(String formidlingsgruppeKode) {
        return ARBEIDSOKER.equals(formidlingsgruppeKode);
    }

    private static boolean erIArbeidOgHarInnsatsbehov(String formidlingsgruppeKode, String servicegruppeKode) {
        return IKKE_ARBEIDSSOKER.equals(formidlingsgruppeKode) && OPPFOLGINGKODER.contains(servicegruppeKode);
    }

    private static boolean oppfolgingsFlaggErSatt(OppfolgingsBruker bruker) {
        return bruker.isOppfolging();
    }
}
