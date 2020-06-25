package no.nav.pto.veilarbportefolje.feedconsumer.aktivitet;

public enum AktivitetTyperFraAktivitetspan {
    egen,
    stilling,
    sokeavtale,
    behandling,
    ijobb,
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
