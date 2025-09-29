package no.nav.pto.veilarbportefolje.aap

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.aap.domene.*
import no.nav.pto.veilarbportefolje.aap.repository.AapRepository
import no.nav.pto.veilarbportefolje.domene.*
import no.nav.pto.veilarbportefolje.domene.value.NavKontor
import no.nav.pto.veilarbportefolje.domene.value.VeilederId
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.oppfolging.domene.OppfolgingMedStartdato
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.IdenterForBruker
import no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp
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
import java.util.*
import java.util.concurrent.TimeUnit


class AapServiceTest(
    @Autowired private val jdbcTemplate: JdbcTemplate,
    @Autowired private val aapRepository: AapRepository,
    @Autowired private val opensearchService: OpensearchService,
    @Autowired private val opensearchIndexerV2: OpensearchIndexerV2
) : EndToEndTest() {

    private lateinit var aapService: AapService
    private val aapClient: AapClient = mock()
    private val aktorClient: AktorClient = mock()
    private val oppfolgingRepositoryV2: OppfolgingRepositoryV2 = mock()
    private val pdlIdentRepository: PdlIdentRepository = mock()

    @BeforeEach
    fun setUp() {
        aapService = AapService(
            aapClient,
            aktorClient,
            oppfolgingRepositoryV2,
            pdlIdentRepository,
            aapRepository,
            opensearchIndexerV2
        )
    }

    @BeforeEach
    fun `reset data`() {
        jdbcTemplate.update("TRUNCATE TABLE YTELSER_AAP")
        jdbcTemplate.execute("truncate table oppfolging_data")
    }

    val norskIdent = Fnr.ofValidFnr("10108000000")
    val navKontor = NavKontor.of("1123")
    val veilederId = VeilederId.of("Z12345")

    @Test
    fun `skal starte henting og lagring av aap ved mottatt kafkamelding`() {
        // Given
        val norskIdent = mockedYtelseAapMelding.personident
        val aktorId = randomAktorId()

        `when`(pdlIdentRepository.erBrukerUnderOppfolging(norskIdent)).thenReturn(true)
        `when`(pdlIdentRepository.hentFnrIdenterForBruker(norskIdent)).thenReturn(IdenterForBruker(listOf(norskIdent)))
        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(oppfolgingRepositoryV2.hentOppfolgingMedStartdato(any())).thenReturn(
            Optional.of(OppfolgingMedStartdato(true, toTimestamp(LocalDate.now().minusMonths(2))))
        )
        `when`(aapClient.hentAapVedtak(anyString(), anyString(), anyString())).thenReturn(mockedAapClientRespons)

        //When
        aapService.behandleKafkaMeldingLogikk(mockedYtelseAapMelding)
        val lagretAap = aapRepository.hentAap(norskIdent)

        //Then
        assertThat(lagretAap).isNotNull
    }

    @Test
    fun `skal ikke behandle kafkamelding når person ikke har aap og er av meldingstype opprett`() {
        // Given
        val norskIdent = mockedYtelseAapMelding.personident
        val aktorId = randomAktorId()

        `when`(pdlIdentRepository.erBrukerUnderOppfolging(norskIdent)).thenReturn(true)
        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(oppfolgingRepositoryV2.hentOppfolgingMedStartdato(any())).thenReturn(
            Optional.of(OppfolgingMedStartdato(true, toTimestamp(LocalDate.now().minusMonths(2))))
        )
        `when`(aapClient.hentAapVedtak(anyString(), anyString(), anyString())).thenReturn(AapVedtakResponseDto(emptyList()))

        // When
        aapService.behandleKafkaMeldingLogikk(mockedYtelseAapMelding)
        val lagretAap = aapRepository.hentAap(norskIdent)

        // Then
        assertThat(lagretAap).isNull()
    }

    @Test
    fun `skal behandle kafkamelding og slette data når person ikke har aap men meldingstype er oppdater`() {
        // Given
        val norskIdent = mockedYtelseAapMelding.personident
        val aktorId = randomAktorId()

        `when`(pdlIdentRepository.erBrukerUnderOppfolging(norskIdent)).thenReturn(true)
        `when`(pdlIdentRepository.hentFnrIdenterForBruker(norskIdent)).thenReturn(IdenterForBruker(listOf(norskIdent)))
        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(oppfolgingRepositoryV2.hentOppfolgingMedStartdato(any())).thenReturn(
            Optional.of(OppfolgingMedStartdato(true, toTimestamp(LocalDate.now().minusMonths(2))))
        )

        // Opprett rad i db for å verifisere at den blir slettet i neste steg
        `when`(aapClient.hentAapVedtak(anyString(), anyString(), anyString())).thenReturn(mockedAapClientRespons)
        aapService.behandleKafkaMeldingLogikk(mockedYtelseAapMelding)
        val lagretAapOpprett = aapRepository.hentAap(norskIdent)
        assertThat(lagretAapOpprett).isNotNull

        // Oppdatermelding på samme person uten aap skal slette rad i db
        `when`(aapClient.hentAapVedtak(anyString(), anyString(), anyString())).thenReturn(AapVedtakResponseDto(emptyList()))
        aapService.behandleKafkaMeldingLogikk(mockedYtelseAapMelding.copy(meldingstype = YTELSE_MELDINGSTYPE.OPPDATER))
        val lagretAapOppdater = aapRepository.hentAap(norskIdent)
        assertThat(lagretAapOppdater).isNull()
    }

    @Test
    fun `skal ikke behandle kafkamelding når person ikke er under oppfølging`() {
        // Given
        val norskIdent = mockedYtelseAapMelding.personident
        `when`(pdlIdentRepository.erBrukerUnderOppfolging(norskIdent)).thenReturn(false)

        // When
        aapService.behandleKafkaMeldingLogikk(mockedYtelseAapMelding)
        val lagretAap = aapRepository.hentAap(norskIdent)

        // Then
        assertThat(lagretAap).isNull()
    }


    @Test
    fun `hentAapVedtakForOppfolgingPeriode filtrerer vedtak utenfor oppfolging`() {
        val fnr = "12345678910"
        val aktorId = AktorId("1234")
        val oppfolgingStartdato = LocalDate.of(2023, 1, 1)

        `when`(aktorClient.hentAktorId(Fnr.of(fnr))).thenReturn(aktorId)
        `when`(oppfolgingRepositoryV2.hentOppfolgingMedStartdato(aktorId)).thenReturn(
            Optional.of(OppfolgingMedStartdato(true, toTimestamp(oppfolgingStartdato)))
        )

        val vedtakInnenfor = mockedVedtak.copy(
            periode = AapVedtakResponseDto.Periode(
                fraOgMedDato = LocalDate.of(2023, 2, 1),
                tilOgMedDato = LocalDate.of(2023, 12, 31)
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

        val resultat = aapService.hentSisteAapPeriodeFraApi(fnr, oppfolgingStartdato)
        assertThat(resultat).isEqualTo(vedtakInnenfor)
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
    fun `aap periode som overlapper oppfolgingStartdato skal beholdes`() {
        val oppfolgingsStartdato = LocalDate.of(2024, 1, 1)
        val aapVedtakPeriode = AapVedtakResponseDto.Periode(
            fraOgMedDato = LocalDate.of(2023, 12, 1),
            tilOgMedDato = LocalDate.of(2024, 6, 30)
        )

        val resultat = aapService.filtrerAapKunIOppfolgingPeriode(oppfolgingsStartdato, aapVedtakPeriode)
        assertThat(resultat).isEqualTo(aapVedtakPeriode)
    }

    @Test
    fun `AAP skal populere og filtrere riktig i opensearch når man har aap i kelvin `() {
        val aktorId = randomAktorId()
        setInitialState(aktorId, harAap = true)
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
            assertThat(responseBrukere.brukere.first().isHarAapKelvin).isEqualTo(true)
        }
    }

    @Test
    fun `AAP skal populere og filtrere riktig i opensearch når man ikke har aap i kelvin `() {
        val aktorId = randomAktorId()
        setInitialState(aktorId, harAap = false)
        val getResponse = opensearchTestClient.fetchDocument(aktorId)
        assertThat(getResponse.isExists).isTrue()

        val aapKelvinRespons = getResponse.sourceAsMap["aap_kelvin"];
        assertThat(aapKelvinRespons).isEqualTo(false)

        val filtervalg = Filtervalg()
        filtervalg.setYtelseAapKelvin(listOf(YtelseAapKelvin.HAR_IKKE_AAP))
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
            assertThat(responseBrukere.brukere.first().isHarAapKelvin).isEqualTo(false)
        }
    }

    @Test
    fun `AAP returnere alle brukere med og uten aap når begge filtrene er valgt `() {
        val aktorId1 = randomAktorId()
        val aktorId2 = randomAktorId()
        setInitialState(aktorId1, harAap = false)
        setInitialState(aktorId2, harAap = true)

        val filtervalg = Filtervalg()
        filtervalg.setYtelseAapKelvin(listOf(YtelseAapKelvin.HAR_IKKE_AAP, YtelseAapKelvin.HAR_AAP))
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
            assertThat(responseBrukere.brukere.filter { it.isHarAapKelvin }.size).isEqualTo(1)
            assertThat(responseBrukere.brukere.filter { !it.isHarAapKelvin }.size).isEqualTo(1)
        }
    }

    @Test
    fun `skal populere og filtrere riktig i opensearch ved sletting av AAP `() {
        val aktorId = randomAktorId()
        // Legg til bruker med aap og oppdater opensearch
        setInitialState(aktorId, harAap = true)
        val getResponse = opensearchTestClient.fetchDocument(aktorId)
        val aapKelvinRespons = getResponse.sourceAsMap["aap_kelvin"];
        assertThat(aapKelvinRespons).isEqualTo(true)

        //Fjern aap og oppdater opensearch
        `when`(aapClient.hentAapVedtak(anyString(), anyString(), anyString())).thenReturn(uttdatertMockeAapClientRespons)
        aapService.behandleKafkaMeldingLogikk(mockedYtelseAapMelding.copy(personident = norskIdent.toString(), meldingstype = YTELSE_MELDINGSTYPE.OPPDATER))
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


    private fun setInitialState(aktorId: AktorId, harAap: Boolean) {
        testDataClient.lagreBrukerUnderOppfolging(aktorId, norskIdent, navKontor, veilederId)
        populateOpensearch(navKontor, veilederId, aktorId.get())

        `when`(pdlIdentRepository.erBrukerUnderOppfolging(norskIdent.toString())).thenReturn(true)
        `when`(pdlIdentRepository.hentFnrIdenterForBruker(norskIdent.toString())).thenReturn(IdenterForBruker(listOf(norskIdent.toString())))
        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        `when`(oppfolgingRepositoryV2.hentOppfolgingMedStartdato(any())).thenReturn(
            Optional.of(OppfolgingMedStartdato(true, toTimestamp(LocalDate.now().minusMonths(2))))
        )
        val mockedRespons = if (harAap) mockedAapClientRespons else AapVedtakResponseDto(emptyList())
        `when`(aapClient.hentAapVedtak(anyString(), anyString(), anyString())).thenReturn(mockedRespons)

        aapService.behandleKafkaMeldingLogikk(mockedYtelseAapMelding.copy(personident = norskIdent.toString()))

    }

}

val mockedYtelseAapMelding = YtelserKafkaDTO(
    personident = randomNorskIdent().get(),
    meldingstype = YTELSE_MELDINGSTYPE.OPPRETT,
    ytelsestype = YTELSE_TYPE.AAP,
    kildesystem = YTELSE_KILDESYSTEM.KELVIN
)

val mockedVedtak = AapVedtakResponseDto.Vedtak(
    status = "LØPENDE",
    saksnummer = "S123",
    rettighetsType = "TYPE1",
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
