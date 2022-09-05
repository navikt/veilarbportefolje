package no.nav.pto.veilarbportefolje.aktiviteter;

public enum AktivitetsType {
    egen,
    stilling,
    stilling_fra_nav,
    sokeavtale,
    behandling,
    ijobb,
    mote,
    tiltak,
    gruppeaktivitet,
    utdanningaktivitet;

    public static boolean contains(String value) {
        try {
            AktivitetsType.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
