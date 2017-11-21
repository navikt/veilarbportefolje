package no.nav.fo.domene;

import java.util.Optional;
import java.util.stream.Stream;

public enum AAPUnntakDagerIgjenFasettMapping implements FasettMapping {
    UKE_UNDER12(0, 11), UKE12_23(12, 23), UKE24_35(24, 35), UKE36_47(36, 47),
    UKE48_59(48, 59), UKE60_71(60, 71), UKE72_83(72, 83), UKE84_95(84, 95),
    UKE96_107(96, 107), UKE108_119(108, 119), UKE120_131(120, 131), UKE132_143(132, 143),
    UKE144_155(144, 155), UKE156_167(156, 167), UKE168_179(168, 179), UKE180_191(180, 191),
    UKE192_203(192, 203), UKE204_215(204, 215);


    private final int start;
    private final int stop;

    AAPUnntakDagerIgjenFasettMapping(int start, int stop) {
        this.start = start;
        this.stop = stop;
    }

    public static Optional<AAPUnntakDagerIgjenFasettMapping> finnUkemapping(int uker) {
        return Stream.of(values())
                .filter((mapping) -> mapping.start <= uker && uker <= mapping.stop)
                .findAny();
    }

    public static AAPUnntakDagerIgjenFasettMapping of(String s) {
        if (s == null) {
            return null;
        }
        return valueOf(s);
    }
}
