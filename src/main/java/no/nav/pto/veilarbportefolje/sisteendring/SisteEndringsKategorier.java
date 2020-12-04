package no.nav.pto.veilarbportefolje.sisteendring;


public enum SisteEndringsKategorier {
    NY_AKTIVITET,
    ENDRET_AKTIVITET;

    public static boolean contains(String value) {
        try {
            SisteEndringsKategorier.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
