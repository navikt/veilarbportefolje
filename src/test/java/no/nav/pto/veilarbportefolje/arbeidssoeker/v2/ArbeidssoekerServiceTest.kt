package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer
import no.nav.pto.veilarbportefolje.util.TestDataClient
import no.nav.pto.veilarbportefolje.util.TestDataUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.ZonedDateTime
import java.util.*

class ArbeidssoekerServiceTest {
    private val db = SingletonPostgresContainer.init().createJdbcTemplate()
    private val sisteArbeidssoekerPeriodeRepository = SisteArbeidssoekerPeriodeRepository(db)
    private val opplysningerOmArbeidssoekerRepository = OpplysningerOmArbeidssoekerRepository(db)
    private val profileringRepository = ProfileringRepository(db)
    private val oppslagArbeidssoekerregisteretClient = mock(OppslagArbeidssoekerregisteretClient::class.java)
    private val pdlIdentRepository = mock(PdlIdentRepository::class.java)

    private val arbeidssoekerService = ArbeidssoekerService(
        oppslagArbeidssoekerregisteretClient,
        pdlIdentRepository,
        opplysningerOmArbeidssoekerRepository,
        sisteArbeidssoekerPeriodeRepository,
        profileringRepository
    )

    @Test
    fun slettArbeidssoekerData_skal_slette_all_relatert_data_for_bruker() {
        // Arrange
        val aktorId = TestDataUtils.randomAktorId()
        val fnr1 = TestDataUtils.randomFnr()
        val fnr2 = TestDataUtils.randomFnr()
        val periodeId1 = UUID.randomUUID()
        val opplysningerOmArbeidssoekerId1 = UUID.randomUUID()
        val periodeId2 = UUID.randomUUID()
        val opplysningerOmArbeidssoekerId2 = UUID.randomUUID()
        `when`(pdlIdentRepository.hentFnrForAktivBruker(aktorId)).thenReturn(fnr1)

        sisteArbeidssoekerPeriodeRepository.insertSisteArbeidssoekerPeriode(ArbeidssoekerPeriodeEntity(periodeId1, fnr1.get()))
        sisteArbeidssoekerPeriodeRepository.insertSisteArbeidssoekerPeriode(ArbeidssoekerPeriodeEntity(periodeId2, fnr2.get()))
        opplysningerOmArbeidssoekerRepository.insertOpplysningerOmArbeidssoekerOgJobbsituasjon(
            genererRandomOpplysningerOmArbeidssoeker(periodeId1, opplysningerOmArbeidssoekerId1)
        )
        opplysningerOmArbeidssoekerRepository.insertOpplysningerOmArbeidssoekerOgJobbsituasjon(
            genererRandomOpplysningerOmArbeidssoeker(periodeId2, opplysningerOmArbeidssoekerId2)
        )

        // Act
        arbeidssoekerService.slettArbeidssoekerData(aktorId, Optional.of(fnr1))

        // Assert
        val sisteArbeidssoekerPeriode1 = TestDataClient.getArbeidssoekerPeriodeFraDb(db, periodeId1)
        assertThat(sisteArbeidssoekerPeriode1).isNull()
        val sisteArbeidssoekerPeriode2 = TestDataClient.getArbeidssoekerPeriodeFraDb(db, periodeId2)
        assertThat(sisteArbeidssoekerPeriode2).isNotNull()

        val opplysningerOmArbeidssoeker1 = TestDataClient.getOpplysningerOmArbeidssoekerFraDb(db, periodeId1)
        assertThat(opplysningerOmArbeidssoeker1).isNull()
        val opplysningerOmArbeidssoeker2 = TestDataClient.getOpplysningerOmArbeidssoekerFraDb(db, periodeId2)
        assertThat(opplysningerOmArbeidssoeker2).isNotNull()

        val opplysningerOmArbeidssoekerJobbsituasjon1 = TestDataClient.getOpplysningerOmArbeidssoekerJobbsituasjonFraDb(db, opplysningerOmArbeidssoekerId1)
        assertThat(opplysningerOmArbeidssoekerJobbsituasjon1).isNull()
        val opplysningerOmArbeidssoekerJobbsituasjon2 = TestDataClient.getOpplysningerOmArbeidssoekerJobbsituasjonFraDb(db, opplysningerOmArbeidssoekerId2)
        assertThat(opplysningerOmArbeidssoekerJobbsituasjon2).isNotNull()
    }
}

fun genererRandomOpplysningerOmArbeidssoeker(periodeId: UUID, opplysningerOmArbeidssoekerId: UUID): OpplysningerOmArbeidssoekerEntity {
    return OppslagArbeidssoekerregisteretClient.OpplysningerOmArbeidssoekerResponse(
        periodeId = periodeId,
        opplysningerOmArbeidssoekerId = opplysningerOmArbeidssoekerId,
        sendtInnAv = OppslagArbeidssoekerregisteretClient.MetadataResponse(
            tidspunkt = ZonedDateTime.now(),
            utfoertAv = OppslagArbeidssoekerregisteretClient.BrukerResponse(
                type = OppslagArbeidssoekerregisteretClient.BrukerType.SLUTTBRUKER
            ),
            kilde = "paw-arbeidssoekerregisteret-inngang",
            aarsak = "opplysning om arbeidssøker sendt inn"
        ),
        jobbsituasjon = listOf(
            OppslagArbeidssoekerregisteretClient.BeskrivelseMedDetaljerResponse(
                beskrivelse = OppslagArbeidssoekerregisteretClient.JobbSituasjonBeskrivelse.ER_PERMITTERT,
                detaljer = mapOf(Pair("prosent", "25"))
            ),
            OppslagArbeidssoekerregisteretClient.BeskrivelseMedDetaljerResponse(
                beskrivelse = OppslagArbeidssoekerregisteretClient.JobbSituasjonBeskrivelse.MIDLERTIDIG_JOBB,
                detaljer = mapOf(Pair("prosent", "75"))
            )
        ),
        annet = OppslagArbeidssoekerregisteretClient.AnnetResponse(
            andreForholdHindrerArbeid = OppslagArbeidssoekerregisteretClient.JaNeiVetIkke.NEI
        ),
        utdanning = OppslagArbeidssoekerregisteretClient.UtdanningResponse(
            nus = "3",
            bestaatt = OppslagArbeidssoekerregisteretClient.JaNeiVetIkke.JA,
            godkjent = OppslagArbeidssoekerregisteretClient.JaNeiVetIkke.JA
        ),
        helse = OppslagArbeidssoekerregisteretClient.HelseResponse(
            helsetilstandHindrerArbeid = OppslagArbeidssoekerregisteretClient.JaNeiVetIkke.NEI
        )
    ).toOpplysningerOmArbeidssoeker()
}