package no.nav.pto.veilarbportefolje.tiltakspenger

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.domene.AktorClient
import no.nav.pto.veilarbportefolje.domene.value.NavKontor
import no.nav.pto.veilarbportefolje.domene.value.VeilederId
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingService
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerResponseDto
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.List

class TiltakspengerServiceTest(
    @Autowired private val jdbcTemplate: JdbcTemplate,
    @Autowired private val tiltakspengerRespository: TiltakspengerRespository,
    @Autowired private val pdlIdentRepository: PdlIdentRepository,
    @Autowired private val oppfolgingService: OppfolgingService,
) : EndToEndTest() {

    private lateinit var tiltakspengerService: TiltakspengerService
    private val tiltakspengerClient: TiltakspengerClient = mock()
    private val aktorClient: AktorClient = mock()


    @BeforeEach
    fun setUp() {
        resetDatabase()
        initializeService()
    }

    private fun resetDatabase() {
        listOf("YTELSER_TILTAKSPENGER", "oppfolging_data", "bruker_identer")
            .forEach { jdbcTemplate.execute("TRUNCATE TABLE $it") }
    }

    private fun initializeService() {
        tiltakspengerService = TiltakspengerService(
            tiltakspengerClient,
            tiltakspengerRespository,
            oppfolgingService,
            pdlIdentRepository,
            aktorClient
        )
    }

    val norskIdent = Fnr.ofValidFnr("10108000000")
    val aktorId = AktorId.of("12345")
    val identerBruker = List.of<PDLIdent>(
        PDLIdent(aktorId.get(), false, Gruppe.AKTORID),
        PDLIdent(norskIdent.get(), false, Gruppe.FOLKEREGISTERIDENT)
    )
    val navKontor = NavKontor.of("1123")
    val veilederId = VeilederId.of("Z12345")

    @Test
    fun `hentTiltakspengerForOppfolgingPeriode filtrerer vedtak utenfor oppfolging`() {
        val oppfolgingStartdato = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Oslo"))
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, oppfolgingStartdato)
        pdlIdentRepository.upsertIdenter(identerBruker)

        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)

        val vedtakInnenfor = mockedVedtak.copy(
            fom = LocalDate.of(2022, 2, 1),
            tom = LocalDate.of(2023, 1, 1)
        )

        val vedtakForTidlig = mockedVedtak.copy(
            fom = LocalDate.of(2022, 1, 1),
            tom = LocalDate.of(2022, 12, 31)
        )

        val apiResponse = listOf(vedtakInnenfor, vedtakForTidlig)
        `when`(tiltakspengerClient.hentTiltakspenger(anyString(), anyString(), any())).thenReturn(apiResponse)

        val resultat = tiltakspengerService.hentSistePeriodeFraApi(norskIdent.get(), oppfolgingStartdato.toLocalDate())
        assertThat(resultat).isEqualTo(vedtakInnenfor)
    }

}

val mockedVedtak = TiltakspengerResponseDto(
    sakId = "S123",
    rettighet = TiltakspengerRettighet.TILTAKSPENGER,
    kilde = "tp",
    fom = LocalDate.now().minusMonths(1),
    tom = LocalDate.now().plusMonths(1)
)
