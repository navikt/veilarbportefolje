package no.nav.pto.veilarbportefolje.util;

import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.Profileringsresultat;
import no.nav.pto.veilarbportefolje.domene.VurderingsBehov;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OppfolgingUtilsTest {

    @Test
    public void bruker_trenger_vurdering() {
        assertTrue(OppfolgingUtils.trengerVurdering("IARBS", "BKART"));
        assertTrue(OppfolgingUtils.trengerVurdering("IARBS", "IVURD"));
    }

    @Test
    public void bruker_med_iserv_trenger_ikke_vurdering() {
        assertFalse(OppfolgingUtils.trengerVurdering("ISERV", "IVURD"));
        assertFalse(OppfolgingUtils.trengerVurdering("ISERV", "BKART"));
    }

    @Test
    public void bruker_uten_bkart_ivurd_trenger_ikke_vurdering() {
        assertFalse(OppfolgingUtils.trengerVurdering("IARBS", "VURDU"));
    }

    @Test
    public void bruker_uten_iserv_og_bkart__aevbehov() {
        assertThat(OppfolgingUtils.vurderingsBehov("BKART", null)).isEqualTo(VurderingsBehov.ARBEIDSEVNE_VURDERING);
    }

    @Test
    public void bruker_uten_iserv_og_ivurd_har_aevbehov() {
        assertThat(OppfolgingUtils.vurderingsBehov("IVURD", null)).isEqualTo(VurderingsBehov.IKKE_VURDERT);
    }

    @Test
    public void bruker_uten_iserv_og_ukjent_kode_har_ikke_vurderingsbehov() {
        assertThat(OppfolgingUtils.vurderingsBehov("VURDU", null)).isNull();
    }

    @Test
    public void bruker_med_oppgitt_hindringer_har_aevbehov() {
        assertThat(OppfolgingUtils.vurderingsBehov("dontcare", Profileringsresultat.OPPGITT_HINDRINGER)).isEqualTo(VurderingsBehov.OPPGITT_HINDRINGER);
    }

    @Test
    public void bruker_med_antatt_gode_muligheter_har_ikke_vurderingsbehov() {
        assertThat(OppfolgingUtils.vurderingsBehov("dontcare", Profileringsresultat.ANTATT_GODE_MULIGHETER)).isEqualTo(VurderingsBehov.ANTATT_GODE_MULIGHETER);
    }

    @Test
    public void bruker_med_profilering_antatt_gode_muligheter_har_vurderingbehov_gode_muligheter() {
        assertThat(OppfolgingUtils.vurderingsBehov("dontcare", Profileringsresultat.ANTATT_GODE_MULIGHETER)).isEqualTo(VurderingsBehov.ANTATT_GODE_MULIGHETER);
    }

    @Test
    public void bruker_med_profilering_oppgitt_hindringer_har_vurderingbehov_opgitt_hindringer() {
        assertThat(OppfolgingUtils.vurderingsBehov("dontcare", Profileringsresultat.OPPGITT_HINDRINGER)).isEqualTo(VurderingsBehov.OPPGITT_HINDRINGER);
    }
}
