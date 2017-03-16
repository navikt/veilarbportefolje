package no.nav.fo.domene;

import java.time.LocalDate;

public enum KvartalMapping {
    KV1, KV2, KV3, KV4,
    KV5, KV6, KV7, KV8,
    KV9, KV10, KV11, KV12,
    KV13, KV14, KV15, KV16;

    public static KvartalMapping finnKvartal(LocalDate startDato, LocalDate dato) {
        return KvartalMapping.values()[absoluttKvartal(dato) - absoluttKvartal(startDato)];
    }

    static int absoluttKvartal(LocalDate dato) {
        int utgangspunkt = 1970;
        int arDiff = dato.getYear() - utgangspunkt;
        int kvartal = ((dato.getMonthValue()-1) / 3) + 1;

        return arDiff * 4 + kvartal;
    }
}
