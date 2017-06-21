package no.nav.fo.domene.Aktivitet;

public enum AktivitetTyper {
    jobbsoeking,
    egenaktivitet;

    public static boolean contains(String value) {
        try {
            AktivitetTyper.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
