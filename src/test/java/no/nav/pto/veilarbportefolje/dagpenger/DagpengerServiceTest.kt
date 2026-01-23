package no.nav.pto.veilarbportefolje.dagpenger

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.client.AktorClient
import no.nav.pto.veilarbportefolje.dagpenger.domene.DagpengerRettighetstype
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerBeregningerResponseDto
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerPeriodeDto
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerPerioderResponseDto
import no.nav.pto.veilarbportefolje.domene.NavKontor
import no.nav.pto.veilarbportefolje.domene.VeilederId
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNorskIdent
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DagpengerServiceTest(
    @param:Autowired private val jdbcTemplate: JdbcTemplate,
    @param:Autowired private val dagpengerRepository: DagpengerRepository,
    @param:Autowired private val pdlIdentRepository: PdlIdentRepository,
    @param:Autowired private val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    @param:Autowired private val opensearchIndexerPaDatafelt: OpensearchIndexerPaDatafelt,
    @param:Autowired private val opensearchService: OpensearchService,
) : EndToEndTest() {

    private lateinit var dagpengerService: DagpengerService
    private val dagpengerClient: DagpengerClient = mock()
    private val aktorClient: AktorClient = mock()


    @BeforeEach
    fun setUp() {
        resetDatabase()
        initializeService()
    }

    private fun resetDatabase() {
        listOf("YTELSER_DAGPENGER", "oppfolging_data", "bruker_identer")
            .forEach { jdbcTemplate.execute("TRUNCATE TABLE $it") }
    }

    private fun initializeService() {
        dagpengerService = DagpengerService(
            dagpengerClient,
            dagpengerRepository,
            oppfolgingRepositoryV2,
            pdlIdentRepository,
            aktorClient,
            opensearchIndexerPaDatafelt
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
    fun `skal starte henting og lagring av dagpenger ved mottatt kafkamelding`() {
        // Given
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        pdlIdentRepository.upsertIdenter(identerBruker)
        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(dagpengerClient.hentDagpengerPerioder(anyString(), anyString(), any())).thenReturn(mockedPerioder)
        `when`(dagpengerClient.hentDagpengerBeregninger(anyString(), anyString(), any())).thenReturn(mockedBeregning)

        //When
        dagpengerService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMelding)
        val lagretMelding = dagpengerRepository.hentDagpenger(norskIdent.get())

        //Then
        assertThat(lagretMelding).isNotNull
        assertThat(lagretMelding!!.rettighetstype).isEqualTo(DagpengerRettighetstype.DAGPENGER_ARBEIDSSOKER_ORDINAER)
        assertThat(lagretMelding.fom).isEqualTo(LocalDate.of(2023, 1, 1))
        assertThat(lagretMelding.tom).isNull()
        assertThat(lagretMelding.antallDagerResterende).isEqualTo(118)
        assertThat(lagretMelding.datoAntallDagerBleBeregnet).isEqualTo(LocalDate.of(2026, 1, 3))

    }

    @Test
    fun `skal ikke behandle kafkamelding når person ikke har dagpenger`() {
        // Given
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        pdlIdentRepository.upsertIdenter(identerBruker)

        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(dagpengerClient.hentDagpengerPerioder(anyString(), anyString(), any())).thenReturn(
            mockedPerioder.copy(
                perioder = emptyList()
            )
        )
        `when`(dagpengerClient.hentDagpengerBeregninger(anyString(), anyString(), any())).thenReturn(emptyList())


        // When
        dagpengerService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMelding)
        val lagretMelding = dagpengerRepository.hentDagpenger(norskIdent.get())

        // Then
        assertThat(lagretMelding).isNull()
    }

    @Test
    fun `skal ikke behandle kafkamelding når person ikke er under oppfølging`() {
        pdlIdentRepository.upsertIdenter(identerBruker)
        dagpengerService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMelding)

        val lagretMelding = dagpengerRepository.hentDagpenger(norskIdent.get())
        assertThat(lagretMelding).isNull()
    }

    @Test
    fun `hentDagpengerForOppfolgingPeriode filtrerer perioder utenfor oppfolging og fra arena`() {
        // Given
        val oppfolgingStartdato = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Oslo"))
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, oppfolgingStartdato)
        pdlIdentRepository.upsertIdenter(identerBruker)

        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)

        val perioderInnenfor = mockedPerioder.copy(
            perioder = listOf(
                periodeTomErNull,
                periodeTomErIkkePassert,
                periodeTomErIkkePassert
            )
        )

        val periodeUtenforOgArena = mockedPerioder.copy(
            perioder = listOf(
                periodeKildeErArena,
                periodeErUtenforOppfølging
            )
        )

        // utenfor oppfølging eller arena skal filtreres bort
        `when`(dagpengerClient.hentDagpengerPerioder(anyString(), anyString(), any())).thenReturn(periodeUtenforOgArena)
        val resultat = dagpengerService.hentSistePeriodeFraApi(norskIdent.get(), oppfolgingStartdato.toLocalDate())
        assertThat(resultat).isNull()

        // innenfor oppfølging skal beholdes, og periode med åpen til og med dato skal prioriteres
        `when`(dagpengerClient.hentDagpengerPerioder(anyString(), anyString(), any())).thenReturn(perioderInnenfor)
        val resultat2 = dagpengerService.hentSistePeriodeFraApi(norskIdent.get(), oppfolgingStartdato.toLocalDate())
        assertThat(resultat2).isNotNull
        assertThat(resultat2).isEqualTo(periodeTomErNull)
    }

    @Test
    fun `hentAntallResterendeDagerFraApi skal hente ut antall dager fra den nyeste datoen `() {
        // Given
        val oppfolgingStartdato = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Oslo"))
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, oppfolgingStartdato)
        pdlIdentRepository.upsertIdenter(identerBruker)

        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)

        // Ingen beregning skal returnere null
        `when`(dagpengerClient.hentDagpengerBeregninger(anyString(), anyString(), any())).thenReturn(emptyList())
        val resultat_uten_beregning =
            dagpengerService.hentAntallResterendeDagerFraApi(norskIdent.get(), oppfolgingStartdato.toLocalDate())
        assertThat(resultat_uten_beregning).isNull()

        // Skal hente ut beregning med nyeste dato
        `when`(dagpengerClient.hentDagpengerBeregninger(anyString(), anyString(), any())).thenReturn(mockedBeregning)
        val resultat =
            dagpengerService.hentAntallResterendeDagerFraApi(norskIdent.get(), oppfolgingStartdato.toLocalDate())
        assertThat(resultat).isNotNull
        assertThat(resultat!!.gjenståendeDager).isEqualTo(118)
        assertThat(resultat.dato).isEqualTo(LocalDate.of(2026, 1, 3))
    }

    @Test
    fun `skal oppdatere til ny ident og slette gammel rad når kafkamedling med ny ident kommer`() {
        // Given
        val norskIdentHistorisk = randomNorskIdent()
        val identerBruker = listOf(
            PDLIdent(aktorId.get(), false, Gruppe.AKTORID),
            PDLIdent(norskIdent.get(), false, Gruppe.FOLKEREGISTERIDENT),
            PDLIdent(norskIdentHistorisk.get(), true, Gruppe.FOLKEREGISTERIDENT)
        )

        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        pdlIdentRepository.upsertIdenter(identerBruker)

        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(dagpengerClient.hentDagpengerPerioder(anyString(), anyString(), any())).thenReturn(mockedPerioder)
        `when`(dagpengerClient.hentDagpengerBeregninger(anyString(), anyString(), any())).thenReturn(mockedBeregning)

        // Lagre en rad på historisk ident, før kafkamelding med ny ident kommer
        dagpengerService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMelding.copy(personId = norskIdentHistorisk.get()))
        val lagretGammelIdent = dagpengerRepository.hentDagpenger(norskIdentHistorisk.get())
        assertThat(lagretGammelIdent).isNotNull()

        // Behandle kafkamelding med ny ident
        dagpengerService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMelding)
        val lagretGammelIdentEtterOppdatering = dagpengerRepository.hentDagpenger(norskIdentHistorisk.get())
        val lagretNyIdent = dagpengerRepository.hentDagpenger(norskIdent.get())

        // Then
        assertThat(lagretGammelIdentEtterOppdatering).isNull()
        assertThat(lagretNyIdent).isNotNull()
    }

//    @Test
//    fun `Tiltakspenger skal populere og filtrere riktig i opensearch når man har ytelsen`() {
//        val aktorId = randomAktorId()
//        setInitialState(aktorId)
//        val getResponse = opensearchTestClient.fetchDocument(aktorId)
//        assertThat(getResponse.isExists).isTrue()
//
//        val tiltakspengerRespons = getResponse.sourceAsMap["tiltakspenger"];
//
//        assertThat(tiltakspengerRespons).isNotNull
//        assertThat(tiltakspengerRespons).isEqualTo(true)
//
//        val filtervalg = getFiltervalgDefaults().copy(
//            ytelseTiltakspenger = listOf(YtelseTiltakspenger.HAR_TILTAKSPENGER)
//        )
//
//        verifiserAsynkront(
//            2, TimeUnit.SECONDS
//        ) {
//            val responseBrukere: BrukereMedAntall = opensearchService.hentBrukere(
//                "1123",
//                Optional.empty(),
//                Sorteringsrekkefolge.STIGENDE,
//                Sorteringsfelt.IKKE_SATT,
//                filtervalg,
//                null,
//                null
//            )
//
//            assertThat(responseBrukere.antall).isEqualTo(1)
//            assertThat(responseBrukere.brukere.first().ytelser.tiltakspenger).isNotNull()
//        }
//    }
//
//    private fun setInitialState(aktorId: AktorId) {
//        testDataClient.lagreBrukerUnderOppfolging(aktorId, norskIdent, navKontor, veilederId)
//        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
//        populateOpensearch(navKontor, veilederId, aktorId.get())
//
//        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
//        val mockedRespons = listOf(mockedVedtak)
//        `when`(tiltakspengerClient.hentTiltakspenger(anyString(), anyString(), any())).thenReturn(mockedRespons)
//
//        tiltakspengerService.behandleKafkaMeldingLogikk(mockedYtelseKafkaMelding.copy(personId = norskIdent.toString()))
//
//    }
//

    val mockedYtelseKafkaMelding = YtelserKafkaDTO(
        personId = "10108000000",
        meldingstype = YTELSE_MELDINGSTYPE.OPPRETT,
        ytelsestype = YTELSE_TYPE.DAGPENGER,
        kildesystem = YTELSE_KILDESYSTEM.DPSAK
    )

    val periodeTomErNull = DagpengerPeriodeDto(
        fraOgMedDato = LocalDate.of(2023, 1, 1),
        tilOgMedDato = null,
        ytelseType = DagpengerRettighetstype.DAGPENGER_ARBEIDSSOKER_ORDINAER,
        kilde = "DP_SAK"
    )

    val periodeKildeErArena = DagpengerPeriodeDto(
        fraOgMedDato = LocalDate.of(2023, 1, 1),
        tilOgMedDato = null,
        ytelseType = DagpengerRettighetstype.DAGPENGER_ARBEIDSSOKER_ORDINAER,
        kilde = "ARENA"
    )

    val periodeTomErPassert = DagpengerPeriodeDto(
        fraOgMedDato = LocalDate.of(2023, 1, 1),
        tilOgMedDato = LocalDate.of(2023, 2, 1),
        ytelseType = DagpengerRettighetstype.DAGPENGER_ARBEIDSSOKER_ORDINAER,
        kilde = "DP_SAK"
    )

    val periodeTomErIkkePassert = DagpengerPeriodeDto(
        fraOgMedDato = LocalDate.of(2023, 1, 1),
        tilOgMedDato = LocalDate.now().plusDays(30),
        ytelseType = DagpengerRettighetstype.DAGPENGER_ARBEIDSSOKER_ORDINAER,
        kilde = "DP_SAK"
    )

    val periodeErUtenforOppfølging = DagpengerPeriodeDto(
        fraOgMedDato = LocalDate.of(2020, 1, 1),
        tilOgMedDato = LocalDate.of(2020, 2, 1),
        ytelseType = DagpengerRettighetstype.DAGPENGER_ARBEIDSSOKER_ORDINAER,
        kilde = "DP_SAK"
    )

    val mockedPerioder = DagpengerPerioderResponseDto(
        personIdent = "10108000000",
        perioder = listOf(
            periodeTomErNull,
            periodeKildeErArena,
            periodeTomErPassert,
            periodeTomErIkkePassert,
            periodeErUtenforOppfølging
        )
    )

    val mockedBeregning = listOf(
        DagpengerBeregningerResponseDto(
            dato = LocalDate.of(2026, 1, 1),
            sats = 500,
            utbetaltBeløp = 15000,
            gjenståendeDager = 120
        ),
        DagpengerBeregningerResponseDto(
            dato = LocalDate.of(2026, 1, 2),
            sats = 500,
            utbetaltBeløp = 15000,
            gjenståendeDager = 119
        ),
        DagpengerBeregningerResponseDto(
            dato = LocalDate.of(2026, 1, 3),
            sats = 500,
            utbetaltBeløp = 15000,
            gjenståendeDager = 118
        )

    )

}
