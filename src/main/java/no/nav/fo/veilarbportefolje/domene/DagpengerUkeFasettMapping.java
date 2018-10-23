package no.nav.fo.veilarbportefolje.domene;

import java.util.Optional;
import java.util.stream.Stream;

public enum DagpengerUkeFasettMapping implements FasettMapping {
    UKE_UNDER2(0, 1), UKE2_5(2, 5), UKE6_9(6, 9), UKE10_13(10, 13),
    UKE14_17(14, 17), UKE18_21(18, 21), UKE22_25(22, 25), UKE26_29(26, 29),
    UKE30_33(30, 33), UKE34_37(34, 37), UKE38_41(38, 41), UKE42_45(42, 45),
    UKE46_49(46, 49), UKE50_52(50, 52);

    private final int start;
    private final int stop;

    DagpengerUkeFasettMapping(int start, int stop) {
        this.start = start;
        this.stop = stop;
    }

    public static Optional<DagpengerUkeFasettMapping> finnUkemapping(int uker) {
        return Stream.of(values())
                .filter((mapping) -> mapping.start <= uker && uker <= mapping.stop)
                .findAny();
    }

    public static DagpengerUkeFasettMapping of(String s) {
        if (s == null) {
            return null;
        }
        return valueOf(s);
    }
}
