package no.nav.fo.veilarbportefolje.util;

import no.nav.fo.veilarbportefolje.indeksering.BrukerDTO;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static no.nav.fo.veilarbportefolje.util.UnderOppfolgingRegler.erUnderOppfolging;
import static org.assertj.core.api.Assertions.assertThat;

public class UnderOppfolgingReglerTest {

    private static final Set<String> KVALIFISERINGSGRUPPEKODER = new HashSet<>(
            asList("BATT", "KAP11", "IKVAL", "IVURD", "VURDU", "VURDI", "VARIG", "OPPFI", "BKART", "BFORM"));


    @Test
    public void skal_vaere_under_oppfolging() {
        BrukerDTO bruker = BrukerDTO.builder()
                .formidlingsgruppekode("foo")
                .kvalifiseringsgruppekode("bar")
                .oppfolging(true)
                .build();

        boolean result = UnderOppfolgingRegler.erUnderOppfolging(bruker);
        assertThat(result).isTrue();
    }

    @Test
    public void skal_ikke_vaere_under_oppfolging() {
        BrukerDTO bruker = BrukerDTO.builder()
                .formidlingsgruppekode("foo")
                .kvalifiseringsgruppekode("bar")
                .oppfolging(false)
                .build();

        boolean result = UnderOppfolgingRegler.erUnderOppfolging(bruker);
        assertThat(result).isFalse();
    }


    @Test
    public void skalVareOppfolgningsbrukerPgaArenaStatus() throws Exception {
        assertThat(erUnderOppfolging("IARBS", "BATT")).isTrue();
    }

    @Test
    public void erUnderOppfolging_default_false(){
        assertThat(erUnderOppfolging(null, null)).isFalse();
    }

    @Test
    public void erUnderOppfolging_ARBS_true() {
        alleKombinasjonerErTrue("ARBS");
    }

    private void alleKombinasjonerErTrue(String formidlingsgruppeKode) {
        assertThat(erUnderOppfolging(formidlingsgruppeKode, null)).isTrue();
        for (String kgKode : KVALIFISERINGSGRUPPEKODER) {
            assertThat(erUnderOppfolging(formidlingsgruppeKode, kgKode)).isTrue();
        }
    }

    @Test
    public void erUnderOppfolging_ISERV_false() {
        assertThat(erUnderOppfolging("ISERV", null)).isFalse();
        for (String kgKode : KVALIFISERINGSGRUPPEKODER) {
            assertThat(erUnderOppfolging("ISERV", kgKode)).isFalse();
        }
    }

    @Test
    public void erUnderOppfolging_IARBS_true_for_BATT_BFORM_IKVAL_VURDU_OPPFI_VARIG() {
        for (String kgKode : asList("BATT", "IKVAL", "VURDU", "OPPFI", "BFORM", "VARIG")) {
            assertThat(erUnderOppfolging("IARBS", kgKode)).isTrue();
        }
    }

    @Test
    public void erUnderOppfolging_IARBS_False_for_KAP11_IVURD_VURDI_BKART() {
        assertThat(erUnderOppfolging("IARBS", null)).isFalse();
        for (String kgKode : asList("KAP11", "IVURD", "VURDI", "BKART")) {
            assertThat(erUnderOppfolging("IARBS", kgKode)).isFalse();
        }
    }

}
