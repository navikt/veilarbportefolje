package no.nav.pto.veilarbportefolje.aktiviteter.v1

import no.nav.common.types.identer.EnhetId
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.junit.Assert.assertEquals
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [ApplicationConfigTest::class])
class TiltaksAktivitetServiceTest {
    val brukertiltakRepository: BrukertiltakRepository = mock(BrukertiltakRepository::class.java)
    val tiltaksaktivitetService = TiltaksaktivitetService(brukertiltakRepository)

    @Test
    fun `skal hente tiltakstyper for enhet`() {
        val enhetId = EnhetId("1234")
        val expectedTiltakskodeMapping = TiltakskodeMapping(
            mutableMapOf(
                "TILTAK1" to "Tiltak 1",
                "TILTAK2" to "Tiltak 2"
            )
        )
        `when`(brukertiltakRepository.hentTiltakstyperForEnhet(enhetId)).thenReturn(expectedTiltakskodeMapping)
        val result = tiltaksaktivitetService.hentTiltakstyper(enhetId)

        verify(brukertiltakRepository).hentTiltakstyperForEnhet(enhetId)

        assertEquals(expectedTiltakskodeMapping, result)
        assertThat(result.tiltak).hasSize(2)
        assertThat(result.tiltak["TILTAK1"]).isEqualTo("Tiltak 1")
        assertThat(result.tiltak["TILTAK2"]).isEqualTo("Tiltak 2")
    }
}