package no.nav.fo.domene.aktivitet;

public enum AktivitetTyper {
    egen,
    stilling,
    sokeavtale,
    behandling,
    ijobb,
    mote;

    public static boolean contains(String value) {
        try {
            AktivitetTyper.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
