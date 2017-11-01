package no.nav.fo.domene.aktivitet;

public enum AktivitetTyperFraAktivitetspan {
    egen,
    stilling,
    sokeavtale,
    behandling,
    ijobb,
    samtalereferat,
    mote;

    public static boolean contains(String value) {
        try {
            AktivitetTyperFraAktivitetspan.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
