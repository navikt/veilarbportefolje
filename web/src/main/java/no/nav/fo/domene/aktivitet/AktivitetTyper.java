package no.nav.fo.domene.aktivitet;

public enum AktivitetTyper {
    egenaktivitet,
    jobbsoeking,
    sokeavtale,
    behandling,
    ijobb;

    public static boolean contains(String value) {
        try {
            AktivitetTyper.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
