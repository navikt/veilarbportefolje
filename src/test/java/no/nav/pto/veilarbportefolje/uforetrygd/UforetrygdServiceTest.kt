package no.nav.pto.veilarbportefolje.uforetrygd

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.client.AktorClient
import no.nav.pto.veilarbportefolje.domene.*
import no.nav.pto.veilarbportefolje.domene.filtervalg.YtelseUforetrygd
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe
import no.nav.pto.veilarbportefolje.uforetrygd.dto.UforetrygdResponseDto
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_KILDESYSTEM
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_MELDINGSTYPE
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_TYPE
import no.nav.pto.veilarbportefolje.ytelserkafka.YtelserKafkaDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit

class UforetrygdServiceTest(
    @param:Autowired private val jdbcTemplate: JdbcTemplate,
    @param:Autowired private val uforetrygdRepository: UforetrygdRepository,
    @param:Autowired private val pdlIdentRepository: PdlIdentRepository,
    @param:Autowired private val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    @param:Autowired private val opensearchIndexerPaDatafelt: OpensearchIndexerPaDatafelt,
    @param:Autowired private val opensearchService: OpensearchService
) : EndToEndTest() {

    private lateinit var uforetrygdService: UforetrygdService
    private val uforetrygdClient: UforetrygdClient = mock()
    private val aktorClient: AktorClient = mock()

    @BeforeEach
    fun setUp() {
        resetDatabase()
        initializeService()
    }

    private fun resetDatabase() {
        listOf("YTELSER_UFORETRYGD", "oppfolging_data", "bruker_identer")
            .forEach { jdbcTemplate.execute("TRUNCATE TABLE $it") }
    }

    private fun initializeService() {
        uforetrygdService = UforetrygdService(
            oppfolgingRepositoryV2 = oppfolgingRepositoryV2,
            pdlIdentRepository = pdlIdentRepository,
            aktorClient = aktorClient,
            uforetrygdRepository = uforetrygdRepository,
            uforetrygdClient = uforetrygdClient,
            opensearchIndexerPaDatafelt = opensearchIndexerPaDatafelt
        )
    }

    val norskIdent = Fnr.ofValidFnr("10108000000")
    val aktorId = AktorId.of("12345")
    val identerBruker = listOf(
        PDLIdent(aktorId.get(), false, Gruppe.AKTORID),
        PDLIdent(norskIdent.get(), false, Gruppe.FOLKEREGISTERIDENT)
    )
    val navKontor = NavKontor.of("1123")
    val veilederId = VeilederId.of("Z12345")

    @Test
    fun `skal starte henting og lagring av uføretrygd ved mottatt kafkamelding`() {
        // Given
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        pdlIdentRepository.upsertIdenter(identerBruker)
        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(uforetrygdClient.hentUforetrygd(anyString())).thenReturn(mockedUforetrygdResponseDto)

        //When
        uforetrygdService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMeldingOpprett)
        val lagretMelding = uforetrygdRepository.hentUforetrygd(norskIdent.get())

        //Then
        assertThat(lagretMelding).isNotNull
    }

    @Test
    fun `skal ikke behandle kafkamelding når person ikke har uføretrygd`() {
        // Given
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        pdlIdentRepository.upsertIdenter(identerBruker)

        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(uforetrygdClient.hentUforetrygd(anyString())).thenReturn(null)

        // When
        uforetrygdService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMeldingOpprett)
        val lagretMelding = uforetrygdRepository.hentUforetrygd(norskIdent.get())

        // Then
        assertThat(lagretMelding).isNull()
    }

    @Test
    fun `skal ikke behandle kafkamelding når person ikke er under oppfølging`() {
        pdlIdentRepository.upsertIdenter(identerBruker)

        uforetrygdService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMeldingOpprett)
        val lagretMelding = uforetrygdRepository.hentUforetrygd(norskIdent.get())

        assertThat(lagretMelding).isNull()
    }

    @Test
    fun `skal slette uføretrygd når mottatt kafkamelding om sletting`() {
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        pdlIdentRepository.upsertIdenter(identerBruker)

        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(uforetrygdClient.hentUforetrygd(anyString())).thenReturn(mockedUforetrygdResponseDto)

        uforetrygdService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMeldingOpprett)
        uforetrygdService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMeldingSlett)
        val lagretMelding = uforetrygdRepository.hentUforetrygd(norskIdent.get())

        assertThat(lagretMelding).isNull()
    }


    @Test
    fun `Uføretrygd skal populere og filtrere riktig i opensearch når man har ytelsen`() {
        val aktorId = randomAktorId()
        setInitialState(aktorId)
        val getResponse = opensearchTestClient.fetchDocument(aktorId)
        assertThat(getResponse.isExists).isTrue()

        val uforetrygdMap = getResponse.sourceAsMap[DatafeltKeys.Ytelser.UFORETRYGD] as Map<*, *>
        val virkningsdato = uforetrygdMap[DatafeltKeys.Ytelser.UFORETRYGD_VIRKNINGSDATO]
        val uforegrad = uforetrygdMap[DatafeltKeys.Ytelser.UFORETRYGD_UFOREGRAD]

        assertThat(uforetrygdMap).isNotNull
        assertThat(virkningsdato).isEqualTo("2024-01-01")
        assertThat(uforegrad).isEqualTo(50)

        val filtervalg = getFiltervalgDefaults().copy(
            ytelseUforetrygd = listOf(YtelseUforetrygd.HAR_UFORETRYGD)
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
            assertThat(responseBrukere.brukere.first().ytelser.uforetrygd).isNotNull()
        }
    }

    private fun setInitialState(aktorId: AktorId) {
        testDataClient.lagreBrukerUnderOppfolging(aktorId, norskIdent, navKontor, veilederId)
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        populateOpensearch(navKontor, veilederId, aktorId.get())

        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        val mockedRespons = mockedUforetrygdResponseDto
        `when`(uforetrygdClient.hentUforetrygd(anyString())).thenReturn(mockedRespons)

        uforetrygdService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMeldingOpprett.copy(personId = norskIdent.toString()))

    }

}


val mockedYtelseKafkaMeldingOpprett = YtelserKafkaDTO(
    personId = "10108000000",
    meldingstype = YTELSE_MELDINGSTYPE.OPPRETT,
    ytelsestype = YTELSE_TYPE.UFORETRYGD,
    kildesystem = YTELSE_KILDESYSTEM.PESYS
)

val mockedYtelseKafkaMeldingSlett = YtelserKafkaDTO(
    personId = "10108000000",
    meldingstype = YTELSE_MELDINGSTYPE.SLETT,
    ytelsestype = YTELSE_TYPE.UFORETRYGD,
    kildesystem = YTELSE_KILDESYSTEM.PESYS
)

val mockedUforetrygdResponseDto = UforetrygdResponseDto(
    uføregrad = 50,
    virkningsdato = java.time.LocalDate.of(2024, 1, 1)
)
