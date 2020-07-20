package no.nav.pto.veilarbportefolje.aktiviteter;

public enum AktivitetIkkeAktivStatuser {
    bruker_er_interessert,
    fullfort,
    avbrutt,
    gjennomfort;

    public static boolean contains(String value) {
        try {
            AktivitetIkkeAktivStatuser.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
