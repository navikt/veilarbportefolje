package no.nav.pto.veilarbportefolje.sykmeldt

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.client.AktorClient
import no.nav.pto.veilarbportefolje.database.PostgresTable
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe
import no.nav.pto.veilarbportefolje.sykmeldt.dto.OppfolgingstilfelleDTO
import no.nav.pto.veilarbportefolje.sykmeldt.dto.SykmeldtPersonDTO
import no.nav.pto.veilarbportefolje.sykmeldt.dto.SykmeldtPersonKafkaDTO
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

class SykmeldtServiceTest(
    @param:Autowired private val jdbcTemplate: JdbcTemplate,
    @param:Autowired private val sykmeldtRepository: SykmeldtRepository,
    @param:Autowired private val pdlIdentRepository: PdlIdentRepository,
    @param:Autowired private val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
) : EndToEndTest() {

    private lateinit var sykmeldtService: SykmeldtService
    private val sykmeldtClient: SykmeldtClient = mock()
    private val aktorClient: AktorClient = mock()

    private val norskIdent = Fnr.ofValidFnr("10108000000")
    private val aktorId = AktorId.of("12345")
    private val identerBruker = listOf(
        PDLIdent(aktorId.get(), false, Gruppe.AKTORID),
        PDLIdent(norskIdent.get(), false, Gruppe.FOLKEREGISTERIDENT)
    )

    private val kafkaMelding = SykmeldtPersonKafkaDTO(
        uuid = UUID.randomUUID().toString(),
        createdAt = OffsetDateTime.now(),
        personIdentNumber = norskIdent.get(),
    )

    private val aktivSykmeldtPeriode = OppfolgingstilfelleDTO(
        arbeidstakerAtTilfelleEnd = true,
        start = LocalDate.now().minusWeeks(3),
        end = LocalDate.now().plusWeeks(2),
    )

    private val utloptSykmeldtPeriode = OppfolgingstilfelleDTO(
        arbeidstakerAtTilfelleEnd = true,
        start = LocalDate.now().minusWeeks(6),
        end = LocalDate.now().minusDays(1),
    )

    @BeforeEach
    fun setUp() {
        listOf(PostgresTable.SYKMELDT.TABLE_NAME, "oppfolging_data", "bruker_identer")
            .forEach { jdbcTemplate.execute("TRUNCATE TABLE $it") }

        sykmeldtService = SykmeldtService(
            sykmeldtClient = sykmeldtClient,
            sykmeldtRepository = sykmeldtRepository,
            oppfolgingRepositoryV2 = oppfolgingRepositoryV2,
            pdlIdentRepository = pdlIdentRepository,
            aktorClient = aktorClient,
        )
    }

    @Test
    fun `skal hente og lagre sykmeldtdata ved mottatt kafkamelding når bruker er sykmeldt nå`() {
        settBrukerUnderOppfolging()
        mockApiRespons(listOf(aktivSykmeldtPeriode))

        sykmeldtService.behandleKafkaMeldingLogikk(kafkaMelding)

        val lagret = sykmeldtRepository.hentSykmeldtPeriode(norskIdent.get())
        assertThat(lagret).isNotNull
        assertThat(lagret!!.start).isEqualTo(aktivSykmeldtPeriode.start)
        assertThat(lagret.slutt).isEqualTo(aktivSykmeldtPeriode.end)
        assertThat(lagret.harArbeidsgiver).isTrue()
    }

    @Test
    fun `skal bruke første tilfelle i listen`() {
        settBrukerUnderOppfolging()
        val annetAktivtTilfelle = aktivSykmeldtPeriode.copy(
            start = LocalDate.now().minusWeeks(1),
            end = LocalDate.now().plusWeeks(5),
            arbeidstakerAtTilfelleEnd = false
        )
        mockApiRespons(listOf(aktivSykmeldtPeriode, annetAktivtTilfelle))

        sykmeldtService.behandleKafkaMeldingLogikk(kafkaMelding)

        val lagret = sykmeldtRepository.hentSykmeldtPeriode(norskIdent.get())
        assertThat(lagret!!.start).isEqualTo(aktivSykmeldtPeriode.start)
        assertThat(lagret.slutt).isEqualTo(aktivSykmeldtPeriode.end)
    }

    @Test
    fun `skal ikke lagre sykmeldtdata når første tilfelle er utløpt`() {
        settBrukerUnderOppfolging()
        mockApiRespons(listOf(utloptSykmeldtPeriode))

        sykmeldtService.behandleKafkaMeldingLogikk(kafkaMelding)

        assertThat(sykmeldtRepository.hentSykmeldtPeriode(norskIdent.get())).isNull()
    }

    @Test
    fun `skal ikke lagre sykmeldtdata når api returnerer tom liste`() {
        settBrukerUnderOppfolging()
        mockApiRespons(emptyList())

        sykmeldtService.behandleKafkaMeldingLogikk(kafkaMelding)

        assertThat(sykmeldtRepository.hentSykmeldtPeriode(norskIdent.get())).isNull()
    }

    @Test
    fun `skal slette lagret sykmeldtdata når bruker ikke lenger er sykmeldt`() {
        settBrukerUnderOppfolging()
        mockApiRespons(listOf(aktivSykmeldtPeriode))
        sykmeldtService.behandleKafkaMeldingLogikk(kafkaMelding)
        assertThat(sykmeldtRepository.hentSykmeldtPeriode(norskIdent.get())).isNotNull

        mockApiRespons(listOf(utloptSykmeldtPeriode))
        sykmeldtService.behandleKafkaMeldingLogikk(kafkaMelding)

        assertThat(sykmeldtRepository.hentSykmeldtPeriode(norskIdent.get())).isNull()
    }

    @Test
    fun `skal ikke behandle kafkamelding når person ikke er under oppfølging`() {
        pdlIdentRepository.upsertIdenter(identerBruker)

        sykmeldtService.behandleKafkaMeldingLogikk(kafkaMelding)

        assertThat(sykmeldtRepository.hentSykmeldtPeriode(norskIdent.get())).isNull()
        verify(sykmeldtClient, never()).hentSykmeldtPeriode(anyString())
    }

    @Test
    fun `skal oppdatere til ny ident og slette gammel rad når kafkamelding med ny ident kommer`() {
        val norskIdentHistorisk = randomNorskIdent()
        pdlIdentRepository.upsertIdenter(
            identerBruker + PDLIdent(norskIdentHistorisk.get(), true, Gruppe.FOLKEREGISTERIDENT)
        )
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2), UUID.randomUUID())
        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
        mockApiRespons(listOf(aktivSykmeldtPeriode))

        sykmeldtService.behandleKafkaMeldingLogikk(kafkaMelding.copy(personIdentNumber = norskIdentHistorisk.get()))
        assertThat(sykmeldtRepository.hentSykmeldtPeriode(norskIdentHistorisk.get())).isNotNull

        sykmeldtService.behandleKafkaMeldingLogikk(kafkaMelding)

        assertThat(sykmeldtRepository.hentSykmeldtPeriode(norskIdentHistorisk.get())).isNull()
        assertThat(sykmeldtRepository.hentSykmeldtPeriode(norskIdent.get())).isNotNull
    }

    @Test
    fun `skal hente og lagre sykmeldtdata ved oppfølgingsstart`() {
        pdlIdentRepository.upsertIdenter(identerBruker)
        `when`(aktorClient.hentFnr(aktorId)).thenReturn(norskIdent)
        mockApiRespons(listOf(aktivSykmeldtPeriode))

        sykmeldtService.hentOgLagreSykmeldtperiodeForBrukerVedOppfolgingStart(aktorId)

        assertThat(sykmeldtRepository.hentSykmeldtPeriode(norskIdent.get())).isNotNull
    }

    @Test
    fun `slettSykmeldtData skal slette sykmeldtdata for alle identer til bruker`() {
        val norskIdentHistorisk = randomNorskIdent()
        pdlIdentRepository.upsertIdenter(
            identerBruker + PDLIdent(norskIdentHistorisk.get(), true, Gruppe.FOLKEREGISTERIDENT)
        )
        sykmeldtRepository.upsertSykmeldtperiode(norskIdent.get(), aktivSykmeldtPeriode)
        sykmeldtRepository.upsertSykmeldtperiode(norskIdentHistorisk.get(), aktivSykmeldtPeriode)

        sykmeldtService.slettSykmeldtData(aktorId, Optional.of(norskIdent))

        assertThat(sykmeldtRepository.hentSykmeldtPeriode(norskIdent.get())).isNull()
        assertThat(sykmeldtRepository.hentSykmeldtPeriode(norskIdentHistorisk.get())).isNull()
    }


    private fun settBrukerUnderOppfolging() {
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now().minusMonths(2), UUID.randomUUID())
        pdlIdentRepository.upsertIdenter(identerBruker)
        `when`(aktorClient.hentAktorId(any())).thenReturn(aktorId)
    }

    private fun mockApiRespons(tilfeller: List<OppfolgingstilfelleDTO>) {
        `when`(sykmeldtClient.hentSykmeldtPeriode(anyString())).thenReturn(
            SykmeldtPersonDTO(
                oppfolgingstilfelleList = tilfeller,
                personIdent = norskIdent.get(),
            )
        )
    }
}
