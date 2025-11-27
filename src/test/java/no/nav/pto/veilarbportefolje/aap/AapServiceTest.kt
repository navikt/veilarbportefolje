package no.nav.pto.veilarbportefolje.aap

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.aap.dto.AapVedtakResponseDto
import no.nav.pto.veilarbportefolje.aap.domene.*
import no.nav.pto.veilarbportefolje.client.AktorClient
import no.nav.pto.veilarbportefolje.domene.*
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall
import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg
import no.nav.pto.veilarbportefolje.domene.filtervalg.YtelseAapKelvin
import no.nav.pto.veilarbportefolje.domene.NavKontor
import no.nav.pto.veilarbportefolje.domene.VeilederId
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
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
import java.util.*
import java.util.List
import java.util.concurrent.TimeUnit


class AapServiceTest(
    @Autowired private val jdbcTemplate: JdbcTemplate,
    @Autowired private val aapRepository: AapRepository,
    @Autowired private val opensearchService: OpensearchService,
    @Autowired private val opensearchIndexerPaDatafelt: OpensearchIndexerPaDatafelt,
    @Autowired private val pdlIdentRepository: PdlIdentRepository,
    @Autowired private val oppfolgingRepositoryV2: OppfolgingRepositoryV2
) : EndToEndTest() {
    private lateinit var aapService: AapService
    private val aapClient: AapClient = mock()
    private val aktorClient: AktorClient = mock()

    @BeforeEach
    fun setUp() {
        resetDatabase()
        initializeService()
    }

    private fun resetDatabase() {
        listOf("YTELSER_AAP", "oppfolging_data", "bruker_identer")
            .forEach { jdbcTemplate.execute("TRUNCATE TABLE $it") }
    }

    private fun initializeService() {
        aapService = AapService(
            aapClient,
            aktorClient,
            oppfolgingRepositoryV2,
            pdlIdentRepository,
            aapRepository,
            opensearchIndexerPaDatafelt
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
    fun `skal starte henting og lagring av aap ved mottatt kafkamelding`() {
        // Given
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        pdlIdentRepository.upsertIdenter(identerBruker)

        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(aapClient.hentAapVedtak(anyString(), anyString(), anyString())).thenReturn(mockedAapClientRespons)

        //When
        aapService.behandleKafkaMeldingLogikk(mockedYtelseAapMelding)
        val lagretAap = aapRepository.hentAap(norskIdent.get())

        //Then
        assertThat(lagretAap).isNotNull
    }

    @Test
    fun `skal ikke behandle kafkamelding når person ikke har aap og er av meldingstype opprett`() {
        // Given
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        pdlIdentRepository.upsertIdenter(identerBruker)

        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(
            aapClient.hentAapVedtak(
                anyString(),
                anyString(),
                anyString()
            )
        ).thenReturn(AapVedtakResponseDto(emptyList()))

        // When
        aapService.behandleKafkaMeldingLogikk(mockedYtelseAapMelding)
        val lagretAap = aapRepository.hentAap(norskIdent.get())

        // Then
        assertThat(lagretAap).isNull()
    }

    @Test
    fun `skal behandle kafkamelding og slette data når person ikke har aap men meldingstype er oppdater`() {
        // Given
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        pdlIdentRepository.upsertIdenter(identerBruker)

        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)

        // Opprett rad i db for å verifisere at den blir slettet i neste steg
        `when`(aapClient.hentAapVedtak(anyString(), anyString(), anyString())).thenReturn(mockedAapClientRespons)
        aapService.behandleKafkaMeldingLogikk(mockedYtelseAapMelding)
        val lagretAapOpprett = aapRepository.hentAap(norskIdent.get())
        assertThat(lagretAapOpprett).isNotNull

        // Oppdatermelding på samme person uten aap skal slette rad i db
        `when`(
            aapClient.hentAapVedtak(
                anyString(),
                anyString(),
                anyString()
            )
        ).thenReturn(AapVedtakResponseDto(emptyList()))
        aapService.behandleKafkaMeldingLogikk(mockedYtelseAapMelding.copy(meldingstype = YTELSE_MELDINGSTYPE.OPPDATER))
        val lagretAapOppdater = aapRepository.hentAap(norskIdent.get())
        assertThat(lagretAapOppdater).isNull()
    }

    @Test
    fun `skal ikke behandle kafkamelding når person ikke er under oppfølging`() {
        // Given
        pdlIdentRepository.upsertIdenter(identerBruker)

        // When
        aapService.behandleKafkaMeldingLogikk(mockedYtelseAapMelding)
        val lagretAap = aapRepository.hentAap(norskIdent.get())

        // Then
        assertThat(lagretAap).isNull()
    }


    @Test
    fun `hentAapVedtakForOppfolgingPeriode filtrerer vedtak utenfor oppfolging`() {
        val oppfolgingStartdato = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Oslo"))
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, oppfolgingStartdato)
        pdlIdentRepository.upsertIdenter(identerBruker)
        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)

        val vedtakInnenfor = mockedVedtak.copy(
            periode = AapVedtakResponseDto.Periode(
                fraOgMedDato = LocalDate.of(2022, 2, 1),
                tilOgMedDato = LocalDate.of(2023, 1, 1)
            )
        )

        val vedtakForTidlig = mockedVedtak.copy(
            periode = AapVedtakResponseDto.Periode(
                fraOgMedDato = LocalDate.of(2022, 1, 1),
                tilOgMedDato = LocalDate.of(2022, 12, 31)
            )
        )

        val apiResponse = AapVedtakResponseDto(vedtak = listOf(vedtakInnenfor, vedtakForTidlig))
        `when`(aapClient.hentAapVedtak(anyString(), anyString(), anyString())).thenReturn(apiResponse)

        val resultat = aapService.hentSisteAapPeriodeFraApi(norskIdent.get(), oppfolgingStartdato.toLocalDate())
        assertThat(resultat).isEqualTo(vedtakInnenfor)
    }

    @Test
    fun `AAP skal populere og filtrere riktig i opensearch når man har aap i kelvin `() {
        val aktorId = randomAktorId()
        setInitialState(aktorId)
        val getResponse = opensearchTestClient.fetchDocument(aktorId)
        assertThat(getResponse.isExists).isTrue()

        val aapKelvinRespons = getResponse.sourceAsMap["aap_kelvin"];

        assertThat(aapKelvinRespons).isNotNull
        assertThat(aapKelvinRespons).isEqualTo(true)

        val filtervalg = Filtervalg()
        filtervalg.setYtelseAapKelvin(listOf(YtelseAapKelvin.HAR_AAP))
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
            assertThat(responseBrukere.brukere.first().ytelser.aap).isNotNull()
        }
    }

    @Test
    fun `skal populere og filtrere riktig i opensearch ved sletting av AAP `() {
        val aktorId = randomAktorId()
        // Legg til bruker med aap og oppdater opensearch
        setInitialState(aktorId)
        val getResponse = opensearchTestClient.fetchDocument(aktorId)
        val aapKelvinRespons = getResponse.sourceAsMap["aap_kelvin"];
        assertThat(aapKelvinRespons).isEqualTo(true)

        //Fjern aap og oppdater opensearch
        `when`(
            aapClient.hentAapVedtak(
                anyString(),
                anyString(),
                anyString()
            )
        ).thenReturn(uttdatertMockeAapClientRespons)
        aapService.behandleKafkaMeldingLogikk(
            mockedYtelseAapMelding.copy(
                personId = norskIdent.toString(),
                meldingstype = YTELSE_MELDINGSTYPE.OPPDATER
            )
        )
        val getResponse2 = opensearchTestClient.fetchDocument(aktorId)
        val aapKelvinRespons2 = getResponse2.sourceAsMap["aap_kelvin"];
        assertThat(aapKelvinRespons2).isEqualTo(false)

        val filtervalg = Filtervalg()
        filtervalg.setYtelseAapKelvin(listOf(YtelseAapKelvin.HAR_AAP))
        filtervalg.setFerdigfilterListe(listOf())

        verifiserAsynkront(
            2, TimeUnit.SECONDS
        ) {
            val responseBrukere: BrukereMedAntall = opensearchService.hentBrukere(
                navKontor.toString(),
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
        `when`(aapClient.hentAapVedtak(anyString(), anyString(), anyString())).thenReturn(mockedAapClientRespons)

        // Lagre en rad på historisk ident, før kafkamelding med ny ident kommer
        aapService.behandleKafkaMeldingLogikk(mockedYtelseAapMelding.copy(personId = norskIdentHistorisk.get()))
        val lagretAapGammelIdent = aapRepository.hentAap(norskIdentHistorisk.get())
        assertThat(lagretAapGammelIdent).isNotNull()

        // Behandle kafkamelding med ny ident
        aapService.behandleKafkaMeldingLogikk(mockedYtelseAapMelding)
        val lagretAapGammelIdentEtterOppdatering = aapRepository.hentAap(norskIdentHistorisk.get())
        val lagretAapNyIdent = aapRepository.hentAap(norskIdent.get())
        assertThat(lagretAapGammelIdentEtterOppdatering).isNull()
        assertThat(lagretAapNyIdent).isNotNull()

    }

    @Test
    fun `opensearch skal fortsatt indeksere på aktørid ved endring i personident`() {
        // Given
        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(aapClient.hentAapVedtak(anyString(), anyString(), anyString())).thenReturn(mockedAapClientRespons)

        val identerBruker = List.of<PDLIdent>(
            PDLIdent(aktorId.get(), false, Gruppe.AKTORID),
            PDLIdent(norskIdent.get(), false, Gruppe.FOLKEREGISTERIDENT),
        )

        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        pdlIdentRepository.upsertIdenter(identerBruker)
        populateOpensearch(navKontor, veilederId, aktorId.get())

        val nyNorskIdent = randomNorskIdent()
        val identerBrukerMedNyIdent =  List.of<PDLIdent>(
            PDLIdent(aktorId.get(), false, Gruppe.AKTORID),
            PDLIdent(norskIdent.get(), true, Gruppe.FOLKEREGISTERIDENT),
            PDLIdent(nyNorskIdent.get(), false, Gruppe.FOLKEREGISTERIDENT)
        )

        pdlIdentRepository.upsertIdenter(identerBrukerMedNyIdent)
        populateOpensearch(navKontor, veilederId, aktorId.get())
        aapService.behandleKafkaMeldingLogikk(mockedYtelseAapMelding.copy(personId = norskIdent.toString()))

        //indeksering skal fortsatt være på aktørid
        val filtervalg = Filtervalg()
        filtervalg.setYtelseAapKelvin(listOf(YtelseAapKelvin.HAR_AAP))
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
        }

    }


    private fun setInitialState(aktorId: AktorId) {
        testDataClient.lagreBrukerUnderOppfolging(aktorId, norskIdent, navKontor, veilederId)
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2))
        populateOpensearch(navKontor, veilederId, aktorId.get())

        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        val mockedRespons = mockedAapClientRespons
        `when`(aapClient.hentAapVedtak(anyString(), anyString(), anyString())).thenReturn(mockedRespons)

        aapService.behandleKafkaMeldingLogikk(mockedYtelseAapMelding.copy(personId = norskIdent.toString()))

    }

}

val mockedYtelseAapMelding = YtelserKafkaDTO(
    personId = "10108000000",
    meldingstype = YTELSE_MELDINGSTYPE.OPPRETT,
    ytelsestype = YTELSE_TYPE.AAP,
    kildesystem = YTELSE_KILDESYSTEM.KELVIN
)

val mockedVedtak = AapVedtakResponseDto.Vedtak(
    status = AapVedtakStatus.LØPENDE,
    saksnummer = "S123",
    rettighetsType = AapRettighetstype.ARBEIDSSØKER,
    kildesystem = "KILDE1",
    opphorsAarsak = null,
    periode = AapVedtakResponseDto.Periode(
        fraOgMedDato = LocalDate.now().minusMonths(1),
        tilOgMedDato = LocalDate.now().plusMonths(1)
    )
)

val mockedAapClientRespons = AapVedtakResponseDto(
    vedtak = listOf(
        mockedVedtak
    )
)

val uttdatertMockeAapClientRespons = AapVedtakResponseDto(
    vedtak = listOf(
        mockedVedtak.copy(
            periode = AapVedtakResponseDto.Periode(
                fraOgMedDato = LocalDate.now().minusYears(3),
                tilOgMedDato = LocalDate.now().minusYears(2)
            )
        ),
    )
)
