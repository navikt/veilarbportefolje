package no.nav.pto.veilarbportefolje.feed.oppfolging;

import no.nav.pto.veilarbportefolje.domene.VurderingsBehov;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OppfolgingUtilsTest {

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

    @Test
    public void brukerMedIServHarIkkeVurderingsBehov() {
        assertThat(OppfolgingUtils.vurderingsBehov("ISERV", "BKART", null)).isNull();
        assertThat(OppfolgingUtils.vurderingsBehov("ISERV", "IVURD", null)).isNull();
        assertThat(OppfolgingUtils.vurderingsBehov("ISERV", "VURDU", null)).isNull();
    }

    @Test
    public void brukerUtenIServOgBKARTHarAEVBehov() {
        assertThat(OppfolgingUtils.vurderingsBehov("IARBS", "BKART", null)).isEqualTo(VurderingsBehov.ARBEIDSEVNE_VURDERING);
    }

    @Test
    public void brukerUtenIServOgIVURDTHarAEVBehov() {
        assertThat(OppfolgingUtils.vurderingsBehov("IARBS", "IVURD", null)).isEqualTo(VurderingsBehov.IKKE_VURDERT);
    }

    @Test
    public void brukerUtenIServOgUkjentKodeHarIkkeVurderingsBehov() {
        assertThat(OppfolgingUtils.vurderingsBehov("IARBS", "VURDU", null)).isNull();
    }
}
