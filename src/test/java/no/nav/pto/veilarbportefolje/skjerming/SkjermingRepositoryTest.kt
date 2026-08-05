package no.nav.pto.veilarbportefolje.skjerming

import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.database.PostgresTable
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.sql.Timestamp
import java.util.*
import java.util.stream.Stream

class SkjermingRepositoryTest {
    private lateinit var skjermingRepository: SkjermingRepository

    @BeforeEach
    fun setup() {
        val db = SingletonPostgresContainer.init().createJdbcTemplate()
        db.execute("TRUNCATE " + PostgresTable.NOM_SKJERMING.TABLE_NAME)
        skjermingRepository = SkjermingRepository(db)
    }

    @Test
    fun testSavingSkjermingStatus() {
        val fnr = Fnr.of("fnr123")

        skjermingRepository.settSkjerming(fnr, true)
        val forventetSkjermet: Set<Fnr> = skjermingRepository.hentSkjermetPersoner(listOf(fnr))
        Assertions.assertTrue(forventetSkjermet.contains(fnr))

        skjermingRepository.settSkjerming(fnr, false)
        val forventetIkkeSkjermet = skjermingRepository.hentSkjermetPersoner(listOf(fnr))
        Assertions.assertFalse(forventetIkkeSkjermet.contains(fnr))
    }

    @ParameterizedTest
    @MethodSource("skjermingPeriodeTestdata")
    fun testSavingSkjermingPeriode(skjermingdataInput: SkjermingData, forventetSkjermingdataOutput: SkjermingData) {
        skjermingRepository.settSkjermingPeriode(
            skjermingdataInput.fnr,
            skjermingdataInput.skjermetFra,
            skjermingdataInput.skjermetTil
        )
        val actualSkjermingdataOutput: Optional<SkjermingData> =
            skjermingRepository.hentSkjermingData(skjermingdataInput.fnr)

        Assertions.assertTrue(actualSkjermingdataOutput.isPresent)
        Assertions.assertEquals(actualSkjermingdataOutput.get().skjermetFra, forventetSkjermingdataOutput.skjermetFra)
        Assertions.assertEquals(actualSkjermingdataOutput.get().skjermetTil, forventetSkjermingdataOutput.skjermetTil)
    }

    @Test
    fun testDeleteOfSkjermingData() {
        val fnr = Fnr.of("fnr123")
        skjermingRepository.settSkjerming(fnr, true)

        skjermingRepository.deleteSkjermingData(fnr)

        val skjermingDataOptional = skjermingRepository.hentSkjermetPersoner(listOf(fnr))
        Assertions.assertFalse(skjermingDataOptional.contains(fnr))
    }

    @Test
    fun testHentingAvSkjermingData() {
        val fnr1 = Fnr.of("fnr123")
        val fnr2 = Fnr.of("fnr124")
        val fnr3 = Fnr.of("fnr125")
        skjermingRepository.settSkjerming(fnr1, true)
        skjermingRepository.settSkjerming(fnr2, true)
        skjermingRepository.settSkjerming(fnr3, false)

        val fnrSkjermingOptional: Set<Fnr> =
            skjermingRepository.hentSkjermetPersoner(listOf(fnr1, fnr2, fnr3))
        Assertions.assertTrue(fnrSkjermingOptional.contains(fnr1))
        Assertions.assertTrue(fnrSkjermingOptional.contains(fnr2))
        Assertions.assertFalse(fnrSkjermingOptional.contains(fnr3))
    }

    companion object {
        @JvmStatic
        fun skjermingPeriodeTestdata(): Stream<Arguments> {
            val testdata = mapOf(
                SkjermingData(
                    fnr = Fnr.of("fnr123"),
                    erSkjermet = true,
                    skjermetFra = Timestamp.valueOf("2022-02-21 13:14:00"),
                    skjermetTil = null
                ) to
                        SkjermingData(
                            fnr = Fnr.of("fnr123"),
                            erSkjermet = true,
                            skjermetFra = Timestamp.valueOf("2022-02-21 13:14:00"),
                            skjermetTil = null
                        ),

                SkjermingData(
                    fnr = Fnr.of("fnr123"),
                    erSkjermet = true,
                    skjermetFra = Timestamp.valueOf("2022-02-21 13:14:00"),
                    skjermetTil = Timestamp.valueOf("2022-04-21 13:14:00")
                ) to
                        SkjermingData(
                            fnr = Fnr.of("fnr123"),
                            erSkjermet = true,
                            skjermetFra = Timestamp.valueOf("2022-02-21 13:14:00"),
                            skjermetTil = Timestamp.valueOf("2022-04-21 13:14:00"),
                        )
            )

            return testdata.entries.stream().map { Arguments.of(it.key, it.value) }
        }
    }
}
