package no.nav.fo.util;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class UnderOppfolgingReglerTest {

    private static final Set<String> FORMIDLINGSGRUPPEKODER = new HashSet<>(asList("ISERV", "IARBS", "RARBS", "ARBS", "PARBS"));
    private static final Set<String> KVALIFISERINGSGRUPPEKODER = new HashSet<>(
            asList("BATT", "KAP11", "IKVAL", "IVURD", "VURDU", "VURDI", "VARIG", "OPPFI", "BKART", "BFORM"));

    @Test
    public void skalVareOppfolgningsbrukerPgaArenaStatus() throws Exception {
        assertThat(UnderOppfolgingRegler.erUnderOppfolging("IARBS", "BATT")).isTrue();
    }
    
    @Test
    public void sjekkUnderOppfolging_AlleKombinasjoner() {
        for (String fgKode : FORMIDLINGSGRUPPEKODER) {
            for (String kgKode : KVALIFISERINGSGRUPPEKODER) {
                System.out.println(String.format("[%s] [%s] - [%s]", fgKode, kgKode, UnderOppfolgingRegler.erUnderOppfolging(fgKode, kgKode)));
            }
            System.out.println("-------");
        }
    }
}
