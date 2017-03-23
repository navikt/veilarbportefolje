package no.nav.fo.domene;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

public enum KvartalMapping {
    KV1, KV2, KV3, KV4,
    KV5, KV6, KV7, KV8,
    KV9, KV10, KV11, KV12,
    KV13, KV14, KV15, KV16;

    public static Optional<KvartalMapping> finnKvartal(LocalDateTime startDato, LocalDateTime dato) {
        int kvartalDiff = absoluttKvartal(dato) - absoluttKvartal(startDato);
        if (kvartalDiff < 0 || kvartalDiff > 15) {
            return empty();
        }

        return Optional.of(values()[kvartalDiff]);
    }

    public static KvartalMapping of(String s) {
        if (s == null) {
            return null;
        }
        return valueOf(s);
    }

    static int absoluttKvartal(LocalDateTime dato) {
        int utgangspunkt = 1970;
        int arDiff = dato.getYear() - utgangspunkt;
        int kvartal = ((dato.getMonthValue() - 1) / 3) + 1;

        return arDiff * 4 + kvartal;
    }
}
