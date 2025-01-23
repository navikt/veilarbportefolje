package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.database.PostgresTable.SISTE_ARBEIDSSOEKER_PERIODE
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingPeriodeService
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerDTO
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.VeilarbarenaClient
import no.nav.pto.veilarbportefolje.persononinfo.PdlPortefoljeClient
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlBarnResponse
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlPersonResponse
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPersonBarn
import no.nav.pto.veilarbportefolje.postgres.PostgresUtils
import no.nav.pto.veilarbportefolje.util.DateUtils
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.TestDataClient.Companion.getArbeidssoekerPeriodeFraDb
import no.nav.pto.veilarbportefolje.util.TestDataClient.Companion.getOpplysningerOmArbeidssoekerFraDb
import no.nav.pto.veilarbportefolje.util.TestDataClient.Companion.getOpplysningerOmArbeidssoekerJobbsituasjonFraDb
import no.nav.pto.veilarbportefolje.util.TestDataClient.Companion.getProfileringFraDb
import no.nav.pto.veilarbportefolje.util.TestDataUtils
import no.nav.pto.veilarbportefolje.util.TestDataUtils.genererStartetOppfolgingsperiode
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
import no.nav.pto.veilarbportefolje.util.TestUtil
import no.nav.pto.veilarbportefolje.util.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke as JaNeiVetIkkeEkstern
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering as ProfileringKafkamelding
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker as OpplysningerOmArbeidssoekerKafkamelding

@SpringBootTest(classes = [ApplicationConfigTest::class])
class ArbeidssoekerServiceTest(
    @Autowired private val db: JdbcTemplate,
    @Autowired private val sisteArbeidssoekerPeriodeRepository: SisteArbeidssoekerPeriodeRepository,
    @Autowired private val opplysningerOmArbeidssoekerRepository: OpplysningerOmArbeidssoekerRepository,
    @Autowired private val profileringRepository: ProfileringRepository,
    @Autowired private val oppslagArbeidssoekerregisteretClient: OppslagArbeidssoekerregisteretClient,
    @Autowired private val arbeidssoekerService: ArbeidssoekerService,
    @Autowired private val oppfolgingPeriodeService: OppfolgingPeriodeService,
) : EndToEndTest() {

    @MockBean
    private lateinit var pdlPortefoljeClient: PdlPortefoljeClient

    @MockBean
    private lateinit var veilarbarenaClient: VeilarbarenaClient

    @BeforeEach
    fun setup() {
        db.update("truncate TABLE ${SISTE_ARBEIDSSOEKER_PERIODE.TABLE_NAME} CASCADE")
    }

    @Test
    fun slettArbeidssoekerData_skal_slette_all_relatert_data_for_bruker() {
        // Arrange
        val aktorId = randomAktorId()
        val fnr1 = TestDataUtils.randomFnr()
        val fnr2 = TestDataUtils.randomFnr()
        val periodeId1 = UUID.randomUUID()
        val opplysningerOmArbeidssoekerId1 = UUID.randomUUID()
        val periodeId2 = UUID.randomUUID()
        val opplysningerOmArbeidssoekerId2 = UUID.randomUUID()
        pdlIdentRepository.upsertIdenter(
            listOf(
                PDLIdent(fnr1.get(), false, PDLIdent.Gruppe.FOLKEREGISTERIDENT),
                PDLIdent(aktorId.get(), false, PDLIdent.Gruppe.AKTORID)
            )
        )
        sisteArbeidssoekerPeriodeRepository.insertSisteArbeidssoekerPeriode(ArbeidssoekerPeriodeEntity(periodeId1, fnr1.get()))
        sisteArbeidssoekerPeriodeRepository.insertSisteArbeidssoekerPeriode(ArbeidssoekerPeriodeEntity(periodeId2, fnr2.get()))
        opplysningerOmArbeidssoekerRepository.insertOpplysningerOmArbeidssoekerOgJobbsituasjon(
            genererRandomOpplysningerOmArbeidssoekerEntity(periodeId1, opplysningerOmArbeidssoekerId1)
        )
        opplysningerOmArbeidssoekerRepository.insertOpplysningerOmArbeidssoekerOgJobbsituasjon(
            genererRandomOpplysningerOmArbeidssoekerEntity(periodeId2, opplysningerOmArbeidssoekerId2)
        )
        profileringRepository.insertProfilering(
            ProfileringEntity(
                periodeId1,
                Profileringsresultat.ANTATT_GODE_MULIGHETER.name,
                DateUtils.toTimestamp(ZonedDateTime.now())
            )
        )
        profileringRepository.insertProfilering(
            ProfileringEntity(
                periodeId2,
                Profileringsresultat.OPPGITT_HINDRINGER.name,
                DateUtils.toTimestamp(ZonedDateTime.now())
            )
        )

        // Act
        arbeidssoekerService.slettArbeidssoekerData(aktorId, Optional.of(fnr1))

        // Assert
        val sisteArbeidssoekerPeriode1 = getArbeidssoekerPeriodeFraDb(db, periodeId1)
        assertThat(sisteArbeidssoekerPeriode1).isNull()
        val sisteArbeidssoekerPeriode2 = getArbeidssoekerPeriodeFraDb(db, periodeId2)
        assertThat(sisteArbeidssoekerPeriode2).isNotNull()

        val opplysningerOmArbeidssoeker1 = getOpplysningerOmArbeidssoekerFraDb(db, periodeId1)
        assertThat(opplysningerOmArbeidssoeker1).isNull()
        val opplysningerOmArbeidssoeker2 = getOpplysningerOmArbeidssoekerFraDb(db, periodeId2)
        assertThat(opplysningerOmArbeidssoeker2).isNotNull()

        val opplysningerOmArbeidssoekerJobbsituasjon1 =
            getOpplysningerOmArbeidssoekerJobbsituasjonFraDb(db, opplysningerOmArbeidssoekerId1)
        assertThat(opplysningerOmArbeidssoekerJobbsituasjon1).isNull()
        val opplysningerOmArbeidssoekerJobbsituasjon2 =
            getOpplysningerOmArbeidssoekerJobbsituasjonFraDb(db, opplysningerOmArbeidssoekerId2)
        assertThat(opplysningerOmArbeidssoekerJobbsituasjon2).isNotNull()
        val profilering1 = getProfileringFraDb(db, periodeId1)
        assertThat(profilering1).isNull()
        val profilering2 = getProfileringFraDb(db, periodeId2)
        assertThat(profilering2).isNotNull()
    }


    @Test
    fun meldinger_om_periode_og_opplysninger_om_arbeidssoeker_skal_ignoreres_dersom_bruker_ikke_er_under_oppfolging() {
        val periodeId = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16")
        val opplysningerOmArbeidssoekerId = UUID.fromString("913161a3-dde9-4448-abf8-2a01a043f8cd")
        val fnr = Fnr.of("17858998980")
        val aktorId = randomAktorId()

        mockPdlIdenterRespons(aktorId, fnr)
        mockPdlPersonRespons(fnr)
        mockPdlPersonBarnRespons()
        mockHentOppfolgingsbrukerResponse(fnr)
        mockHentArbeidssoekerPerioderResponse(fnr)
        mockHentOpplysningerOmArbeidssoekerResponse(fnr, periodeId)
        mockHentProfileringResponse(fnr, periodeId)

        val arbeidssoekerPeriodeMelding = Periode(
            periodeId,
            fnr.get(),
            Metadata(
                Instant.now(),
                Bruker(
                    BrukerType.SYSTEM,
                    "APP_NAVN:VERSJON"
                ),
                "APP_NAVN:VERSJON",
                "startet periode"
            ),
            null
        )
        arbeidssoekerService.behandleKafkaMeldingLogikk(arbeidssoekerPeriodeMelding)

        val periodeEksistererIDb: Boolean = (PostgresUtils.queryForObjectOrNull {
            db.queryForObject(
                "SELECT COUNT(*) FROM ${SISTE_ARBEIDSSOEKER_PERIODE.TABLE_NAME} WHERE ${SISTE_ARBEIDSSOEKER_PERIODE.ARBEIDSSOKER_PERIODE_ID} = ?",
                Int::class.java,
                periodeId
            )
        } ?: 0) > 0

        assertFalse(periodeEksistererIDb)


        val opplysningerOmArbeidssoekerKafkaMelding = OpplysningerOmArbeidssoekerKafkamelding(
            opplysningerOmArbeidssoekerId,
            periodeId,
            Metadata(
                Instant.now(),
                Bruker(
                    BrukerType.SYSTEM,
                    "APP_NAVN:VERSJON"
                ),
                "APP_NAVN:VERSJON",
                "startet periode"
            ),
            Utdanning("6", JaNeiVetIkkeEkstern.JA, JaNeiVetIkkeEkstern.JA),
            Helse(JaNeiVetIkkeEkstern.JA),
            Jobbsituasjon(listOf(BeskrivelseMedDetaljer())),
            Annet(JaNeiVetIkkeEkstern.JA)
        )

        arbeidssoekerService.behandleKafkaMeldingLogikk(opplysningerOmArbeidssoekerKafkaMelding)
        val harOpplysninger =
            opplysningerOmArbeidssoekerRepository.harSisteOpplysningerOmArbeidssoeker(opplysningerOmArbeidssoekerId)
        assertFalse(harOpplysninger)

        val profileringKafkamelding = ProfileringKafkamelding(
            UUID.randomUUID(),
            periodeId,
            opplysningerOmArbeidssoekerId,
            Metadata(
                Instant.now(),
                Bruker(
                    BrukerType.SYSTEM,
                    "APP_NAVN:VERSJON"
                ),
                "APP_NAVN:VERSJON",
                "startet periode"
            ),
            ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
            true,
            35
        )

        arbeidssoekerService.behandleKafkaMeldingLogikk(profileringKafkamelding)
        val harProfilering = getProfileringFraDb(db, periodeId) != null
        assertFalse(harProfilering)

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererStartetOppfolgingsperiode(aktorId))

        val arbeidssoekerPeriode = getArbeidssoekerPeriodeFraDb(db, periodeId)

        assertNotNull(arbeidssoekerPeriode)
        val opplysningerOmArbeidssoeker =
            getOpplysningerOmArbeidssoekerFraDb(db, arbeidssoekerPeriode!!.arbeidssoekerperiodeId)

        assertNotNull(opplysningerOmArbeidssoeker)
        val opplysningerOmArbeidssoekerJobbsituasjon = getOpplysningerOmArbeidssoekerJobbsituasjonFraDb(
            db,
            opplysningerOmArbeidssoeker!!.opplysningerOmArbeidssoekerId
        )

        assertThat(opplysningerOmArbeidssoekerJobbsituasjon!!.jobbsituasjon[1]).isEqualTo(JobbSituasjonBeskrivelse.ER_PERMITTERT.name)

        val profilering = getProfileringFraDb(db, periodeId)
        assertNotNull(profilering)
    }

    @Test
    fun ved_kafkamelding_om_ny_arbeidssoekerperiode_slettes_gammel_arbeidssokerdata_og_ny_lagres() {
        /* Gitt at:
         - Bruker er under oppfølging
         - Bruker har arbeidssøkerdata (siste arbeidssøkerperiode, opplysninger om arbeidssøker og profilering) lagret i databasen

         Når:
         - Det kommer en melding om ny arbeidssøkerperiode

         Så:
         - Skal gammel arbeidssøkerdata slettes fra databasen
         - Skal ny arbeidssøkerdata lagres i databasen
         */

        // Arrange
        val gammelPeriodeId = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16")
        val nyPeriodeId = UUID.randomUUID()
        val gammelOpplysningerOmArbeidssoekerId = UUID.fromString("913161a3-dde9-4448-abf8-2a01a043f8cd")
        val nyOpplysningerOmArbeidssoekerId = UUID.randomUUID()
        val fnr = Fnr.of("17858998980")
        val aktorId = randomAktorId()

        mockPdlIdenterRespons(aktorId, fnr)
        mockPdlPersonRespons(fnr)
        mockPdlPersonBarnRespons()
        mockHentOppfolgingsbrukerResponse(fnr)
        mockHentArbeidssoekerPerioderResponse(fnr)
        mockHentOpplysningerOmArbeidssoekerResponse(fnr, gammelPeriodeId)
        mockHentProfileringResponse(fnr, gammelPeriodeId)

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererStartetOppfolgingsperiode(aktorId))

        mockHentArbeidssoekerPerioderResponse(fnr, nyPeriodeId)
        mockHentOpplysningerOmArbeidssoekerResponse(fnr, nyPeriodeId, nyOpplysningerOmArbeidssoekerId)
        mockHentProfileringResponse(fnr, nyPeriodeId, nyOpplysningerOmArbeidssoekerId)

        val arbeidssoekerPeriodeMelding = Periode(
            nyPeriodeId,
            fnr.get(),
            Metadata(
                Instant.now(), null, null, null
            ),
            null
        )

        // Act
        arbeidssoekerService.behandleKafkaMeldingLogikk(arbeidssoekerPeriodeMelding)

        // Assert
        val gammelPeriodeEksistererIDb: Boolean = (PostgresUtils.queryForObjectOrNull {
            db.queryForObject(
                "SELECT COUNT(*) FROM ${SISTE_ARBEIDSSOEKER_PERIODE.TABLE_NAME} WHERE ${SISTE_ARBEIDSSOEKER_PERIODE.ARBEIDSSOKER_PERIODE_ID} = ?",
                Int::class.java,
                gammelPeriodeId
            )
        } ?: 0) > 0
        val nyPeriodeEksistererIDb: Boolean = (PostgresUtils.queryForObjectOrNull {
            db.queryForObject(
                "SELECT COUNT(*) FROM ${SISTE_ARBEIDSSOEKER_PERIODE.TABLE_NAME} WHERE ${SISTE_ARBEIDSSOEKER_PERIODE.ARBEIDSSOKER_PERIODE_ID} = ?",
                Int::class.java,
                nyPeriodeId
            )
        } ?: 0) > 0
        val harGammelOpplysninger = opplysningerOmArbeidssoekerRepository.harSisteOpplysningerOmArbeidssoeker(
            gammelOpplysningerOmArbeidssoekerId
        )
        val harNyOpplysninger =
            opplysningerOmArbeidssoekerRepository.harSisteOpplysningerOmArbeidssoeker(nyOpplysningerOmArbeidssoekerId)
        val harGammelProfilering = getProfileringFraDb(db, gammelPeriodeId) != null
        val harNyProfilering = getProfileringFraDb(db, nyPeriodeId) != null

        assertFalse(gammelPeriodeEksistererIDb)
        assertTrue(nyPeriodeEksistererIDb)
        assertFalse(harGammelOpplysninger)
        assertTrue(harNyOpplysninger)
        assertFalse(harGammelProfilering)
        assertTrue(harNyProfilering)
    }

    @Test
    fun ved_kafkamelding_om_nye_opplysninger_om_arbeidssoeker_paa_ny_arbeidssoekerperiode_ignoreres_meldingen_dersom_vi_ikke_har_data_paa_periodeId() {
        // Arrange
        val periodeIdVedOppfolgingStartet = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16")
        val nyPeriodeId = UUID.randomUUID()
        val opplysningerOmArbeidssoekerIdVedOppfolgingStartet = UUID.fromString("913161a3-dde9-4448-abf8-2a01a043f8cd")
        val nyOpplysningerOmArbeidssoekerId = UUID.randomUUID()
        val fnr = Fnr.of("17858998980")
        val aktorId = randomAktorId()

        mockPdlIdenterRespons(aktorId, fnr)
        mockPdlPersonRespons(fnr)
        mockPdlPersonBarnRespons()
        mockHentOppfolgingsbrukerResponse(fnr)
        mockHentArbeidssoekerPerioderResponse(fnr)
        mockHentOpplysningerOmArbeidssoekerResponse(fnr, periodeIdVedOppfolgingStartet)
        mockHentProfileringResponse(fnr, periodeIdVedOppfolgingStartet)

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererStartetOppfolgingsperiode(aktorId))

        val opplysningerOmArbeidssoekerKafkamelding = OpplysningerOmArbeidssoekerKafkamelding(
            nyOpplysningerOmArbeidssoekerId,
            nyPeriodeId,
            Metadata(
                Instant.now(),
                Bruker(
                    BrukerType.SYSTEM,
                    "APP_NAVN:VERSJON"
                ),
                "APP_NAVN:VERSJON",
                "startet periode"
            ),
            Utdanning("6", JaNeiVetIkkeEkstern.JA, JaNeiVetIkkeEkstern.JA),
            Helse(JaNeiVetIkkeEkstern.JA),
            Jobbsituasjon(listOf(BeskrivelseMedDetaljer())),
            Annet(JaNeiVetIkkeEkstern.JA)
        )

        // Act
        arbeidssoekerService.behandleKafkaMeldingLogikk(opplysningerOmArbeidssoekerKafkamelding)

        // Assert
        val harFortsattOpplysningeneFraOppfolgingStartet =
            opplysningerOmArbeidssoekerRepository.harSisteOpplysningerOmArbeidssoeker(
                opplysningerOmArbeidssoekerIdVedOppfolgingStartet
            )

        val harOpplysningerFraNyOpplysningerOmArbeidssoekerperiode =
            opplysningerOmArbeidssoekerRepository.harSisteOpplysningerOmArbeidssoeker(
                nyOpplysningerOmArbeidssoekerId
            )

        assertTrue(harFortsattOpplysningeneFraOppfolgingStartet)
        assertFalse(harOpplysningerFraNyOpplysningerOmArbeidssoekerperiode)

    }

    @Test
    fun ved_kafkamelding_om_nye_opplysninger_om_arbeidssoeker_paa_eksisterende_arbeidssoekerperiode_slettes_gammle_opplysninger_om_arbiedssoeker_og_ny_lagres() {
        // Arrange
        val periodeIdVedOppfolgingStartet = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16")
        val opplysningerOmArbeidssoekerIdVedOppfolgingStartet = UUID.fromString("913161a3-dde9-4448-abf8-2a01a043f8cd")
        val nyOpplysningerOmArbeidssoekerId = UUID.randomUUID()
        val fnr = Fnr.of("17858998980")
        val aktorId = randomAktorId()

        mockPdlIdenterRespons(aktorId, fnr)
        mockPdlPersonRespons(fnr)
        mockPdlPersonBarnRespons()
        mockHentOppfolgingsbrukerResponse(fnr)
        mockHentArbeidssoekerPerioderResponse(fnr)
        mockHentOpplysningerOmArbeidssoekerResponse(fnr, periodeIdVedOppfolgingStartet)
        mockHentProfileringResponse(fnr, periodeIdVedOppfolgingStartet)

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererStartetOppfolgingsperiode(aktorId))

        val opplysningerOmArbeidssoekerKafkamelding = OpplysningerOmArbeidssoekerKafkamelding(
            nyOpplysningerOmArbeidssoekerId,
            periodeIdVedOppfolgingStartet,
            Metadata(
                Instant.now(),
                Bruker(
                    BrukerType.SYSTEM,
                    "APP_NAVN:VERSJON"
                ),
                "APP_NAVN:VERSJON",
                "startet periode"
            ),
            Utdanning("8", JaNeiVetIkkeEkstern.NEI, JaNeiVetIkkeEkstern.JA),
            Helse(JaNeiVetIkkeEkstern.JA),
            Jobbsituasjon(listOf(BeskrivelseMedDetaljer(Beskrivelse.DELTIDSJOBB_VIL_MER, mapOf("prosent" to "100")))),
            Annet(JaNeiVetIkkeEkstern.JA)
        )

        // Act
        arbeidssoekerService.behandleKafkaMeldingLogikk(opplysningerOmArbeidssoekerKafkamelding)

        // Assert
        val harSlettetGammleOpplysningerOmArbeidssoeker =
            !opplysningerOmArbeidssoekerRepository.harSisteOpplysningerOmArbeidssoeker(
                opplysningerOmArbeidssoekerIdVedOppfolgingStartet
            )
        val lagredeOpplysningerOmArbeidssoeker = getOpplysningerOmArbeidssoekerFraDb(db, periodeIdVedOppfolgingStartet)
        val lagredeOpplysningerOmArbeidssoekerJobbsituasjon = getOpplysningerOmArbeidssoekerJobbsituasjonFraDb(
            db,
            nyOpplysningerOmArbeidssoekerId
        )

        assertTrue(harSlettetGammleOpplysningerOmArbeidssoeker)
        assertNotNull(lagredeOpplysningerOmArbeidssoeker)
        assertThat(lagredeOpplysningerOmArbeidssoeker!!.opplysningerOmArbeidssoekerId).isEqualTo(
            nyOpplysningerOmArbeidssoekerId
        )
        assertNotNull(lagredeOpplysningerOmArbeidssoekerJobbsituasjon)
        assertThat(lagredeOpplysningerOmArbeidssoekerJobbsituasjon!!.opplysningerOmArbeidssoekerId).isEqualTo(
            nyOpplysningerOmArbeidssoekerId
        )
        assertThat(lagredeOpplysningerOmArbeidssoekerJobbsituasjon.jobbsituasjon[0]).isEqualTo(JobbSituasjonBeskrivelse.DELTIDSJOBB_VIL_MER.name)
    }

    @Test
    fun ved_kafkamelding_om_ny_profilering_for_arbeidssoeker_paa_eksisterende_arbeidssoekerperiode_slettes_gammel_profilering_om_arbiedssoeker_og_ny_lagres() {
        // Arrange
        val periodeIdVedOppfolgingStartet = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16")
        val nyOpplysningerOmArbeidssoekerId = UUID.randomUUID()
        val fnr = Fnr.of("17858998980")
        val aktorId = randomAktorId()

        mockPdlIdenterRespons(aktorId, fnr)
        mockPdlPersonRespons(fnr)
        mockPdlPersonBarnRespons()
        mockHentOppfolgingsbrukerResponse(fnr)
        mockHentArbeidssoekerPerioderResponse(fnr)
        mockHentOpplysningerOmArbeidssoekerResponse(fnr, periodeIdVedOppfolgingStartet)
        mockHentProfileringResponse(fnr, periodeIdVedOppfolgingStartet)

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererStartetOppfolgingsperiode(aktorId))
        val lagretProfileringForKafkaMelding = getProfileringFraDb(db, periodeIdVedOppfolgingStartet)
        assertThat(lagretProfileringForKafkaMelding!!.profileringsresultat).isEqualTo(Profileringsresultat.OPPGITT_HINDRINGER.name)

        val profileringKafkamelding = ProfileringKafkamelding(
            UUID.randomUUID(),
            periodeIdVedOppfolgingStartet,
            nyOpplysningerOmArbeidssoekerId,
            Metadata(
                Instant.now(),
                Bruker(
                    BrukerType.SYSTEM,
                    "APP_NAVN:VERSJON"
                ),
                "APP_NAVN:VERSJON",
                "startet periode"
            ),
            ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
            true,
            35
        )

        // Act
        arbeidssoekerService.behandleKafkaMeldingLogikk(profileringKafkamelding)

        // Assert
        val lagretProfileringEtterMelding = getProfileringFraDb(db, periodeIdVedOppfolgingStartet)
        assertThat(lagretProfileringEtterMelding!!.profileringsresultat).isEqualTo(Profileringsresultat.ANTATT_BEHOV_FOR_VEILEDNING.name)
    }

    @Test
    fun ved_kafkamelding_om_ny_profilering_for_arbeidssoeker_paa_ny_arbeidssoekerperiode_ignoreres_dersom_vi_ikke_har_priodeId() {
        // Arrange
        val periodeIdVedOppfolgingStartet = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16")
        val periodeIdVedEndring = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab20a45")
        val nyOpplysningerOmArbeidssoekerId = UUID.randomUUID()
        val fnr = Fnr.of("17858998980")
        val aktorId = randomAktorId()

        mockPdlIdenterRespons(aktorId, fnr)
        mockPdlPersonRespons(fnr)
        mockPdlPersonBarnRespons()
        mockHentOppfolgingsbrukerResponse(fnr)
        mockHentArbeidssoekerPerioderResponse(fnr)
        mockHentOpplysningerOmArbeidssoekerResponse(fnr, periodeIdVedOppfolgingStartet)
        mockHentProfileringResponse(fnr, periodeIdVedOppfolgingStartet)

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererStartetOppfolgingsperiode(aktorId))
        val lagretProfileringForKafkaMelding = getProfileringFraDb(db, periodeIdVedOppfolgingStartet)
        assertThat(lagretProfileringForKafkaMelding!!.profileringsresultat).isEqualTo(Profileringsresultat.OPPGITT_HINDRINGER.name)
        assertThat(lagretProfileringForKafkaMelding.periodeId).isEqualTo(periodeIdVedOppfolgingStartet)

        val profileringKafkamelding = ProfileringKafkamelding(
            UUID.randomUUID(),
            periodeIdVedEndring,
            nyOpplysningerOmArbeidssoekerId,
            Metadata(
                Instant.now(),
                Bruker(
                    BrukerType.SYSTEM,
                    "APP_NAVN:VERSJON"
                ),
                "APP_NAVN:VERSJON",
                "startet periode"
            ),
            ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
            true,
            35
        )

        // Act
        arbeidssoekerService.behandleKafkaMeldingLogikk(profileringKafkamelding)

        // Assert
        val lagretProfileringEtterMeldingSkalVereSamme = getProfileringFraDb(db, periodeIdVedOppfolgingStartet)
        assertThat(lagretProfileringEtterMeldingSkalVereSamme!!.profileringsresultat).isEqualTo(Profileringsresultat.OPPGITT_HINDRINGER.name)
        assertThat(lagretProfileringEtterMeldingSkalVereSamme.periodeId).isEqualTo(periodeIdVedOppfolgingStartet)

    }

    private fun mockPdlIdenterRespons(aktorId: AktorId, fnr: Fnr) {
        val identer = listOf(
            PDLIdent(aktorId.get(), false, PDLIdent.Gruppe.AKTORID),
            PDLIdent(fnr.get(), false, PDLIdent.Gruppe.FOLKEREGISTERIDENT)
        )

        `when`<List<PDLIdent>>(pdlPortefoljeClient.hentIdenterFraPdl(aktorId)).thenReturn(identer)
    }

    private fun mockPdlPersonRespons(fnr: Fnr): PDLPerson {
        val file = TestUtil.readFileAsJsonString("/PDL_Files/person_pdl.json", javaClass)
        val pdlPerson = PDLPerson.genererFraApiRespons(
            JsonUtils.fromJson(file, PdlPersonResponse::class.java).data.hentPerson
        )

        `when`(pdlPortefoljeClient.hentBrukerDataFraPdl(fnr)).thenReturn(pdlPerson)

        return pdlPerson
    }

    private fun mockPdlPersonBarnRespons(): PDLPersonBarn {
        val file = TestUtil.readFileAsJsonString("/PDL_Files/person_barn_pdl.json", javaClass)
        val pdlPersonBarn = PDLPersonBarn.genererFraApiRespons(
            JsonUtils.fromJson(file, PdlBarnResponse::class.java).data.hentPerson
        )

        `when`(pdlPortefoljeClient.hentBrukerBarnDataFraPdl(ArgumentMatchers.any())).thenReturn(pdlPersonBarn)

        return pdlPersonBarn
    }

    private fun mockHentOppfolgingsbrukerResponse(fnr: Fnr) {
        val file = TestUtil.readFileAsJsonString("/oppfolgingsbruker.json", javaClass)
        val oppfolgingsbrukerDTO = JsonUtils.fromJson(
            file,
            OppfolgingsbrukerDTO::class.java
        )
        `when`(veilarbarenaClient.hentOppfolgingsbruker(fnr)).thenReturn(
            Optional.of(
                oppfolgingsbrukerDTO
            )
        )
    }

    @Throws(JsonProcessingException::class)
    private fun mockHentArbeidssoekerPerioderResponse(fnr: Fnr, periodeId: UUID? = null) {
        val file = TestUtil.readFileAsJsonString("/arbeidssoekerperioder.json", javaClass)
        val arbeidssoekerResponse: List<ArbeidssokerperiodeResponse> =
            objectMapper.readValue(
                file,
                object : TypeReference<List<ArbeidssokerperiodeResponse>>() {
                })
        `when`(oppslagArbeidssoekerregisteretClient.hentArbeidssokerPerioder(fnr.get())).thenReturn(
            arbeidssoekerResponse.map { it.copy(periodeId = periodeId ?: it.periodeId) }
        )
    }

    @Throws(JsonProcessingException::class)
    private fun mockHentOpplysningerOmArbeidssoekerResponse(
        fnr: Fnr,
        periodeId: UUID,
        nyOpplysningerOmArbeidssoekerId: UUID? = null
    ) {
        val file = TestUtil.readFileAsJsonString("/opplysningerOmArbeidssoeker.json", javaClass)
        val opplysningerOmArbeidssoekerResponse: List<OpplysningerOmArbeidssoekerResponse> =
            objectMapper.readValue(
                file,
                object : TypeReference<List<OpplysningerOmArbeidssoekerResponse>>() {
                })
        `when`(oppslagArbeidssoekerregisteretClient.hentOpplysningerOmArbeidssoeker(fnr.get(), periodeId)).thenReturn(
            opplysningerOmArbeidssoekerResponse.map {
                it.copy(
                    periodeId = periodeId,
                    opplysningerOmArbeidssoekerId = nyOpplysningerOmArbeidssoekerId ?: it.opplysningerOmArbeidssoekerId
                )
            }
        )
    }

    @Throws(JsonProcessingException::class)
    private fun mockHentProfileringResponse(fnr: Fnr, periodeId: UUID, opplysningerOmArbeidssoekerId: UUID? = null) {
        val file = TestUtil.readFileAsJsonString("/profilering.json", javaClass)
        val profileringResponse: List<ProfileringResponse> = objectMapper.readValue(
            file,
            object : TypeReference<List<ProfileringResponse>>() {
            })
        `when`(oppslagArbeidssoekerregisteretClient.hentProfilering(fnr.get(), periodeId)).thenReturn(
            profileringResponse.map {
                it.copy(
                    periodeId = periodeId,
                    opplysningerOmArbeidssoekerId = opplysningerOmArbeidssoekerId ?: it.opplysningerOmArbeidssoekerId
                )
            }
        )
    }
}

fun genererRandomOpplysningerOmArbeidssoekerEntity(
    periodeId: UUID,
    opplysningerOmArbeidssoekerId: UUID
): OpplysningerOmArbeidssoekerEntity {
    return OpplysningerOmArbeidssoekerResponse(
        periodeId = periodeId,
        opplysningerOmArbeidssoekerId = opplysningerOmArbeidssoekerId,
        sendtInnAv = MetadataResponse(
            tidspunkt = ZonedDateTime.now(),
            utfoertAv = BrukerResponse(
                type = no.nav.pto.veilarbportefolje.arbeidssoeker.v2.BrukerType.SLUTTBRUKER
            ),
            kilde = "paw-arbeidssoekerregisteret-inngang",
            aarsak = "opplysning om arbeidssøker sendt inn"
        ),
        jobbsituasjon = listOf(
            BeskrivelseMedDetaljerResponse(
                beskrivelse = JobbSituasjonBeskrivelseResponse.ER_PERMITTERT,
                detaljer = mapOf(Pair("prosent", "25"))
            ),
            BeskrivelseMedDetaljerResponse(
                beskrivelse = JobbSituasjonBeskrivelseResponse.MIDLERTIDIG_JOBB,
                detaljer = mapOf(Pair("prosent", "75"))
            )
        ),
        annet = AnnetResponse(
            andreForholdHindrerArbeid = JaNeiVetIkke.NEI
        ),
        utdanning = UtdanningResponse(
            nus = "3",
            bestaatt = JaNeiVetIkke.JA,
            godkjent = JaNeiVetIkke.JA
        ),
        helse = HelseResponse(
            helsetilstandHindrerArbeid = JaNeiVetIkke.NEI
        )
    ).toOpplysningerOmArbeidssoekerEntity()
}

