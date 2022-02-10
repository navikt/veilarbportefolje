package no.nav.pto.veilarbportefolje.util;


import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;

public class UnderOppfolgingRegler {
    public static boolean erUnderOppfolging(OppfolgingsBruker bruker) {
        return oppfolgingsFlaggErSatt(bruker);
    }

    private static boolean oppfolgingsFlaggErSatt(OppfolgingsBruker bruker) {
        return bruker.isOppfolging();
    }
}
