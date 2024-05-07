package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.database.PostgresTable.*
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.util.UUID

class ArbeidssoekerDataRepositoryTest {
    private val db: JdbcTemplate = SingletonPostgresContainer.init().createJdbcTemplate()
    private val arbeidssoekerDataRepository: ArbeidssoekerDataRepository = ArbeidssoekerDataRepository(db)
    private val opplysningerOmArbeidssoekerRepository: OpplysningerOmArbeidssoekerRepository = OpplysningerOmArbeidssoekerRepository(db)

    private val fnr1 = Fnr.of("12345671231")
    private val fnr2 = Fnr.of("12345671232")
    private val fnr3 = Fnr.of("12345671233")

    private val periodeId1 = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16")
    private val periodeId2 = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d17")
    private val periodeId3 = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d18")

    private val opplysningerId1 = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d19")
    private val opplysningerId2 = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d20")
    private val opplysningerId3 = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d21")

    @BeforeEach
    fun reset() {
        db.update("""TRUNCATE ${SISTE_ARBEIDSSOEKER_PERIODE.TABLE_NAME} CASCADE""")
    }

    @Test
    fun hentAlleOpplysningerOmArbeidssoekerListe() {
        // Arrange
        setUpPeriodeTestData()
        setUpOpplysningerOmArbeidssoekerTestData()

        // Act
        val arbeidssoekerDataForBrukere =
            arbeidssoekerDataRepository.hentOpplysningerOmArbeidssoeker(listOf(fnr1, fnr2, fnr3))

        // Assert
        assertEquals(3, arbeidssoekerDataForBrukere.size)
        assertEquals(2, arbeidssoekerDataForBrukere[fnr1.get()]?.jobbsituasjoner?.size)
        assertEquals(1, arbeidssoekerDataForBrukere[fnr2.get()]?.jobbsituasjoner?.size)

    }

    @Test
    fun hentProfileringPaaAlleBrukerne() {
        // Arrange
        setUpPeriodeTestData()
        setUpProfileringTestData()

        //Act
        val profileringPaaBruker = arbeidssoekerDataRepository.hentProfileringsresultatListe(listOf(fnr1, fnr2, fnr3))

        //Assert
        assertEquals(3, profileringPaaBruker.size)
        assertEquals(Profileringsresultat.ANTATT_BEHOV_FOR_VEILEDNING, profileringPaaBruker[fnr1.get()]?.profileringsresultat)
        assertEquals(Profileringsresultat.ANTATT_GODE_MULIGHETER, profileringPaaBruker[fnr2.get()]?.profileringsresultat)
        assertEquals(Profileringsresultat.OPPGITT_HINDRINGER, profileringPaaBruker[fnr3.get()]?.profileringsresultat)
    }

    private fun setUpPeriodeTestData() {
        db.update(
            """INSERT INTO ${SISTE_ARBEIDSSOEKER_PERIODE.TABLE_NAME} VALUES (?,?)""",
            periodeId1,
            fnr1.get()
        )
        db.update(
            """INSERT INTO ${SISTE_ARBEIDSSOEKER_PERIODE.TABLE_NAME} VALUES (?,?)""",
            periodeId2,
            fnr2.get()
        )
        db.update(
            """INSERT INTO ${SISTE_ARBEIDSSOEKER_PERIODE.TABLE_NAME} VALUES (?,?)""",
            periodeId3,
            fnr3.get()
        )
    }

    private fun setUpOpplysningerOmArbeidssoekerTestData() {
        val opplysninger1 = OpplysningerOmArbeidssoekerEntity(
            opplysningerOmArbeidssoekerId = opplysningerId1,
            periodeId = periodeId1,
            sendtInnTidspunkt = Timestamp.valueOf("2024-04-23 13:22:58.089"),
            utdanningNusKode = "3",
            utdanningBestatt = "JA",
            utdanningGodkjent = "JA",
            opplysningerOmJobbsituasjon = OpplysningerOmArbeidssoekerJobbsituasjonEntity(
                opplysningerOmArbeidssoekerId = opplysningerId1,
                jobbsituasjon = listOf("ER_PERMITTERT", "MIDLERTIDIG_JOBB")
            )
        )

        val opplysninger2 = OpplysningerOmArbeidssoekerEntity(
            opplysningerOmArbeidssoekerId = opplysningerId2,
            periodeId = periodeId2,
            sendtInnTidspunkt = Timestamp.valueOf("2024-04-23 23:22:58.089"),
            utdanningNusKode = "3",
            utdanningBestatt = "JA",
            utdanningGodkjent = "JA",
            opplysningerOmJobbsituasjon = OpplysningerOmArbeidssoekerJobbsituasjonEntity(
                opplysningerOmArbeidssoekerId = opplysningerId2,
                jobbsituasjon = listOf("HAR_SAGT_OPP")
            )
        )

        val opplysninger3 = OpplysningerOmArbeidssoekerEntity(
            opplysningerOmArbeidssoekerId = opplysningerId3,
            periodeId = periodeId3,
            sendtInnTidspunkt = Timestamp.valueOf("2024-04-23 23:22:58.089"),
            utdanningNusKode = "3",
            utdanningBestatt = "JA",
            utdanningGodkjent = "JA",
            opplysningerOmJobbsituasjon = OpplysningerOmArbeidssoekerJobbsituasjonEntity(
                opplysningerOmArbeidssoekerId = opplysningerId3,
                jobbsituasjon = listOf("ALDRI_HATT_JOBB", "NY_JOBB")
            )
        )

        opplysningerOmArbeidssoekerRepository.insertOpplysningerOmArbeidssoekerOgJobbsituasjon(opplysninger1)
        opplysningerOmArbeidssoekerRepository.insertOpplysningerOmArbeidssoekerOgJobbsituasjon(opplysninger2)
        opplysningerOmArbeidssoekerRepository.insertOpplysningerOmArbeidssoekerOgJobbsituasjon(opplysninger3)
    }

    private fun setUpProfileringTestData() {
        db.update(
            """INSERT INTO ${PROFILERING.TABLE_NAME} (
                    ${PROFILERING.PERIODE_ID}, 
                    ${PROFILERING.PROFILERING_RESULTAT}, 
                    ${PROFILERING.SENDT_INN_TIDSPUNKT}
                ) VALUES (?,?,?)""",
            periodeId1,
            Profileringsresultat.ANTATT_BEHOV_FOR_VEILEDNING.name,
            Timestamp.valueOf("2024-04-23 23:22:58.089")
        )

        db.update(
            """INSERT INTO ${PROFILERING.TABLE_NAME} (
                    ${PROFILERING.PERIODE_ID}, 
                    ${PROFILERING.PROFILERING_RESULTAT}, 
                    ${PROFILERING.SENDT_INN_TIDSPUNKT}
                ) VALUES (?,?,?)""",
            periodeId2,
            Profileringsresultat.ANTATT_GODE_MULIGHETER.name,
            Timestamp.valueOf("2024-04-23 23:22:58.089")
        )

        db.update(
            """INSERT INTO ${PROFILERING.TABLE_NAME} (
                    ${PROFILERING.PERIODE_ID}, 
                    ${PROFILERING.PROFILERING_RESULTAT}, 
                    ${PROFILERING.SENDT_INN_TIDSPUNKT}
                ) VALUES (?,?,?)""",
            periodeId3,
            Profileringsresultat.OPPGITT_HINDRINGER.name,
            Timestamp.valueOf("2024-04-23 23:22:58.089")
        )
    }
}