package no.nav.fo.domene;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

public enum ManedMapping {
    MND1, MND2, MND3, MND4,
    MND5, MND6, MND7, MND8,
    MND9, MND10, MND11, MND12;

    public static Optional<ManedMapping> finnManed(LocalDateTime startDato, LocalDateTime dato) {
        if (dato.isBefore(startDato) || dato.isAfter(startDato.plusYears(1))) {
            return empty();
        }

        return of(values()[absoluttManedNummer(dato) - absoluttManedNummer(startDato)]);
    }

    static int absoluttManedNummer(LocalDateTime dato) {
        int utgangspunkt = 1970;
        int arDiff = dato.getYear() - utgangspunkt;

        return arDiff * 12 + dato.getMonthValue();
    }
}
