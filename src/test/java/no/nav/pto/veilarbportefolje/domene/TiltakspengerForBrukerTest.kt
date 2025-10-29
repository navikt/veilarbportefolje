package no.nav.pto.veilarbportefolje.domene

import no.nav.pto.veilarbportefolje.domene.frontendmodell.TiltakspengerForBruker
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet
import org.junit.Test
import org.junit.jupiter.api.Assertions
import java.time.LocalDate

class TiltakspengerForBrukerTest {

    @Test
    fun `TiltakspengerForBruker skal mappe data rett`() {
        val rettighet = TiltakspengerRettighet.TILTAKSPENGER_OG_BARNETILLEGG
        val vedtaksdatoTilOgMed = LocalDate.now()
        val tiltakspengerForBruker = TiltakspengerForBruker.of(vedtaksdatoTilOgMed, rettighet)

        val forventetRettighet = "Tiltakspenger og barnetillegg"

        Assertions.assertEquals(forventetRettighet, tiltakspengerForBruker.rettighet())
        Assertions.assertEquals(vedtaksdatoTilOgMed, tiltakspengerForBruker.vedtaksdatoTilOgMed())
    }

    @Test
    fun `TiltakspengerForBruker skal mappe data rett også om vi mangler et datafelt`() {
        val rettighet = TiltakspengerRettighet.TILTAKSPENGER_OG_BARNETILLEGG
        val vedtaksdatoTilOgMed = LocalDate.now()
        val tiltakspengerForBrukerUtenRettighet = TiltakspengerForBruker.of(vedtaksdatoTilOgMed, null)
        val tiltakspengerForBrukerUtenVedtaksdatoTilOgMed = TiltakspengerForBruker.of(null, rettighet)

        val forventetRettighetstype = "Tiltakspenger og barnetillegg"

        Assertions.assertEquals(null, tiltakspengerForBrukerUtenRettighet.rettighet())
        Assertions.assertEquals(vedtaksdatoTilOgMed, tiltakspengerForBrukerUtenRettighet.vedtaksdatoTilOgMed())

        Assertions.assertEquals(forventetRettighetstype, tiltakspengerForBrukerUtenVedtaksdatoTilOgMed.rettighet())
        Assertions.assertEquals(null, tiltakspengerForBrukerUtenVedtaksdatoTilOgMed.vedtaksdatoTilOgMed())
    }

    @Test
    fun `TiltakspengerForBruker skal bli null om det ikke finnes data for vedtaket`() {
        val tiltakspengerForBruker = TiltakspengerForBruker.of(null, null)
        Assertions.assertNull(tiltakspengerForBruker)
    }

}
