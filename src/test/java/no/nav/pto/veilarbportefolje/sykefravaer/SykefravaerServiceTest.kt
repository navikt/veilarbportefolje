package no.nav.pto.veilarbportefolje.sykefravaer

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.client.AktorClient
import no.nav.pto.veilarbportefolje.database.PostgresTable
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe
import no.nav.pto.veilarbportefolje.sykefravaer.dto.OppfolgingstilfelleDTO
import no.nav.pto.veilarbportefolje.sykefravaer.dto.OppfolgingstilfellePersonDTO
import no.nav.pto.veilarbportefolje.sykefravaer.dto.SykefravaerPersonKafkaDTO
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNorskIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.*

class SykefravaerServiceTest(
    @param:Autowired private val jdbcTemplate: JdbcTemplate,
    @param:Autowired private val sykefravaerRepository: SykefravaerRepository,
    @param:Autowired private val pdlIdentRepository: PdlIdentRepository,
    @param:Autowired private val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
) : EndToEndTest() {

    private lateinit var sykefravaerService: SykefravaerService
    private val sykefravaerClient: SykefravaerClient = mock()
    private val aktorClient: AktorClient = mock()

    private val norskIdent = Fnr.ofValidFnr("10108000000")
    private val aktorId = AktorId.of("12345")
    private val identerBruker = listOf(
        PDLIdent(aktorId.get(), false, Gruppe.AKTORID),
        PDLIdent(norskIdent.get(), false, Gruppe.FOLKEREGISTERIDENT)
    )

    private val kafkaMelding = SykefravaerPersonKafkaDTO(
        uuid = UUID.randomUUID().toString(),
        createdAt = OffsetDateTime.now(),
        personIdentNumber = norskIdent.get(),
    )

    private val aktivSykefravaerPeriode = OppfolgingstilfelleDTO(
        arbeidstakerAtTilfelleEnd = true,
        start = LocalDate.now().minusWeeks(3),
        end = LocalDate.now().plusWeeks(2),
    )

    private val utloptSykefravaerPeriode = OppfolgingstilfelleDTO(
        arbeidstakerAtTilfelleEnd = true,
        start = LocalDate.now().minusWeeks(6),
        end = LocalDate.now().minusDays(1),
    )

    @BeforeEach
    fun setUp() {
        listOf(PostgresTable.SYKEFRAVAER_OPPFOLGING.TABLE_NAME, "oppfolging_data", "bruker_identer")
            .forEach { jdbcTemplate.execute("TRUNCATE TABLE $it") }

        sykefravaerService = SykefravaerService(
            sykefravaerClient = sykefravaerClient,
            sykefravaerRepository = sykefravaerRepository,
            oppfolgingRepositoryV2 = oppfolgingRepositoryV2,
            pdlIdentRepository = pdlIdentRepository,
            aktorClient = aktorClient,
        )
    }

    @Test
    fun `skal hente og lagre sykefravær ved mottatt kafkamelding når bruker er sykmeldt nå`() {
        settBrukerUnderOppfolging()
        mockApiRespons(listOf(aktivSykefravaerPeriode))

        sykefravaerService.behandleKafkaMeldingLogikk(kafkaMelding)

        val lagret = sykefravaerRepository.hentSykefravaerPeriode(norskIdent.get())
        assertThat(lagret).isNotNull
        assertThat(lagret!!.start).isEqualTo(aktivSykefravaerPeriode.start)
        assertThat(lagret.slutt).isEqualTo(aktivSykefravaerPeriode.end)
        assertThat(lagret.harArbeidsgiver).isTrue()
    }

    @Test
    fun `skal bruke første tilfelle i listen`() {
        settBrukerUnderOppfolging()
        val annetAktivtTilfelle = aktivSykefravaerPeriode.copy(
            start = LocalDate.now().minusWeeks(1),
            end = LocalDate.now().plusWeeks(5),
            arbeidstakerAtTilfelleEnd = false
        )
        mockApiRespons(listOf(aktivSykefravaerPeriode, annetAktivtTilfelle))

        sykefravaerService.behandleKafkaMeldingLogikk(kafkaMelding)

        val lagret = sykefravaerRepository.hentSykefravaerPeriode(norskIdent.get())
        assertThat(lagret!!.start).isEqualTo(aktivSykefravaerPeriode.start)
        assertThat(lagret.slutt).isEqualTo(aktivSykefravaerPeriode.end)
    }

    @Test
    fun `skal ikke lagre sykefravær når første tilfelle er utløpt`() {
        settBrukerUnderOppfolging()
        mockApiRespons(listOf(utloptSykefravaerPeriode))

        sykefravaerService.behandleKafkaMeldingLogikk(kafkaMelding)

        assertThat(sykefravaerRepository.hentSykefravaerPeriode(norskIdent.get())).isNull()
    }

    @Test
    fun `skal ikke lagre sykefravær når api returnerer tom liste`() {
        settBrukerUnderOppfolging()
        mockApiRespons(emptyList())

        sykefravaerService.behandleKafkaMeldingLogikk(kafkaMelding)

        assertThat(sykefravaerRepository.hentSykefravaerPeriode(norskIdent.get())).isNull()
    }

    @Test
    fun `skal slette lagret sykefravær når bruker ikke lenger er sykmeldt`() {
        settBrukerUnderOppfolging()
        mockApiRespons(listOf(aktivSykefravaerPeriode))
        sykefravaerService.behandleKafkaMeldingLogikk(kafkaMelding)
        assertThat(sykefravaerRepository.hentSykefravaerPeriode(norskIdent.get())).isNotNull

        mockApiRespons(listOf(utloptSykefravaerPeriode))
        sykefravaerService.behandleKafkaMeldingLogikk(kafkaMelding)

        assertThat(sykefravaerRepository.hentSykefravaerPeriode(norskIdent.get())).isNull()
    }

    @Test
    fun `skal ikke behandle kafkamelding når person ikke er under oppfølging`() {
        pdlIdentRepository.upsertIdenter(identerBruker)

        sykefravaerService.behandleKafkaMeldingLogikk(kafkaMelding)

        assertThat(sykefravaerRepository.hentSykefravaerPeriode(norskIdent.get())).isNull()
        verify(sykefravaerClient, never()).hentSykefravaersoppfolingPeriode(anyString())
    }

    @Test
    fun `skal oppdatere til ny ident og slette gammel rad når kafkamelding med ny ident kommer`() {
        val norskIdentHistorisk = randomNorskIdent()
        pdlIdentRepository.upsertIdenter(
            identerBruker + PDLIdent(norskIdentHistorisk.get(), true, Gruppe.FOLKEREGISTERIDENT)
        )
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2), UUID.randomUUID())
        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        mockApiRespons(listOf(aktivSykefravaerPeriode))

        sykefravaerService.behandleKafkaMeldingLogikk(kafkaMelding.copy(personIdentNumber = norskIdentHistorisk.get()))
        assertThat(sykefravaerRepository.hentSykefravaerPeriode(norskIdentHistorisk.get())).isNotNull

        sykefravaerService.behandleKafkaMeldingLogikk(kafkaMelding)

        assertThat(sykefravaerRepository.hentSykefravaerPeriode(norskIdentHistorisk.get())).isNull()
        assertThat(sykefravaerRepository.hentSykefravaerPeriode(norskIdent.get())).isNotNull
    }

    @Test
    fun `skal hente og lagre sykefravær ved oppfølgingsstart`() {
        pdlIdentRepository.upsertIdenter(identerBruker)
        `when`(aktorClient.hentFnr(aktorId)).thenReturn(norskIdent)
        mockApiRespons(listOf(aktivSykefravaerPeriode))

        sykefravaerService.hentOgLagreSykefravaerForBrukerVedOppfolgingStart(aktorId)

        assertThat(sykefravaerRepository.hentSykefravaerPeriode(norskIdent.get())).isNotNull
    }

    @Test
    fun `slettSykefravaerData skal slette sykefravær for alle identer til bruker`() {
        val norskIdentHistorisk = randomNorskIdent()
        pdlIdentRepository.upsertIdenter(
            identerBruker + PDLIdent(norskIdentHistorisk.get(), true, Gruppe.FOLKEREGISTERIDENT)
        )
        sykefravaerRepository.upsertSykefravaerOppfolgingsperiode(norskIdent.get(), aktivSykefravaerPeriode)
        sykefravaerRepository.upsertSykefravaerOppfolgingsperiode(norskIdentHistorisk.get(), aktivSykefravaerPeriode)

        sykefravaerService.slettSykefravaerData(aktorId, Optional.of(norskIdent))

        assertThat(sykefravaerRepository.hentSykefravaerPeriode(norskIdent.get())).isNull()
        assertThat(sykefravaerRepository.hentSykefravaerPeriode(norskIdentHistorisk.get())).isNull()
    }


    private fun settBrukerUnderOppfolging() {
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2), UUID.randomUUID())
        pdlIdentRepository.upsertIdenter(identerBruker)
        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
    }

    private fun mockApiRespons(tilfeller: List<OppfolgingstilfelleDTO>) {
        `when`(sykefravaerClient.hentSykefravaersoppfolingPeriode(anyString())).thenReturn(
            OppfolgingstilfellePersonDTO(
                oppfolgingstilfelleList = tilfeller,
                personIdent = norskIdent.get(),
            )
        )
    }
}
