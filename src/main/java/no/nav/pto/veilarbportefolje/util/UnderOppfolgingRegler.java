package no.nav.pto.veilarbportefolje.util;


import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell;

public class UnderOppfolgingRegler {
    public static boolean erUnderOppfolging(PortefoljebrukerOpensearchModell bruker) {
        return oppfolgingsFlaggErSatt(bruker);
    }

    private static boolean oppfolgingsFlaggErSatt(PortefoljebrukerOpensearchModell bruker) {
        return bruker.isOppfolging();
    }
}
