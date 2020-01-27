package no.nav.pto.veilarbportefolje.domene;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.util.Optional.empty;

public enum ManedFasettMapping implements FasettMapping {
    MND1, MND2, MND3, MND4,
    MND5, MND6, MND7, MND8,
    MND9, MND10, MND11, MND12;

    public static Optional<ManedFasettMapping> finnManed(LocalDateTime startDato, LocalDateTime dato) {
        int mndDiff = absoluttManedNummer(dato) - absoluttManedNummer(startDato);
        if (mndDiff < 0 || mndDiff > 11) {
            return empty();
        }

        return Optional.of(values()[mndDiff]);
    }

    public static ManedFasettMapping of(String s) {
        if (s == null) {
            return null;
        }
        return valueOf(s);
    }

    static int absoluttManedNummer(LocalDateTime dato) {
        int utgangspunkt = 1970;
        int arDiff = dato.getYear() - utgangspunkt;

        return arDiff * 12 + dato.getMonthValue();
    }
}
