package no.nav.fo.domene.aktivitet;

public enum AktivitetFullfortStatuser {
    fullfort,
    avbrutt,
    gjennomfort;

    public static boolean contains(String value) {
        try {
            AktivitetFullfortStatuser.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
