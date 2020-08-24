package no.nav.pto.veilarbportefolje.aktiviteter;

public enum UtdanningaktivitetTyper {
    EUTD,
    OUTDEF,
    KURS;

    public static boolean contains(String value) {
        try {
            UtdanningaktivitetTyper.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
