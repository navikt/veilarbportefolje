package no.nav.pto.veilarbportefolje.ungdomsprogram

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.client.AktorClient
import no.nav.pto.veilarbportefolje.domene.*
import no.nav.pto.veilarbportefolje.domene.filtervalg.YtelseAapKelvin
import no.nav.pto.veilarbportefolje.domene.filtervalg.YtelseUngdomsprogram
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.UNGDOMSPROGRAM
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.UNGDOMSPROGRAM_FRA_OG_MED
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.UNGDOMSPROGRAM_HAR_FORLENGET_PERIODE
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.UNGDOMSPROGRAM_MAKSDATO
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.UNGDOMSPROGRAM_TIL_OG_MED
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe
import no.nav.pto.veilarbportefolje.ungdomsprogram.dto.Deltakelse
import no.nav.pto.veilarbportefolje.ungdomsprogram.dto.Periode
import no.nav.pto.veilarbportefolje.ungdomsprogram.dto.UngdomsprogramResponseDto
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
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
import java.util.*
import java.util.concurrent.TimeUnit

class UngdomsprogramServiceTest(
    @param:Autowired private val jdbcTemplate: JdbcTemplate,
    @param:Autowired private val pdlIdentRepository: PdlIdentRepository,
    @param:Autowired private val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    @param:Autowired private val undomsprogramRepository: UngdomsprogramRepository,
    @param:Autowired private val opensearchIndexerPaDatafelt: OpensearchIndexerPaDatafelt,
    @param:Autowired private val opensearchService: OpensearchService,
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
            undomsprogramRepository,
            opensearchIndexerPaDatafelt
        )
    }

    val norskIdent = Fnr.ofValidFnr("10108000000")
    val aktorId = AktorId.of("12345")
    val identerBruker = listOf(
        PDLIdent(aktorId.get(), false, Gruppe.AKTORID),
        PDLIdent(norskIdent.get(), false, Gruppe.FOLKEREGISTERIDENT)
    )

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

    @Test
    fun `hentUngdomsprogramForAlleBrukere skal ikke lagre personer som ikke er under oppfølging`() {
        // Given
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        pdlIdentRepository.upsertIdenter(identerBruker)
        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(ungdomsprogramClient.hentAlleMedUngdomsprogram()).thenReturn(mockedPeriode)

        //When
        ungdomsprogramService.hentUngdomsprogramForAlleBrukere()
        val lagretBruker = undomsprogramRepository.hentUngdomsprogram(mockedPeriode.deltakelser[0].deltakerIdent)
        val brukerIkkeUnderOppfolging =
            undomsprogramRepository.hentUngdomsprogram(mockedPeriode.deltakelser[1].deltakerIdent)

        //Then
        assertThat(lagretBruker).isNotNull
        assertThat(lagretBruker!!.fraOgMed).isEqualTo(LocalDate.now().minusMonths(1))
        assertThat(lagretBruker.tilOgMed).isEqualTo(mockedPeriode.deltakelser[0].periode.tilOgMed)
        assertThat(lagretBruker.harForlengetPeriode).isEqualTo(mockedPeriode.deltakelser[0].periode.harForlengetPeriode)
        assertThat(brukerIkkeUnderOppfolging).isNull()
    }

    @Test
    fun `hentUngdomsprogramForAlleBrukere skal ikke lagre personer som ikke har ytelsen i oppfølgingsperioden`() {
        // Given
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        pdlIdentRepository.upsertIdenter(identerBruker)
        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(ungdomsprogramClient.hentAlleMedUngdomsprogram()).thenReturn(mockedPeriodeFortid)

        //When
        ungdomsprogramService.hentUngdomsprogramForAlleBrukere()
        val lagretMelding = undomsprogramRepository.hentUngdomsprogram(norskIdent.get())
        //Then
        assertThat(lagretMelding).isNull()

    }

    @Test
    fun `Ungdomsprogram skal populere og filtrere riktig i opensearch når man har ytelsen`() {
        val aktorId = randomAktorId()
        setInitialState(aktorId)
        val getResponse = opensearchTestClient.fetchDocument(aktorId)
        assertThat(getResponse.isExists).isTrue()

        val ungdomsprogramMap = getResponse.sourceAsMap[UNGDOMSPROGRAM] as Map<*, *>
        val fraOgMed = ungdomsprogramMap[UNGDOMSPROGRAM_FRA_OG_MED]
        val tilOgMed = ungdomsprogramMap[UNGDOMSPROGRAM_TIL_OG_MED]
        val maksdato = ungdomsprogramMap[UNGDOMSPROGRAM_MAKSDATO]
        val harForlengelse = ungdomsprogramMap[UNGDOMSPROGRAM_HAR_FORLENGET_PERIODE]

        assertThat(ungdomsprogramMap).isNotNull
        assertThat(fraOgMed).isEqualTo(LocalDate.now().minusMonths(1).toString())
        assertThat(tilOgMed).isEqualTo(LocalDate.now().plusMonths(1).toString())
        assertThat(maksdato).isEqualTo(LocalDate.now().plusMonths(12).toString())
        assertThat(harForlengelse).isEqualTo(false)

        val filtervalg = getFiltervalgDefaults().copy(
            ytelseUngdomsprogram = listOf(YtelseUngdomsprogram.HAR_UNGDOMSPROGRAM)
        )

        verifiserAsynkront(
            2, TimeUnit.SECONDS
        ) {
            val responseBrukere: BrukereMedAntall = opensearchService.hentBrukere(
                "1123",
                Optional.empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filtervalg,
                null,
                null
            )

            assertThat(responseBrukere.antall).isEqualTo(1)
            assertThat(responseBrukere.brukere.first().ytelser.ungdomsprogram).isNotNull()
        }
    }


    @Test
    fun `skal populere og filtrere riktig i opensearch ved sletting av ungdomsprogram `() {
        val aktorId = randomAktorId()
        // Legg til bruker med ungdomsprogram og oppdater opensearch
        setInitialState(aktorId)
        val getResponse = opensearchTestClient.fetchDocument(aktorId)
        val responsMedUngdomsprogram = getResponse.sourceAsMap[UNGDOMSPROGRAM];
        assertThat(responsMedUngdomsprogram).isNotNull

        //Fjern ungdomsprogram og oppdater opensearch
        `when`(ungdomsprogramClient.hentAlleMedUngdomsprogram()).thenReturn(mockedPeriodeFortid)
        populateOpensearch(NavKontor.of("1123"), VeilederId.of("Z12345"), aktorId.get())
        ungdomsprogramService.hentUngdomsprogramForAlleBrukere()

        val getResponse2 = opensearchTestClient.fetchDocument(aktorId)
        val responsUtenUngdomsprogram = getResponse2.sourceAsMap[UNGDOMSPROGRAM];
        assertThat(responsUtenUngdomsprogram).isNull()

        val filtervalg = getFiltervalgDefaults().copy(
            ytelseUngdomsprogram = listOf(YtelseUngdomsprogram.HAR_UNGDOMSPROGRAM)
        )

        verifiserAsynkront(
            2, TimeUnit.SECONDS
        ) {
            val responseBrukere: BrukereMedAntall = opensearchService.hentBrukere(
                "1123",
                Optional.empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filtervalg,
                null,
                null
            )

            assertThat(responseBrukere.antall).isEqualTo(0)
        }
    }

    private fun setInitialState(aktorId: AktorId) {
        val navKontor = NavKontor.of("1123")
        val veilederId = VeilederId.of("Z12345")
        testDataClient.lagreBrukerUnderOppfolging(aktorId, norskIdent, navKontor, veilederId)
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(24))
        populateOpensearch(navKontor, veilederId, aktorId.get())

        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(ungdomsprogramClient.hentAlleMedUngdomsprogram()).thenReturn(mockedPeriode)
        ungdomsprogramService.hentUngdomsprogramForAlleBrukere()

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
                fraOgMed = LocalDate.now().minusMonths(2),
                tilOgMed = LocalDate.now().plusMonths(2),
                harForlengetPeriode = false,
                periodeMaksDato = LocalDate.now().plusMonths(12)
            )
        ),
        Deltakelse(
            deltakerIdent = "30108000000",
            periode = Periode(
                fraOgMed = LocalDate.now().minusMonths(3),
                tilOgMed = LocalDate.now().plusMonths(3),
                harForlengetPeriode = false,
                periodeMaksDato = LocalDate.now().plusMonths(12)
            )
        )
    )
)

val mockedPeriodeFortid = UngdomsprogramResponseDto(
    deltakelser = listOf(
        Deltakelse(
            deltakerIdent = "10108000000",
            periode = Periode(
                fraOgMed = LocalDate.now().minusMonths(12),
                tilOgMed = LocalDate.now().minusMonths(11),
                harForlengetPeriode = false,
                periodeMaksDato = LocalDate.now().plusMonths(12)
            )
        )
    )
)
