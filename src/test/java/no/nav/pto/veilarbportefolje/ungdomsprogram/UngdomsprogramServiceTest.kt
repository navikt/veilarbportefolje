package no.nav.pto.veilarbportefolje.ungdomsprogram

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.client.AktorClient
import no.nav.pto.veilarbportefolje.domene.NavKontor
import no.nav.pto.veilarbportefolje.domene.VeilederId
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe
import no.nav.pto.veilarbportefolje.ungdomsprogram.dto.Deltakelse
import no.nav.pto.veilarbportefolje.ungdomsprogram.dto.Periode
import no.nav.pto.veilarbportefolje.ungdomsprogram.dto.UngdomsprogramResponseDto
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.List

class UngdomsprogramServiceTest(
    @param:Autowired private val jdbcTemplate: JdbcTemplate,
    @param:Autowired private val pdlIdentRepository: PdlIdentRepository,
    @param:Autowired private val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    @param:Autowired private val undomsprogramRepository: UngdomsprogramRepository
) : EndToEndTest() {

    private lateinit var ungdomsprogramService: UngdomsprogramService
    private val ungdomsprogramClient: UngdomsprogramClient = mock()
    private val aktorClient: AktorClient = mock()

    @BeforeEach
    fun setUp() {
        resetDatabase()
        initializeService()
    }

    private fun resetDatabase() {
        listOf("YTELSER_UNGDOMSPROGRAM", "oppfolging_data", "bruker_identer")
            .forEach { jdbcTemplate.execute("TRUNCATE TABLE $it") }
    }

    private fun initializeService() {
        ungdomsprogramService = UngdomsprogramService(
            ungdomsprogramClient,
            oppfolgingRepositoryV2,
            pdlIdentRepository,
            aktorClient,
            undomsprogramRepository
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
    fun `skal starte henting og lagring av ungdomsprogram ved chron-job`() {
        // Given
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        pdlIdentRepository.upsertIdenter(identerBruker)
        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(ungdomsprogramClient.hentAlleMedUngdomsprogram()).thenReturn(mockedPeriode)

        //When
        ungdomsprogramService.hentUngdomsprogramForAlleBrukere()
        val lagretMelding = undomsprogramRepository.hentUngdomsprogram(norskIdent.get())

        //Then
        assertThat(lagretMelding).isNotNull
    }

}

val mockedPeriode = UngdomsprogramResponseDto(
    deltakelser = listOf(
        Deltakelse(
            deltakerIdent = "10108000000",
            periode = Periode(
                fraOgMed = LocalDate.now().minusMonths(1),
                tilOgMed = LocalDate.now().plusMonths(1),
                harForlengetPeriode = false,
                periodeMaksDato = LocalDate.now().plusMonths(12)
            )
        ),
        Deltakelse(
            deltakerIdent = "20108000000",
            periode = Periode(
                fraOgMed = LocalDate.now().minusMonths(1),
                tilOgMed = LocalDate.now().plusMonths(1),
                harForlengetPeriode = false,
                periodeMaksDato = LocalDate.now().plusMonths(12)
            )
        ),
        Deltakelse(
            deltakerIdent = "30108000000",
            periode = Periode(
                fraOgMed = LocalDate.now().minusMonths(1),
                tilOgMed = LocalDate.now().plusMonths(1),
                harForlengetPeriode = false,
                periodeMaksDato = LocalDate.now().plusMonths(12)
            )
        )
    )
)
