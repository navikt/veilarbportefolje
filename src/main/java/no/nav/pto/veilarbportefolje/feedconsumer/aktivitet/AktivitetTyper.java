package no.nav.pto.veilarbportefolje.feedconsumer.aktivitet;

public enum AktivitetTyper {
    egen,
    stilling,
    sokeavtale,
    behandling,
    ijobb,
    mote,
    tiltak,
    gruppeaktivitet,
    utdanningaktivitet;

    public static boolean contains(String value) {
        try {
            AktivitetTyper.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
