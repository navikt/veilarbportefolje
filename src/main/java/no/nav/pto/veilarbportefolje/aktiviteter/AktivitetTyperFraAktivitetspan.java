package no.nav.pto.veilarbportefolje.aktiviteter;

public enum AktivitetTyperFraAktivitetspan {
    egen,
    stilling,
    sokeavtale,
    behandling,
    ijobb,
    mote,

    tiltak,
    gruppeaktivitet,
    utdanningaktivitet; // TODO: her burde det være en toggle

    public static boolean contains(String value) {
        try {
            AktivitetTyperFraAktivitetspan.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
