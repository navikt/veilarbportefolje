package no.nav.fo.domene;

import java.util.Optional;
import java.util.stream.Stream;

public enum AAPMaxtidUkeFasettMapping implements FasettMapping {
    UKE_UNDER2(0, 1), UKE2_10(2, 10), UKE11_19(11, 19), UKE20_28(20, 28),
    UKE29_37(29, 37), UKE38_46(38, 46), UKE47_55(47, 55), UKE56_64(56, 64),
    UKE65_73(65, 73), UKE74_82(74, 82), UKE83_91(83, 91), UKE92_100(92, 100),
    UKE101_109(101, 109), UKE110_118(110, 118), UKE119_127(119, 127), UKE128_136(128, 136),
    UKE137_145(137, 145), UKE146_154(146, 154), UKE155_163(155, 163), UKE164_172(164, 172),
    UKE173_181(173, 181), UKE182_190(182, 190), UKE191_199(191, 199), UKE200_208(200, 208),
    UKE209_215(209, 215);


    private final int start;
    private final int stop;

    AAPMaxtidUkeFasettMapping(int start, int stop) {
        this.start = start;
        this.stop = stop;
    }

    public static Optional<AAPMaxtidUkeFasettMapping> finnUkemapping(int uker) {
        return Stream.of(values())
                .filter((mapping) -> mapping.start <= uker && uker <= mapping.stop)
                .findAny();
    }

    public static AAPMaxtidUkeFasettMapping of(String s) {
        if (s == null) {
            return null;
        }
        return valueOf(s);
    }
}
