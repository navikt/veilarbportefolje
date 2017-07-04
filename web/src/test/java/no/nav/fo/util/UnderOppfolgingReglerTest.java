package no.nav.fo.util;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Test;

public class UnderOppfolgingReglerTest {

    @Test
    public void skalVareOppfolgningsbrukerPgaArenaStatus() throws Exception {
        assertThat(UnderOppfolgingRegler.erUnderOppfolging("IARBS", "BATT")).isTrue();
    }
}
