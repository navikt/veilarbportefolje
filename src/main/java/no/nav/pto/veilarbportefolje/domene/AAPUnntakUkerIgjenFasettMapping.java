package no.nav.pto.veilarbportefolje.domene;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public enum AAPUnntakUkerIgjenFasettMapping implements FasettMapping {
    UKE_UNDER12(0, 11), UKE12_23(12, 23), UKE24_35(24, 35), UKE36_47(36, 47),
    UKE48_59(48, 59), UKE60_71(60, 71), UKE72_83(72, 83), UKE84_95(84, 95),
    UKE96_107(96, 107);

    private final int start;
    private final int stop;

    AAPUnntakUkerIgjenFasettMapping(int start, int stop) {
        this.start = start;
        this.stop = stop;
    }

    public static Optional<AAPUnntakUkerIgjenFasettMapping> finnUkeMapping(int dager) {
        int MAX_ANTALL_DAGER = 522;

        if (dager > MAX_ANTALL_DAGER) {
            log.info("bruker med {} dager. max antall dager {}", dager, MAX_ANTALL_DAGER);
            dager = MAX_ANTALL_DAGER;
        } else if (dager < 0) {
            log.info("bruker emd mindre enn 0 dager, {} ", dager);
            dager = 0;
        }

        int uker = dager / 5;

        return Stream.of(values())
                .filter(mapping -> mapping.start <= uker && uker <= mapping.stop)
                .findAny();
    }

    public static AAPUnntakUkerIgjenFasettMapping of(String s) {
        if (s == null) {
            return null;
        }
        return valueOf(s);
    }
}
