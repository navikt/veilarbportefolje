package no.nav.pto.veilarbportefolje.aap

import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakResponseDto
import no.nav.pto.veilarbportefolje.domene.AktorClient
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import org.junit.jupiter.api.Test
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.mock


class AapServiceTest {

    private lateinit var aapService: AapService
    private val aapClient: AapClient = mock()
    private val aktorClient: AktorClient = mock()
    private val oppfolgingRepositoryV2: OppfolgingRepositoryV2 = mock()

    @BeforeEach
    fun setUp() {
        aapService = AapService(aapClient, aktorClient, oppfolgingRepositoryV2)
    }


    @Test
    fun `aap periode før oppfolgingStartdato skal filtreres bort`() {
        val oppfolgingsStartdato = LocalDate.of(2024, 1, 1)
        val periode = AapVedtakResponseDto.Periode(
            fraOgMedDato = LocalDate.of(2023, 1, 1),
            tilOgMedDato = LocalDate.of(2023, 12, 31)
        )

        val resultat = aapService.filtrerAapKunIOppfolgingPeriode(oppfolgingsStartdato, periode)
        assertThat(resultat).isNull()
    }

    @Test
    fun `periode som overlapper oppfolgingStartdato skal beholdes`() {
        val oppfolgingsStartdato = LocalDate.of(2024, 1, 1)
        val periode = AapVedtakResponseDto.Periode(
            fraOgMedDato = LocalDate.of(2023, 12, 1),
            tilOgMedDato = LocalDate.of(2024, 6, 30)
        )

        val resultat = aapService.filtrerAapKunIOppfolgingPeriode(oppfolgingsStartdato, periode)

        assertThat(resultat).isEqualTo(periode)
    }

}
