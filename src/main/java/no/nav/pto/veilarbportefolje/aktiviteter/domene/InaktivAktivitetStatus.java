package no.nav.pto.veilarbportefolje.aktiviteter.domene;

public enum InaktivAktivitetStatus {
    BRUKER_ER_INTERESSERT,
    FULLFORT,
    AVBRUTT,
    GJENNOMFORT;

    public static boolean contains(String value) {
        try {
            InaktivAktivitetStatus.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
