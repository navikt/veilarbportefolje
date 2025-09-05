package no.nav.pto.veilarbportefolje.aap

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakResponseDto
import no.nav.pto.veilarbportefolje.domene.AktorClient
import no.nav.pto.veilarbportefolje.oppfolging.domene.OppfolgingMedStartdato
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.util.*


class AapServiceTest {

    private lateinit var aapService: AapService
    private val aapClient: AapClient = mock()
    private val aktorClient: AktorClient = mock()
    private val oppfolgingRepositoryV2: OppfolgingRepositoryV2 = mock()
    private val pdlIdentRepository: PdlIdentRepository = mock()
    private val aapRepository: AapRepository = mock()

    @BeforeEach
    fun setUp() {
        aapService = AapService(aapClient, aktorClient, oppfolgingRepositoryV2, pdlIdentRepository, aapRepository)
    }


    @Test
    fun `hentAapVedtakForOppfolgingPeriode filtrerer vedtak utenfor oppfolging`() {
        val fnr = "12345678910"
        val aktorId = AktorId("1234")
        val oppfolgingStartdato = LocalDate.of(2023, 1, 1)

        `when`(aktorClient.hentAktorId(Fnr.of(fnr))).thenReturn(aktorId)
        `when`(oppfolgingRepositoryV2.hentOppfolgingMedStartdato(aktorId)).thenReturn(
            Optional.of(OppfolgingMedStartdato(true, toTimestamp(oppfolgingStartdato) ))
        )

        val vedtakInnenfor = AapVedtakResponseDto.Vedtak(
            status = "LØPENDE",
            saksnummer = "S123",
            rettighetsType = "TYPE1",
            kildesystem = "KILDE1",
            opphorsAarsak = null,
            periode = AapVedtakResponseDto.Periode(
                fraOgMedDato = LocalDate.of(2023, 2, 1),
                tilOgMedDato = LocalDate.of(2023, 12, 31)
            )
        )
        val vedtakForTidlig = AapVedtakResponseDto.Vedtak(
            status = "LØPENDE",
            saksnummer = "S123",
            rettighetsType = "TYPE1",
            kildesystem = "KILDE1",
            opphorsAarsak = null,
            periode = AapVedtakResponseDto.Periode(
                fraOgMedDato = LocalDate.of(2022, 1, 1),
                tilOgMedDato = LocalDate.of(2022, 12, 31)
            )
        )

        val apiResponse = AapVedtakResponseDto(vedtak = listOf(vedtakInnenfor, vedtakForTidlig))
        `when`(aapClient.hentAapVedtak(anyString(), anyString(), anyString())).thenReturn(apiResponse)

        val resultat = aapService.hentSisteAapVedtakForOppfolgingPeriode(fnr)

        assertThat(resultat.vedtak).containsExactly(vedtakInnenfor)
    }


    @Test
    fun `aap periode før oppfolgingStartdato skal filtreres bort`() {
        val oppfolgingsStartdato = LocalDate.of(2024, 1, 1)
        val aapVedtakPeriode = AapVedtakResponseDto.Periode(
            fraOgMedDato = LocalDate.of(2023, 1, 1),
            tilOgMedDato = LocalDate.of(2023, 12, 31)
        )

        val resultat = aapService.filtrerAapKunIOppfolgingPeriode(oppfolgingsStartdato, aapVedtakPeriode)
        assertThat(resultat).isNull()
    }

    @Test
    fun `periode som overlapper oppfolgingStartdato skal beholdes`() {
        val oppfolgingsStartdato = LocalDate.of(2024, 1, 1)
        val aapVedtakPeriode = AapVedtakResponseDto.Periode(
            fraOgMedDato = LocalDate.of(2023, 12, 1),
            tilOgMedDato = LocalDate.of(2024, 6, 30)
        )

        val resultat = aapService.filtrerAapKunIOppfolgingPeriode(oppfolgingsStartdato, aapVedtakPeriode)

        assertThat(resultat).isEqualTo(aapVedtakPeriode)
    }

}
