package no.nav.pto.veilarbportefolje.oppfolging

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.domene.VeilederId
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriService
import no.nav.pto.veilarbportefolje.huskelapp.HuskelappService
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt
import no.nav.pto.veilarbportefolje.oppfolging.dto.VeilederTilordnetDTO
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.ZonedDateTime
import java.util.Optional
import java.util.stream.Stream

class VeilederTilordnetServiceUnitTest {

    private lateinit var oppfolgingRepositoryV2: OppfolgingRepositoryV2
    private lateinit var huskelappService: HuskelappService
    private lateinit var fargekategoriService: FargekategoriService
    private lateinit var opensearchIndexerPaDatafelt: OpensearchIndexerPaDatafelt
    private lateinit var pdlIdentRepository: PdlIdentRepository
    private lateinit var service: VeilederTilordnetService

    private val aktoerId = AktorId.of("11111111111")
    private val veilederId = VeilederId.of("Z999999")
    private val tidspunkt = ZonedDateTime.now()

    @BeforeEach
    fun setUp() {
        oppfolgingRepositoryV2 = mock(OppfolgingRepositoryV2::class.java)
        huskelappService = mock(HuskelappService::class.java)
        fargekategoriService = mock(FargekategoriService::class.java)
        opensearchIndexerPaDatafelt = mock(OpensearchIndexerPaDatafelt::class.java)
        pdlIdentRepository = mock(PdlIdentRepository::class.java)

        service = VeilederTilordnetService(
            oppfolgingRepositoryV2,
            huskelappService,
            fargekategoriService,
            opensearchIndexerPaDatafelt,
            pdlIdentRepository,
        )
    }

    @Test
    fun `skal kaste ved null DTO`() {
        assertThatThrownBy { service.behandleKafkaMeldingLogikk(null) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("VeilederTilordnetDTO var null")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullKombinasjoner")
    fun `skal ikke kaste NPE for null-kombinasjoner i DTO`(
        dto: VeilederTilordnetDTO,
    ) {
        `when`(pdlIdentRepository.hentFnrForAktivBruker(aktoerId)).thenReturn(null)

        service.behandleKafkaMeldingLogikk(dto)

        verify(oppfolgingRepositoryV2).settVeileder(aktoerId, dto.veilederId)
        verify(oppfolgingRepositoryV2).settTildeltTidspunkt(aktoerId, dto.tilordnetTidspunkt)
    }

    @Test
    fun `skal deaktivere huskelapp naar bruker har byttet NAV-kontor`() {
        val fnr = Fnr.of("01010112345")
        val dto = VeilederTilordnetDTO(aktoerId, veilederId, tidspunkt)
        `when`(pdlIdentRepository.hentFnrForAktivBruker(aktoerId)).thenReturn(fnr)
        `when`(huskelappService.brukerHarHuskelappPaForrigeNavkontor(aktoerId, Optional.of(fnr))).thenReturn(true)

        service.behandleKafkaMeldingLogikk(dto)

        verify(huskelappService).deaktivereAlleHuskelapperPaaBruker(aktoerId, Optional.of(fnr))
    }

    @Test
    fun `skal ikke deaktivere huskelapp naar bruker ikke har byttet NAV-kontor`() {
        val fnr = Fnr.of("01010112345")
        val dto = VeilederTilordnetDTO(aktoerId, veilederId, tidspunkt)
        `when`(pdlIdentRepository.hentFnrForAktivBruker(aktoerId)).thenReturn(fnr)
        `when`(huskelappService.brukerHarHuskelappPaForrigeNavkontor(aktoerId, Optional.of(fnr))).thenReturn(false)

        service.behandleKafkaMeldingLogikk(dto)

        verify(huskelappService, never()).deaktivereAlleHuskelapperPaaBruker(aktoerId, Optional.of(fnr))
    }

    @Test
    fun `skal slette fargekategori naar bruker har byttet NAV-kontor`() {
        val fnr = Fnr.of("01010112345")
        val dto = VeilederTilordnetDTO(aktoerId, veilederId, tidspunkt)
        `when`(pdlIdentRepository.hentFnrForAktivBruker(aktoerId)).thenReturn(fnr)
        `when`(fargekategoriService.brukerHarFargekategoriPaForrigeNavkontor(aktoerId, Optional.of(fnr))).thenReturn(true)

        service.behandleKafkaMeldingLogikk(dto)

        verify(fargekategoriService).slettFargekategoriPaaBruker(aktoerId, Optional.of(fnr))
    }

    @Test
    fun `skal ikke slette fargekategori naar bruker ikke har byttet NAV-kontor`() {
        val fnr = Fnr.of("01010112345")
        val dto = VeilederTilordnetDTO(aktoerId, veilederId, tidspunkt)
        `when`(pdlIdentRepository.hentFnrForAktivBruker(aktoerId)).thenReturn(fnr)
        `when`(fargekategoriService.brukerHarFargekategoriPaForrigeNavkontor(aktoerId, Optional.of(fnr))).thenReturn(false)

        service.behandleKafkaMeldingLogikk(dto)

        verify(fargekategoriService, never()).slettFargekategoriPaaBruker(aktoerId, Optional.of(fnr))
    }

    companion object {
        @JvmStatic
        fun nullKombinasjoner(): Stream<Arguments> {
            val aktoerId = AktorId.of("11111111111")
            val veilederId = VeilederId.of("Z999999")
            val tidspunkt = ZonedDateTime.now()
            return Stream.of(
                Arguments.of("veileder fjernet (null veilederId)", VeilederTilordnetDTO(aktoerId, null, tidspunkt)),
                Arguments.of("mangler tidspunkt (null tilordnetTidspunkt)", VeilederTilordnetDTO(aktoerId, veilederId, null)),
                Arguments.of("begge felt null", VeilederTilordnetDTO(aktoerId, null, null)),
            )
        }
    }
}
