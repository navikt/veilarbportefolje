package no.nav.pto.veilarbportefolje.domene

import no.nav.pto.veilarbportefolje.aap.domene.Rettighetstype
import org.junit.Test
import org.junit.jupiter.api.Assertions
import java.time.LocalDate

class AapKelvinForBrukerTest {

    @Test
    fun `AapKelvinForBruker skal mappe data rett`() {
        val rettighetstype = Rettighetstype.VURDERES_FOR_UFØRETRYGD
        val vedtaksdatoTilOgMed = LocalDate.now()
        val aapKelvinForBruker = AapKelvinForBruker.of(vedtaksdatoTilOgMed, rettighetstype)

        val forventetRettighetstype = "Vurderes for uføretrygd"

        Assertions.assertEquals(forventetRettighetstype, aapKelvinForBruker.rettighetstype())
        Assertions.assertEquals(vedtaksdatoTilOgMed, aapKelvinForBruker.vedtaksdatoTilOgMed())
    }

    @Test
    fun `AapKelvinForBruker skal mappe data rett også om vi mangler et datafelt`() {
        val rettighetstype = Rettighetstype.VURDERES_FOR_UFØRETRYGD
        val vedtaksdatoTilOgMed = LocalDate.now()
        val aapKelvinForBrukerUtenRettighetstype = AapKelvinForBruker.of(vedtaksdatoTilOgMed, null)
        val aapKelvinForBrukerUtenVedtaksdatoTilOgMed = AapKelvinForBruker.of(null, rettighetstype)

        val forventetRettighetstype = "Vurderes for uføretrygd"

        Assertions.assertEquals(null, aapKelvinForBrukerUtenRettighetstype.rettighetstype())
        Assertions.assertEquals(vedtaksdatoTilOgMed, aapKelvinForBrukerUtenRettighetstype.vedtaksdatoTilOgMed())

        Assertions.assertEquals(forventetRettighetstype, aapKelvinForBrukerUtenVedtaksdatoTilOgMed.rettighetstype())
        Assertions.assertEquals(null, aapKelvinForBrukerUtenVedtaksdatoTilOgMed.vedtaksdatoTilOgMed())
    }

    @Test
    fun `AapKelvinForBruker skal bli null om det ikke finnes data for vedtaket`() {
        val aapKelvinForBruker = AapKelvinForBruker.of(null, null)

        Assertions.assertNull(aapKelvinForBruker)
    }

}
