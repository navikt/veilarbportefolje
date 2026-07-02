package no.nav.pto.veilarbportefolje.aktiviteter.v1

import no.nav.common.types.identer.EnhetId
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakService
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class TiltaksAktivitetServiceTest {
    val brukertiltakRepository: BrukertiltakRepository = mock(BrukertiltakRepository::class.java)
    val tiltaksaktivitetService = TiltaksaktivitetService(brukertiltakRepository)

    @Test
    fun `skal hente tiltakstyper for enhet`() {
        // Arrange
        val enhetId = EnhetId("1234")
        val expectedTiltakskodeMapping = TiltakskodeMapping(
            mutableMapOf(
                "TILTAK1" to "Tiltak 1",
                "TILTAK2" to "Tiltak 2"
            )
        )
        `when`(brukertiltakRepository.hentTiltakstyperForEnhet(enhetId)).thenReturn(expectedTiltakskodeMapping)

        val result = tiltaksaktivitetService.hentTiltakstyper(enhetId)

        assert(result == expectedTiltakskodeMapping)
    }
}