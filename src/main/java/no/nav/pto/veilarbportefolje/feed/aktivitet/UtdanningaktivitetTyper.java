package no.nav.pto.veilarbportefolje.feed.aktivitet;

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
