package no.nav.pto.veilarbportefolje.feed.oppfolging;

import no.nav.pto.veilarbportefolje.domene.VurderingsBehov;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OppfolgingUtilsTest {

    @Test
    public void brukerSkalVaereUnderOppfolging() {
        assertTrue(OppfolgingUtils.erBrukerUnderOppfolging("ARBS", "DUMMY", true));
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
        assertTrue(OppfolgingUtils.trengerVurdering("IARBS", "BKART", false, null));
        assertTrue(OppfolgingUtils.trengerVurdering("IARBS", "IVURD", false, null));
    }
    @Test
    public void brukerMedISERVTrengerIkkeVurdering() {
        assertFalse(OppfolgingUtils.trengerVurdering("ISERV", "IVURD", false, null));
        assertFalse(OppfolgingUtils.trengerVurdering("ISERV", "BKART", false, null));
    }

    @Test
    public void brukerUtenBKART_IVURD_trengerIkkeVurdering() {
        assertFalse(OppfolgingUtils.trengerVurdering("IARBS", "VURDU", false, null));
    }

    @Test
    public void brukerMedIServHarIkkeVurderingsBehov() {
        assertThat(OppfolgingUtils.vurderingsBehov("ISERV", "BKART")).isNull();
        assertThat(OppfolgingUtils.vurderingsBehov("ISERV", "IVURD")).isNull();
        assertThat(OppfolgingUtils.vurderingsBehov("ISERV", "VURDU")).isNull();
    }

    @Test
    public void brukerUtenIServOgBKARTHarAEVBehov() {
        assertThat(OppfolgingUtils.vurderingsBehov("IARBS", "BKART")).isEqualTo(VurderingsBehov.ARBEIDSEVNE_VURDERING);
    }

    @Test
    public void brukerUtenIServOgIVURDTHarAEVBehov() {
        assertThat(OppfolgingUtils.vurderingsBehov("IARBS", "IVURD")).isEqualTo(VurderingsBehov.IKKE_VURDERT);
    }

    @Test
    public void brukerUtenIServOgUkjentKodeHarIkkeVurderingsBehov() {
        assertThat(OppfolgingUtils.vurderingsBehov("IARBS", "VURDU")).isNull();
    }
}
