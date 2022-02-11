package no.nav.pto.veilarbportefolje.aktiviteter;

public enum AktivitetType {
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
            AktivitetType.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
