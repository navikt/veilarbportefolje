package no.nav.fo.domene.aktivitet;

public enum AktivitetTyper {
    egen,
    stilling,
    sokeavtale,
    behandling,
    ijobb,
    samtalereferat,
    mote,
    tiltak,
    gruppeaktivitet;

    public static boolean contains(String value) {
        try {
            AktivitetTyper.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
