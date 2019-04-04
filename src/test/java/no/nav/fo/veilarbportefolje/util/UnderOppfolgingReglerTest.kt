package no.nav.fo.veilarbportefolje.util

import no.nav.fo.veilarbportefolje.indeksering.domene.OppfolgingsBruker
import org.junit.Test

import java.util.HashSet

import java.util.Arrays.asList
import no.nav.fo.veilarbportefolje.util.UnderOppfolgingRegler.erUnderOppfolging
import org.assertj.core.api.Assertions.assertThat

class UnderOppfolgingReglerTest {


    @Test
    fun skal_vaere_under_oppfolging() {
        val bruker = OppfolgingsBruker()
                .setFnr("00000000000")
                .setFormidlingsgruppekode("foo")
                .setKvalifiseringsgruppekode("bar")
                .setOppfolging(true)

        val result = UnderOppfolgingRegler.erUnderOppfolging(bruker)
        assertThat(result).isTrue()
    }

    @Test
    fun skal_ikke_vaere_under_oppfolging() {
        val bruker = OppfolgingsBruker()
                .setFnr("00000000000")
                .setFormidlingsgruppekode("foo")
                .setKvalifiseringsgruppekode("bar")
                .setOppfolging(false)

        val result = UnderOppfolgingRegler.erUnderOppfolging(bruker)
        assertThat(result).isFalse()
    }


    @Test
    @Throws(Exception::class)
    fun skalVareOppfolgningsbrukerPgaArenaStatus() {
        assertThat(erUnderOppfolging("IARBS", "BATT")).isTrue()
    }

    @Test
    fun erUnderOppfolging_default_false() {
        assertThat(erUnderOppfolging(null, null)).isFalse()
    }

    @Test
    fun erUnderOppfolging_ARBS_true() {
        alleKombinasjonerErTrue("ARBS")
    }

    private fun alleKombinasjonerErTrue(formidlingsgruppeKode: String) {
        assertThat(erUnderOppfolging(formidlingsgruppeKode, null)).isTrue()
        for (kgKode in KVALIFISERINGSGRUPPEKODER) {
            assertThat(erUnderOppfolging(formidlingsgruppeKode, kgKode)).isTrue()
        }
    }

    @Test
    fun erUnderOppfolging_PARBS_true() {
        alleKombinasjonerErTrue("PARBS")
    }

    @Test
    fun erUnderOppfolging_RARBS_true() {
        alleKombinasjonerErTrue("RARBS")
    }

    @Test
    fun erUnderOppfolging_ISERV_false() {
        assertThat(erUnderOppfolging("ISERV", null)).isFalse()
        for (kgKode in KVALIFISERINGSGRUPPEKODER) {
            assertThat(erUnderOppfolging("ISERV", kgKode)).isFalse()
        }
    }

    @Test
    fun erUnderOppfolging_IARBS_true_for_BATT_BFORM_IKVAL_VURDU_OPPFI_VARIG() {
        for (kgKode in asList("BATT", "IKVAL", "VURDU", "OPPFI", "BFORM", "VARIG")) {
            assertThat(erUnderOppfolging("IARBS", kgKode)).isTrue()
        }
    }

    @Test
    fun erUnderOppfolging_IARBS_False_for_KAP11_IVURD_VURDI_BKART() {
        assertThat(erUnderOppfolging("IARBS", null)).isFalse()
        for (kgKode in asList("KAP11", "IVURD", "VURDI", "BKART")) {
            assertThat(erUnderOppfolging("IARBS", kgKode)).isFalse()
        }
    }

    companion object {

        private val KVALIFISERINGSGRUPPEKODER = HashSet(
                asList("BATT", "KAP11", "IKVAL", "IVURD", "VURDU", "VURDI", "VARIG", "OPPFI", "BKART", "BFORM"))
    }

}
