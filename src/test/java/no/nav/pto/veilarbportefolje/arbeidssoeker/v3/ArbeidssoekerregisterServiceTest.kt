package no.nav.pto.veilarbportefolje.arbeidssoeker.v3

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.pto.veilarbportefolje.arbeidssoeker.v3.domene.toAggregertPeriodeEntity
import no.nav.pto.veilarbportefolje.database.PostgresTable.ARBEIDSSOEKERREGISTER_DATA
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import java.time.ZonedDateTime
import java.util.*
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType as BrukerTypeKafka
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke as JaNeiVetIkkeKafka
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode as PeriodeKafkaMelding
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering as ProfileringKafkaMelding
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil as ProfilertTilKafka
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker as OpplysningerOmArbeidssoekerKafkaMelding
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning as UtdanningKafka

class ArbeidssoekerregisterServiceTest(
    @param:Autowired private val jdbcTemplate: JdbcTemplate,
    @param:Autowired private val pdlIdentRepository: PdlIdentRepository,
    @param:Autowired private val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    @param:Autowired private val opensearchIndexerPaDatafelt: OpensearchIndexerPaDatafelt,
    @param:Autowired private val arbeidssoekerregisterRepository: ArbeidssoekerregisterRepository,
    @param:Autowired private val transactionManager: PlatformTransactionManager
) : EndToEndTest() {
    private lateinit var arbeidssoekerregisterService: ArbeidssoekerregisterService
    private val arbeidssoekerregisterClient: ArbeidssoekerregisterClient = mock()

    private val fnr = Fnr.ofValidFnr("10108000000")
    private val historiskFnr = Fnr.ofValidFnr("01010112345")
    private val aktorId = AktorId.of("12345")
    private val identerBruker = listOf(
        PDLIdent(aktorId.get(), false, Gruppe.AKTORID),
        PDLIdent(fnr.get(), false, Gruppe.FOLKEREGISTERIDENT),
        PDLIdent(historiskFnr.get(), true, Gruppe.FOLKEREGISTERIDENT)
    )

    private val oppfolgingStart = ZonedDateTime.parse("2024-01-01T00:00:00Z")
    private val periodeId = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16")
    private val periodeStart = ZonedDateTime.parse("2024-04-23T13:04:40.739Z")
    private val opplysningTidspunkt = ZonedDateTime.parse("2024-04-23T13:22:58.089Z")
    private val profileringTidspunkt = ZonedDateTime.parse("2024-04-25T08:19:53.612Z")

    @BeforeEach
    fun setUp() {
        listOf(ARBEIDSSOEKERREGISTER_DATA.TABLE_NAME, "oppfolging_data", "bruker_identer")
            .forEach { jdbcTemplate.execute("TRUNCATE TABLE $it") }

        arbeidssoekerregisterService = ArbeidssoekerregisterService(
            arbeidssoekerregisterClient,
            pdlIdentRepository,
            oppfolgingRepositoryV2,
            arbeidssoekerregisterRepository,
            opensearchIndexerPaDatafelt,
            transactionManager
        )

        pdlIdentRepository.upsertIdenter(identerBruker)
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, oppfolgingStart, UUID.randomUUID())
    }

    // hentOgLagreArbeidssoekerdataVedOppfolgingStart

    @Test
    fun `ved oppfolging start skal periode fra api lagres`() {
        val respons = lagAggregertPeriodeResponse()
        mockApiRespons(respons)

        arbeidssoekerregisterService.hentOgLagreArbeidssoekerdataVedOppfolgingStart(aktorId)

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(respons.toAggregertPeriodeEntity())
        assertThat(lagret.synkroniseringStatus).isEqualTo(SynkroniseringStatus.SYNKRONISERT)
    }

    @Test
    fun `ved oppfolging start uten periode i api skal bruker lagres med status usikker`() {
        mockApiRespons(null)

        arbeidssoekerregisterService.hentOgLagreArbeidssoekerdataVedOppfolgingStart(aktorId)

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isNull()
        assertThat(lagret.synkroniseringStatus).isEqualTo(SynkroniseringStatus.USIKKERT_OM_BRUKER_HAR_DATA)
    }

    @Test
    fun `ved oppfolging start med periode avsluttet for oppfolgingsstart skal bruker lagres med status usikker`() {
        mockApiRespons(lagAggregertPeriodeResponse(avsluttet = oppfolgingStart.minusDays(1)))

        arbeidssoekerregisterService.hentOgLagreArbeidssoekerdataVedOppfolgingStart(aktorId)

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isNull()
        assertThat(lagret.synkroniseringStatus).isEqualTo(SynkroniseringStatus.USIKKERT_OM_BRUKER_HAR_DATA)
    }

    // hentOgLagreArbeidssoekerdataVedAdminJobb

    @Test
    fun `ved admin jobb uten eksisterende data skal periode fra api lagres`() {
        val respons = lagAggregertPeriodeResponse()
        mockApiRespons(respons)

        arbeidssoekerregisterService.hentOgLagreArbeidssoekerdataVedAdminJobb(aktorId)

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(respons.toAggregertPeriodeEntity())
    }

    @Test
    fun `ved admin jobb uten periode i api skal eksisterende data ikke endres`() {
        val eksisterende = lagAggregertPeriodeResponse().toAggregertPeriodeEntity()
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, eksisterende)
        mockApiRespons(null)

        arbeidssoekerregisterService.hentOgLagreArbeidssoekerdataVedAdminJobb(aktorId)

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(eksisterende)
        assertThat(lagret.synkroniseringStatus).isEqualTo(SynkroniseringStatus.SYNKRONISERT)
    }

    @Test
    fun `ved admin jobb med samme periode og nyere opplysninger i api skal data oppdateres`() {
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(
            fnr, lagAggregertPeriodeResponse().toAggregertPeriodeEntity()
        )
        val nyereRespons = lagAggregertPeriodeResponse(
            opplysningTidspunkt = opplysningTidspunkt.plusMinutes(1),
            profilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER
        )
        mockApiRespons(nyereRespons)

        arbeidssoekerregisterService.hentOgLagreArbeidssoekerdataVedAdminJobb(aktorId)

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(nyereRespons.toAggregertPeriodeEntity())
    }

    @Test
    fun `ved admin jobb med samme periode og avsluttet tidspunkt i api skal data oppdateres`() {
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(
            fnr, lagAggregertPeriodeResponse().toAggregertPeriodeEntity()
        )
        val avsluttetRespons = lagAggregertPeriodeResponse(avsluttet = ZonedDateTime.parse("2024-06-01T10:00:00Z"))
        mockApiRespons(avsluttetRespons)

        arbeidssoekerregisterService.hentOgLagreArbeidssoekerdataVedAdminJobb(aktorId)

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(avsluttetRespons.toAggregertPeriodeEntity())
    }

    @Test
    fun `ved admin jobb med samme periode og like tidspunkt skal data ikke endres`() {
        val eksisterende = lagAggregertPeriodeResponse().toAggregertPeriodeEntity()
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, eksisterende)
        mockApiRespons(lagAggregertPeriodeResponse(profilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER))

        arbeidssoekerregisterService.hentOgLagreArbeidssoekerdataVedAdminJobb(aktorId)

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(eksisterende)
    }

    @Test
    fun `ved admin jobb skal tidspunkt med hoyere presisjon enn mikrosekunder ikke regnes som nyere`() {
        val eksisterende = lagAggregertPeriodeResponse().toAggregertPeriodeEntity()
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, eksisterende)
        mockApiRespons(
            lagAggregertPeriodeResponse(
                opplysningTidspunkt = opplysningTidspunkt.plusNanos(500),
                profilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER
            )
        )

        arbeidssoekerregisterService.hentOgLagreArbeidssoekerdataVedAdminJobb(aktorId)

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(eksisterende)
    }

    @Test
    fun `ved admin jobb med ny periode som startet senere skal eksisterende erstattes`() {
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(
            fnr, lagAggregertPeriodeResponse().toAggregertPeriodeEntity()
        )
        val nyPeriode = lagAggregertPeriodeResponse(
            periodeId = UUID.randomUUID(),
            startet = periodeStart.plusMonths(2),
            opplysningTidspunkt = periodeStart.plusMonths(2),
            profileringTidspunkt = periodeStart.plusMonths(2)
        )
        mockApiRespons(nyPeriode)

        arbeidssoekerregisterService.hentOgLagreArbeidssoekerdataVedAdminJobb(aktorId)

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(nyPeriode.toAggregertPeriodeEntity())
    }

    @Test
    fun `ved admin jobb med eldre data pa historisk fnr skal gammel rad slettes og ny lagres pa aktivt fnr`() {
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(
            historiskFnr, lagAggregertPeriodeResponse().toAggregertPeriodeEntity()
        )
        val nyereRespons = lagAggregertPeriodeResponse(profileringTidspunkt = profileringTidspunkt.plusDays(1))
        mockApiRespons(nyereRespons)

        arbeidssoekerregisterService.hentOgLagreArbeidssoekerdataVedAdminJobb(aktorId)

        assertThat(arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(historiskFnr)).isNull()
        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(nyereRespons.toAggregertPeriodeEntity())
    }

    // behandleKafkaMeldingLogikk - periode

    @Test
    fun `ny periode pa kafka med api i synk skal lagres med opplysninger og profilering fra api`() {
        val respons = lagAggregertPeriodeResponse()
        mockApiRespons(respons)

        arbeidssoekerregisterService.behandleKafkaMeldingLogikk(lagPeriodeKafkaMelding())

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(respons.toAggregertPeriodeEntity())
        assertThat(lagret.synkroniseringStatus).isEqualTo(SynkroniseringStatus.SYNKRONISERT)
    }

    @Test
    fun `ny periode pa kafka med api ute av synk skal lagres uten opplysninger og profilering`() {
        mockApiRespons(null)

        arbeidssoekerregisterService.behandleKafkaMeldingLogikk(lagPeriodeKafkaMelding())

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode!!.periodeId).isEqualTo(periodeId)
        assertThat(lagret.aggregertPeriode!!.opplysningId).isNull()
        assertThat(lagret.aggregertPeriode!!.profileringId).isNull()
        assertThat(lagret.synkroniseringStatus).isEqualTo(SynkroniseringStatus.API_UTE_AV_SYNC)
    }

    @Test
    fun `avsluttet samme periode pa kafka skal beholde opplysninger og profilering uten kall mot api`() {
        val eksisterende = lagAggregertPeriodeResponse().toAggregertPeriodeEntity()
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, eksisterende)
        val avsluttet = ZonedDateTime.parse("2024-06-01T10:00:00Z")

        arbeidssoekerregisterService.behandleKafkaMeldingLogikk(lagPeriodeKafkaMelding(avsluttet = avsluttet))

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(eksisterende.copy(periodeAvsluttetTidspunkt = avsluttet))
        assertThat(lagret.synkroniseringStatus).isEqualTo(SynkroniseringStatus.SYNKRONISERT)
        verifyNoInteractions(arbeidssoekerregisterClient)
    }

    @Test
    fun `eldre periode pa kafka skal ignoreres`() {
        val eksisterende = lagAggregertPeriodeResponse().toAggregertPeriodeEntity()
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, eksisterende)

        arbeidssoekerregisterService.behandleKafkaMeldingLogikk(
            lagPeriodeKafkaMelding(periodeId = UUID.randomUUID(), startet = periodeStart.minusDays(1))
        )

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(eksisterende)
        verifyNoInteractions(arbeidssoekerregisterClient)
    }

    @Test
    fun `ny periode pa kafka skal slette eksisterende rader pa alle identer`() {
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(
            fnr, lagAggregertPeriodeResponse().toAggregertPeriodeEntity()
        )
        arbeidssoekerregisterRepository.lagreBrukerMedSynkroniseringStatusUsikker(historiskFnr)
        val nyPeriodeId = UUID.randomUUID()
        val nyStart = periodeStart.plusMonths(2)
        val respons = lagAggregertPeriodeResponse(
            periodeId = nyPeriodeId,
            startet = nyStart,
            opplysningTidspunkt = nyStart,
            profileringTidspunkt = nyStart
        )
        mockApiRespons(respons)

        arbeidssoekerregisterService.behandleKafkaMeldingLogikk(
            lagPeriodeKafkaMelding(periodeId = nyPeriodeId, startet = nyStart)
        )

        assertThat(arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(historiskFnr)).isNull()
        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(respons.toAggregertPeriodeEntity())
        assertThat(lagret.synkroniseringStatus).isEqualTo(SynkroniseringStatus.SYNKRONISERT)
    }

    // behandleKafkaMeldingLogikk - opplysninger om arbeidssøker

    @Test
    fun `opplysninger pa kafka for ukjent periode skal ignoreres`() {
        val eksisterende = lagAggregertPeriodeResponse().toAggregertPeriodeEntity()
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, eksisterende)

        arbeidssoekerregisterService.behandleKafkaMeldingLogikk(
            lagOpplysningerKafkaMelding(periodeId = UUID.randomUUID(), sendtInn = opplysningTidspunkt.plusDays(1))
        )

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(eksisterende)
    }

    @Test
    fun `nyere opplysninger pa kafka skal lagres pa eksisterende periode`() {
        val eksisterende = lagAggregertPeriodeResponse().toAggregertPeriodeEntity()
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, eksisterende)
        val melding = lagOpplysningerKafkaMelding(sendtInn = opplysningTidspunkt.plusDays(1))

        arbeidssoekerregisterService.behandleKafkaMeldingLogikk(melding)

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(
            eksisterende.copy(
                opplysningId = melding.id,
                opplysningJobbsituasjonBeskrivelse = listOf(Beskrivelse.HAR_SAGT_OPP.name),
                opplysningUtdanningNusKode = 6,
                opplysningUtdanningBestatt = JaNeiVetIkkeKafka.NEI.name,
                opplysningUtdanningGodkjent = JaNeiVetIkkeKafka.VET_IKKE.name,
                opplysningSendtInnTidspunkt = opplysningTidspunkt.plusDays(1)
            )
        )
    }

    @Test
    fun `eldre opplysninger pa kafka skal ignoreres`() {
        val eksisterende = lagAggregertPeriodeResponse().toAggregertPeriodeEntity()
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, eksisterende)

        arbeidssoekerregisterService.behandleKafkaMeldingLogikk(
            lagOpplysningerKafkaMelding(sendtInn = opplysningTidspunkt.minusDays(1))
        )

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(eksisterende)
    }

    @Test
    fun `opplysninger pa kafka med samme tidspunkt som eksisterende skal ignoreres`() {
        val eksisterende = lagAggregertPeriodeResponse().toAggregertPeriodeEntity()
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, eksisterende)

        arbeidssoekerregisterService.behandleKafkaMeldingLogikk(
            lagOpplysningerKafkaMelding(sendtInn = opplysningTidspunkt.plusNanos(500))
        )

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(eksisterende)
    }

    // behandleKafkaMeldingLogikk - profilering

    @Test
    fun `profilering pa kafka for ukjent periode skal ignoreres`() {
        val eksisterende = lagAggregertPeriodeResponse().toAggregertPeriodeEntity()
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, eksisterende)

        arbeidssoekerregisterService.behandleKafkaMeldingLogikk(
            lagProfileringKafkaMelding(periodeId = UUID.randomUUID(), sendtInn = profileringTidspunkt.plusDays(1))
        )

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(eksisterende)
    }

    @Test
    fun `nyere profilering pa kafka skal lagres pa eksisterende periode`() {
        val eksisterende = lagAggregertPeriodeResponse().toAggregertPeriodeEntity()
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, eksisterende)
        val melding = lagProfileringKafkaMelding(sendtInn = profileringTidspunkt.plusDays(1))

        arbeidssoekerregisterService.behandleKafkaMeldingLogikk(melding)

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(
            eksisterende.copy(
                profileringId = melding.id,
                profileringResultat = ProfilertTilKafka.ANTATT_GODE_MULIGHETER.name,
                profileringSendtInnTidspunkt = profileringTidspunkt.plusDays(1)
            )
        )
    }

    @Test
    fun `eldre profilering pa kafka skal ignoreres`() {
        val eksisterende = lagAggregertPeriodeResponse().toAggregertPeriodeEntity()
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, eksisterende)

        arbeidssoekerregisterService.behandleKafkaMeldingLogikk(
            lagProfileringKafkaMelding(sendtInn = profileringTidspunkt.minusDays(1))
        )

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(eksisterende)
    }

    @Test
    fun `profilering pa kafka med samme tidspunkt som eksisterende skal ignoreres`() {
        val eksisterende = lagAggregertPeriodeResponse().toAggregertPeriodeEntity()
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, eksisterende)

        arbeidssoekerregisterService.behandleKafkaMeldingLogikk(
            lagProfileringKafkaMelding(sendtInn = profileringTidspunkt.plusNanos(500))
        )

        val lagret = arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(fnr)
        assertThat(lagret!!.aggregertPeriode).isEqualTo(eksisterende)
    }

    private fun lagPeriodeKafkaMelding(
        periodeId: UUID = this.periodeId,
        startet: ZonedDateTime = periodeStart,
        avsluttet: ZonedDateTime? = null
    ) = PeriodeKafkaMelding(
        periodeId,
        fnr.get(),
        Metadata(startet.toInstant(), Bruker(BrukerTypeKafka.SYSTEM, "test"), "test", "test"),
        avsluttet?.let { Metadata(it.toInstant(), Bruker(BrukerTypeKafka.SYSTEM, "test"), "test", "test") }
    )

    private fun lagProfileringKafkaMelding(
        periodeId: UUID = this.periodeId,
        sendtInn: ZonedDateTime
    ) = ProfileringKafkaMelding(
        UUID.randomUUID(),
        periodeId,
        UUID.randomUUID(),
        Metadata(sendtInn.toInstant(), Bruker(BrukerTypeKafka.SYSTEM, "test"), "test", "test"),
        ProfilertTilKafka.ANTATT_GODE_MULIGHETER,
        true,
        30
    )

    private fun lagOpplysningerKafkaMelding(
        periodeId: UUID = this.periodeId,
        sendtInn: ZonedDateTime
    ) = OpplysningerOmArbeidssoekerKafkaMelding(
        UUID.randomUUID(),
        periodeId,
        Metadata(sendtInn.toInstant(), Bruker(BrukerTypeKafka.SLUTTBRUKER, fnr.get()), "test", "test"),
        UtdanningKafka("6", JaNeiVetIkkeKafka.NEI, JaNeiVetIkkeKafka.VET_IKKE),
        Helse(JaNeiVetIkkeKafka.NEI),
        Jobbsituasjon(listOf(BeskrivelseMedDetaljer(Beskrivelse.HAR_SAGT_OPP, emptyMap()))),
        Annet(JaNeiVetIkkeKafka.NEI)
    )

    private fun mockApiRespons(respons: AggregertPeriodeResponse?) {
        `when`(arbeidssoekerregisterClient.hentAggregertArbeidssokerPeriode(fnr.get())).thenReturn(respons)
    }

    private fun lagAggregertPeriodeResponse(
        periodeId: UUID = this.periodeId,
        startet: ZonedDateTime = periodeStart,
        avsluttet: ZonedDateTime? = null,
        opplysningTidspunkt: ZonedDateTime = this.opplysningTidspunkt,
        profileringTidspunkt: ZonedDateTime = this.profileringTidspunkt,
        profilertTil: ProfilertTil = ProfilertTil.OPPGITT_HINDRINGER
    ): AggregertPeriodeResponse {
        val opplysningId = UUID.fromString("913161a3-dde9-4448-abf8-2a01a043f8cd")
        return AggregertPeriodeResponse(
            id = periodeId,
            identitetsnummer = fnr.get(),
            startet = PeriodeHendelseResponse(sendtInnAv = lagMetadata(startet), tidspunkt = startet),
            avsluttet = avsluttet?.let { PeriodeHendelseResponse(sendtInnAv = lagMetadata(it), tidspunkt = it) },
            opplysning = OpplysningerResponse(
                id = opplysningId,
                sendtInnAv = lagMetadata(opplysningTidspunkt),
                utdanning = UtdanningResponse(nus = "3", bestaatt = JaNeiVetIkke.JA, godkjent = JaNeiVetIkke.JA),
                helse = null,
                jobbsituasjon = JobbsituasjonResponse(
                    beskrivelser = listOf(
                        BeskrivelseMedDetaljerResponse(
                            beskrivelse = JobbSituasjonBeskrivelseResponse.ER_PERMITTERT,
                            detaljer = emptyMap()
                        )
                    )
                ),
                annet = null,
                tidspunkt = opplysningTidspunkt
            ),
            profilering = ProfileringResponse(
                id = UUID.fromString("7c9a2feb-0b31-4101-825a-0c4a3d465e01"),
                opplysningerOmArbeidssokerId = opplysningId,
                sendtInnAv = lagMetadata(profileringTidspunkt),
                profilertTil = profilertTil,
                jobbetSammenhengendeSeksAvTolvSisteMnd = false,
                alder = 33,
                tidspunkt = profileringTidspunkt
            ),
            egenvurdering = null,
            bekreftelse = null
        )
    }

    private fun lagMetadata(tidspunkt: ZonedDateTime) = MetadataResponse(
        tidspunkt = tidspunkt,
        utfoertAv = BrukerResponse(type = BrukerType.SYSTEM, id = "test", sikkerhetsnivaa = null),
        kilde = "test",
        aarsak = "test",
        tidspunktFraKilde = null
    )
}
