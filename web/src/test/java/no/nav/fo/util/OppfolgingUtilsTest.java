package no.nav.fo.util;

import org.junit.Test;

import static no.nav.fo.util.UnderOppfolgingRegler.ARBEIDSOKERKODER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OppfolgingUtilsTest {

    @Test
    public void brukerSkalVaereUnderOppfolging() {
        String formidlingsgruppekode = (String) ARBEIDSOKERKODER.toArray()[0];
        assertTrue(OppfolgingUtils.erBrukerUnderOppfolging(formidlingsgruppekode, "DUMMY", true));
    }

    @Test
    public void brukerSkalVaereUnderOppfolging2() {
        assertTrue(OppfolgingUtils.erBrukerUnderOppfolging("DUMMY", "DUMMY", true));
    }

    @Test
    public void brukerSkalIKKEVaereUnderOppfolging1() {
        assertFalse(OppfolgingUtils.erBrukerUnderOppfolging("DUMMY", "DUMMY", false));
    }

    @Test
    public void brukerTrengerVurdering() {
        assertTrue(OppfolgingUtils.trengerVurdering("IARBS", "BKART"));
        assertTrue(OppfolgingUtils.trengerVurdering("IARBS", "IVURD"));
    }
    @Test
    public void brukerMedISERVTrengerIkkeVurdering() {
        assertFalse(OppfolgingUtils.trengerVurdering("ISERV", "IVURD"));
        assertFalse(OppfolgingUtils.trengerVurdering("ISERV", "BKART"));
    }

    @Test
    public void brukerUtenBKART_IVURD_trengerIkkeVurdering() {
        assertFalse(OppfolgingUtils.trengerVurdering("IARBS", "VURDU"));
    }
}
