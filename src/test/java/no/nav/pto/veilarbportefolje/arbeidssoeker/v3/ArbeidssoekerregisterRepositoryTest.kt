package no.nav.pto.veilarbportefolje.arbeidssoeker.v3

import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.arbeidssoeker.v3.domene.AggregertPeriodeEntity
import no.nav.pto.veilarbportefolje.arbeidssoeker.v3.domene.OpplysningerOmArbeidssoekerEntity
import no.nav.pto.veilarbportefolje.arbeidssoeker.v3.domene.ProfileringEntity
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.database.PostgresTable.ARBEIDSSOEKERREGISTER_DATA
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@SpringBootTest(classes = [ApplicationConfigTest::class])
class ArbeidssoekerregisterRepositoryTest(
    @param:Autowired val jdbcTemplate: JdbcTemplate,
    @param:Autowired val repository: ArbeidssoekerregisterRepository
) {
    private val fnr = Fnr.of("23916795505")

    private val aggregertPeriode = AggregertPeriodeEntity(
        identitetsnummer = fnr.get(),
        periodeId = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16"),
        periodeStartTidspunkt = ZonedDateTime.parse("2024-04-23T13:04:40.739Z"),
        periodeAvsluttetTidspunkt = null,
        opplysningId = UUID.fromString("913161a3-dde9-4448-abf8-2a01a043f8cd"),
        opplysningJobbsituasjonBeskrivelse = listOf(
            JobbSituasjonBeskrivelseResponse.ALDRI_HATT_JOBB.name,
            JobbSituasjonBeskrivelseResponse.ER_PERMITTERT.name
        ),
        opplysningUtdanningNusKode = 3,
        opplysningUtdanningBestatt = JaNeiVetIkke.JA.name,
        opplysningUtdanningGodkjent = JaNeiVetIkke.VET_IKKE.name,
        opplysningSendtInnTidspunkt = ZonedDateTime.parse("2024-04-23T13:22:58.089Z"),
        profileringId = UUID.fromString("7c9a2feb-0b31-4101-825a-0c4a3d465e01"),
        profileringResultat = ProfilertTil.OPPGITT_HINDRINGER.name,
        profileringSendtInnTidspunkt = ZonedDateTime.parse("2024-04-25T08:19:53.612Z")
    )

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE ${ARBEIDSSOEKERREGISTER_DATA.TABLE_NAME}")
    }

    @Test
    fun `lagreSisteArbeidssoekerPeriode med ny bruker skal inserte ny rad`() {
        repository.lagreSisteArbeidssoekerPeriode(fnr, aggregertPeriode)

        val resultat = repository.hentArbeidssoekerregisterData(fnr)

        assertThat(resultat).isNotNull
        assertThat(resultat!!.identitetsnummer).isEqualTo(fnr.get())
        assertThat(resultat.aggregertPeriode).isEqualTo(aggregertPeriode)
        assertThat(resultat.synkroniseringStatus).isEqualTo(SynkroniseringStatus.SYNKRONISERT)
        assertThat(resultat.radOpprettet).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
    }

    @Test
    fun `lagreSisteArbeidssoekerPeriode med eksisterende bruker skal overskrive data og beholde radOpprettet`() {
        repository.lagreSisteArbeidssoekerPeriode(fnr, aggregertPeriode)
        val radOpprettet = repository.hentArbeidssoekerregisterData(fnr)!!.radOpprettet

        val oppdatertPeriode = aggregertPeriode.copy(
            periodeId = UUID.randomUUID(),
            periodeAvsluttetTidspunkt = ZonedDateTime.parse("2024-06-01T10:00:00Z"),
            opplysningId = null,
            opplysningJobbsituasjonBeskrivelse = null,
            opplysningUtdanningNusKode = null,
            opplysningUtdanningBestatt = null,
            opplysningUtdanningGodkjent = null,
            opplysningSendtInnTidspunkt = null,
            profileringId = null,
            profileringResultat = null,
            profileringSendtInnTidspunkt = null
        )
        repository.lagreSisteArbeidssoekerPeriode(fnr, oppdatertPeriode)

        val resultat = repository.hentArbeidssoekerregisterData(fnr)

        assertThat(resultat).isNotNull
        assertThat(resultat!!.aggregertPeriode).isEqualTo(oppdatertPeriode)
        assertThat(resultat.synkroniseringStatus).isEqualTo(SynkroniseringStatus.SYNKRONISERT)
        assertThat(resultat.radOpprettet).isEqualTo(radOpprettet)
    }

    @Test
    fun `lagreBrukerMedSynkroniseringStatusUsikker med ny bruker skal inserte rad uten arbeidssoekerdata`() {
        repository.lagreBrukerMedSynkroniseringStatusUsikker(fnr)

        val resultat = repository.hentArbeidssoekerregisterData(fnr)

        assertThat(resultat).isNotNull
        assertThat(resultat!!.aggregertPeriode).isNull()
        assertThat(resultat.synkroniseringStatus).isEqualTo(SynkroniseringStatus.USIKKERT_OM_BRUKER_HAR_DATA)
        assertThat(resultat.radOpprettet).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
    }

    @Test
    fun `lagreBrukerMedSynkroniseringStatusUsikker med eksisterende bruker skal kun oppdatere status`() {
        repository.lagreSisteArbeidssoekerPeriode(fnr, aggregertPeriode)

        repository.lagreBrukerMedSynkroniseringStatusUsikker(fnr)

        val resultat = repository.hentArbeidssoekerregisterData(fnr)

        assertThat(resultat).isNotNull
        assertThat(resultat!!.aggregertPeriode).isEqualTo(aggregertPeriode)
        assertThat(resultat.synkroniseringStatus).isEqualTo(SynkroniseringStatus.USIKKERT_OM_BRUKER_HAR_DATA)
    }

    @Test
    fun `hentArbeidssoekerregisterData med ikke-eksisterende bruker skal returnere null`() {
        assertThat(repository.hentArbeidssoekerregisterData(fnr)).isNull()
    }

    @Test
    fun `hentArbeidssoekerregisterDataForPeriodeid skal returnere periode med matchende periodeId`() {
        repository.lagreSisteArbeidssoekerPeriode(fnr, aggregertPeriode)
        repository.lagreSisteArbeidssoekerPeriode(
            Fnr.of("01010112345"),
            aggregertPeriode.copy(identitetsnummer = "01010112345", periodeId = UUID.randomUUID())
        )

        val resultat = repository.hentArbeidssoekerregisterDataForPeriodeid(aggregertPeriode.periodeId)

        assertThat(resultat).isEqualTo(aggregertPeriode)
    }

    @Test
    fun `lagreOpplysningerForPeriode skal overskrive opplysninger og beholde periode og profilering`() {
        repository.lagreSisteArbeidssoekerPeriode(fnr, aggregertPeriode)
        val nyeOpplysninger = OpplysningerOmArbeidssoekerEntity(
            opplysningId = UUID.randomUUID(),
            opplysningJobbsituasjonBeskrivelse = listOf(JobbSituasjonBeskrivelseResponse.HAR_SAGT_OPP.name),
            opplysningUtdanningNusKode = 6,
            opplysningUtdanningBestatt = JaNeiVetIkke.NEI.name,
            opplysningUtdanningGodkjent = JaNeiVetIkke.JA.name,
            opplysningSendtInnTidspunkt = Instant.parse("2024-05-01T10:00:00.123456Z")
        )

        repository.lagreOpplysningerForPeriode(aggregertPeriode.periodeId, nyeOpplysninger)

        val resultat = repository.hentArbeidssoekerregisterData(fnr)
        assertThat(resultat!!.aggregertPeriode).isEqualTo(
            aggregertPeriode.copy(
                opplysningId = nyeOpplysninger.opplysningId,
                opplysningJobbsituasjonBeskrivelse = nyeOpplysninger.opplysningJobbsituasjonBeskrivelse,
                opplysningUtdanningNusKode = nyeOpplysninger.opplysningUtdanningNusKode,
                opplysningUtdanningBestatt = nyeOpplysninger.opplysningUtdanningBestatt,
                opplysningUtdanningGodkjent = nyeOpplysninger.opplysningUtdanningGodkjent,
                opplysningSendtInnTidspunkt = ZonedDateTime.parse("2024-05-01T10:00:00.123456Z")
            )
        )
        assertThat(resultat.synkroniseringStatus).isEqualTo(SynkroniseringStatus.SYNKRONISERT)
    }

    @Test
    fun `lagreProfileringForPeriode skal overskrive profilering og beholde periode og opplysninger`() {
        repository.lagreSisteArbeidssoekerPeriode(fnr, aggregertPeriode)
        val nyProfilering = ProfileringEntity(
            profileringId = UUID.randomUUID(),
            profileringResultat = ProfilertTil.ANTATT_GODE_MULIGHETER.name,
            profileringSendtInnTidspunkt = Instant.parse("2024-05-01T10:00:00.123456Z")
        )

        repository.lagreProfileringForPeriode(aggregertPeriode.periodeId, nyProfilering)

        val resultat = repository.hentArbeidssoekerregisterData(fnr)
        assertThat(resultat!!.aggregertPeriode).isEqualTo(
            aggregertPeriode.copy(
                profileringId = nyProfilering.profileringId,
                profileringResultat = nyProfilering.profileringResultat,
                profileringSendtInnTidspunkt = ZonedDateTime.parse("2024-05-01T10:00:00.123456Z")
            )
        )
        assertThat(resultat.synkroniseringStatus).isEqualTo(SynkroniseringStatus.SYNKRONISERT)
    }
}
