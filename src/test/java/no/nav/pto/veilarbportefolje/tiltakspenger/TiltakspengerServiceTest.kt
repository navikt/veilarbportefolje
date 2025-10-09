package no.nav.pto.veilarbportefolje.tiltakspenger

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.aap.domene.YTELSE_KILDESYSTEM
import no.nav.pto.veilarbportefolje.aap.domene.YTELSE_MELDINGSTYPE
import no.nav.pto.veilarbportefolje.aap.domene.YTELSE_TYPE
import no.nav.pto.veilarbportefolje.aap.domene.YtelserKafkaDTO
import no.nav.pto.veilarbportefolje.domene.*
import no.nav.pto.veilarbportefolje.domene.value.NavKontor
import no.nav.pto.veilarbportefolje.domene.value.VeilederId
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerResponseDto
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNorskIdent
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
import java.util.*
import java.util.List
import java.util.concurrent.TimeUnit

class TiltakspengerServiceTest(
    @Autowired private val jdbcTemplate: JdbcTemplate,
    @Autowired private val tiltakspengerRespository: TiltakspengerRespository,
    @Autowired private val pdlIdentRepository: PdlIdentRepository,
    @Autowired private val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    @Autowired private val opensearchIndexerV2: OpensearchIndexerV2,
    @Autowired private val opensearchService: OpensearchService,
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
            oppfolgingRepositoryV2,
            pdlIdentRepository,
            aktorClient,
            opensearchIndexerV2
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
    fun `skal starte henting og lagring av tiltakspenger ved mottatt kafkamelding`() {
        // Given
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        pdlIdentRepository.upsertIdenter(identerBruker)
        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(tiltakspengerClient.hentTiltakspenger(anyString(), anyString(), any())).thenReturn(listOf(mockedVedtak))

        //When
        tiltakspengerService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMelding)
        val lagretMelding = tiltakspengerRespository.hentTiltakspenger(norskIdent.get())

        //Then
        assertThat(lagretMelding).isNotNull
    }

    @Test
    fun `skal ikke behandle kafkamelding når person ikke har tiltakspenger`() {
        // Given
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        pdlIdentRepository.upsertIdenter(identerBruker)

        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(tiltakspengerClient.hentTiltakspenger(anyString(), anyString(), any())).thenReturn(emptyList())

        // When
        tiltakspengerService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMelding)
        val lagretMelding = tiltakspengerRespository.hentTiltakspenger(norskIdent.get())

        // Then
        assertThat(lagretMelding).isNull()
    }

    @Test
    fun `skal behandle kafkamelding og slette data når person ikke har ytelsen men meldingstype er oppdater`() {
        // Given
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        pdlIdentRepository.upsertIdenter(identerBruker)
        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)

        // Opprett rad på bruker
        `when`(tiltakspengerClient.hentTiltakspenger(anyString(), anyString(), any())).thenReturn(listOf(mockedVedtak))
        tiltakspengerService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMelding)

        // Oppdatermelding på samme person uten ytelsen skal slette rad i db
        `when`(tiltakspengerClient.hentTiltakspenger(anyString(), anyString(), any())).thenReturn(emptyList())
        tiltakspengerService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMelding.copy(meldingstype = YTELSE_MELDINGSTYPE.OPPDATER))
        val lagretMelding = tiltakspengerRespository.hentTiltakspenger(norskIdent.get())

        // Then
        assertThat(lagretMelding).isNull()
    }

    @Test
    fun `skal ikke behandle kafkamelding når person ikke er under oppfølging`() {
        pdlIdentRepository.upsertIdenter(identerBruker)
        tiltakspengerService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMelding)
        val lagretMelding = tiltakspengerRespository.hentTiltakspenger(norskIdent.get())

        assertThat(lagretMelding).isNull()
    }

    @Test
    fun `hentTiltakspengerForOppfolgingPeriode filtrerer vedtak utenfor oppfolging`() {
        // Given
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

        // When
        val resultat = tiltakspengerService.hentSistePeriodeFraApi(norskIdent.get(), oppfolgingStartdato.toLocalDate())

        // Then
        assertThat(resultat).isEqualTo(vedtakInnenfor)
    }

    @Test
    fun `skal oppdatere til ny ident og slette gammel rad når kafkamedling med ny ident kommer`() {
        // Given
        val norskIdentHistorisk = randomNorskIdent()
        val identerBruker = List.of<PDLIdent>(
            PDLIdent(aktorId.get(), false, Gruppe.AKTORID),
            PDLIdent(norskIdent.get(), false, Gruppe.FOLKEREGISTERIDENT),
            PDLIdent(norskIdentHistorisk.get(), true, Gruppe.FOLKEREGISTERIDENT)
        )

        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        pdlIdentRepository.upsertIdenter(identerBruker)

        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(tiltakspengerClient.hentTiltakspenger(anyString(), anyString(), any())).thenReturn(listOf(mockedVedtak))

        // Lagre en rad på historisk ident, før kafkamelding med ny ident kommer
        tiltakspengerService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMelding.copy(personId = norskIdentHistorisk.get()))
        val lagretGammelIdent = tiltakspengerRespository.hentTiltakspenger(norskIdentHistorisk.get())
        assertThat(lagretGammelIdent).isNotNull()

        // Behandle kafkamelding med ny ident
        tiltakspengerService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMelding)
        val lagretGammelIdentEtterOppdatering = tiltakspengerRespository.hentTiltakspenger(norskIdentHistorisk.get())
        val lagretNyIdent = tiltakspengerRespository.hentTiltakspenger(norskIdent.get())

        // Then
        assertThat(lagretGammelIdentEtterOppdatering).isNull()
        assertThat(lagretNyIdent).isNotNull()
    }

    @Test
    fun `Tiltakspenger skal populere og filtrere riktig i opensearch når man har ytelsen`() {
        val aktorId = randomAktorId()
        setInitialState(aktorId, harTiltakspenger = true)
        val getResponse = opensearchTestClient.fetchDocument(aktorId)
        assertThat(getResponse.isExists).isTrue()

        val tiltakspengerRespons = getResponse.sourceAsMap["tiltakspenger"];

        assertThat(tiltakspengerRespons).isNotNull
        assertThat(tiltakspengerRespons).isEqualTo(true)

        val filtervalg = Filtervalg()
        filtervalg.setYtelseTiltakspenger(listOf(YtelseTiltakspenger.HAR_TILTAKSPENGER))
        filtervalg.setFerdigfilterListe(listOf())

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
            assertThat(responseBrukere.brukere.first().tiltakspenger).isNotNull()
        }
    }

    @Test
    fun `Tiltakspenger skal populere og filtrere riktig i opensearch når man ikke har ytelsen`() {
        val aktorId = randomAktorId()
        setInitialState(aktorId, harTiltakspenger = false)
        val getResponse = opensearchTestClient.fetchDocument(aktorId)
        assertThat(getResponse.isExists).isTrue()

        val tiltakspengerRespons = getResponse.sourceAsMap["tiltakspenger"];
        assertThat(tiltakspengerRespons).isEqualTo(false)

        val filtervalg = Filtervalg()
        filtervalg.setYtelseTiltakspenger(listOf(YtelseTiltakspenger.HAR_IKKE_TILTAKSPENGER))
        filtervalg.setFerdigfilterListe(listOf())

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
            assertThat(responseBrukere.brukere.first().tiltakspenger).isNull()
        }
    }

    @Test
    fun `Tiltakspenger returnere alle brukere med og uten ytelsen når begge filtrene er valgt `() {
        val aktorId1 = randomAktorId()
        val aktorId2 = randomAktorId()
        setInitialState(aktorId1, harTiltakspenger = false)
        setInitialState(aktorId2, harTiltakspenger = true)

        val filtervalg = Filtervalg()
        filtervalg.setYtelseTiltakspenger(listOf(YtelseTiltakspenger.HAR_TILTAKSPENGER, YtelseTiltakspenger.HAR_IKKE_TILTAKSPENGER))
        filtervalg.setFerdigfilterListe(listOf())

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

            assertThat(responseBrukere.antall).isEqualTo(2)
            assertThat(responseBrukere.brukere.filter { it.tiltakspenger != null }.size).isEqualTo(1)
            assertThat(responseBrukere.brukere.filter { it.tiltakspenger == null }.size).isEqualTo(1)
        }
    }

    private fun setInitialState(aktorId: AktorId, harTiltakspenger: Boolean) {
        testDataClient.lagreBrukerUnderOppfolging(aktorId, norskIdent, navKontor, veilederId)
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        populateOpensearch(navKontor, veilederId, aktorId.get())

        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        val mockedRespons = if (harTiltakspenger) listOf(mockedVedtak) else emptyList()
        `when`(tiltakspengerClient.hentTiltakspenger(anyString(), anyString(), any())).thenReturn(mockedRespons)

        tiltakspengerService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMelding.copy(personId = norskIdent.toString()))

    }

}

val mockedYtelseKafkaMelding = YtelserKafkaDTO(
    personId = "10108000000",
    meldingstype = YTELSE_MELDINGSTYPE.OPPRETT,
    ytelsestype = YTELSE_TYPE.TILTAKSPENGER,
    kildesystem = YTELSE_KILDESYSTEM.TP
)

val mockedVedtak = TiltakspengerResponseDto(
    sakId = "S123",
    rettighet = TiltakspengerRettighet.TILTAKSPENGER,
    kilde = "TP",
    fom = LocalDate.now().minusMonths(1),
    tom = LocalDate.now().plusMonths(1)
)
