package no.nav.fo.domene;

import no.nav.fo.exception.UgyldigAntallDagerIgjenException;

import java.util.Optional;
import java.util.stream.Stream;

public enum AAPUnntakUkerIgjenFasettMapping implements FasettMapping {
    UKE_UNDER2(0, 1), UKE2_6(2, 6), UKE7_11(7, 11), UKE12_16(12, 16),
    UKE17_21(17, 21), UKE22_26(22, 26), UKE27_31(27, 31), UKE32_36(32, 36),
    UKE37_41(37, 41), UKE42_46(42, 46), UKE47_51(47, 51), UKE52_56(52, 56),
    UKE57_61(57, 61), UKE62_66(62, 66), UKE67_71(67, 71), UKE72_76(72, 76),
    UKE77_81(77, 81), UKE82_86(82, 86), UKE87_91(87, 91), UKE92_96(92, 96),
    UKE97_101(97, 101), UKE102_104(102, 104);

    private final int start;
    private final int stop;

    AAPUnntakUkerIgjenFasettMapping(int start, int stop) {
        this.start = start;
        this.stop = stop;
    }

    public static Optional<AAPUnntakUkerIgjenFasettMapping> finnUkeMapping(int dager) {
        int MAX_ANTALL_DAGER = 522;

        if (dager > MAX_ANTALL_DAGER) {
            throw new UgyldigAntallDagerIgjenException(dager);
        } else if (dager < 0) {
            dager = 0;
        }

        int uker = dager / 5;

        return Stream.of(values())
                .filter((mapping) -> mapping.start <= uker && uker <= mapping.stop)
                .findAny();
    }

    public static AAPUnntakUkerIgjenFasettMapping of(String s) {
        if (s == null) {
            return null;
        }
        return valueOf(s);
    }
}
