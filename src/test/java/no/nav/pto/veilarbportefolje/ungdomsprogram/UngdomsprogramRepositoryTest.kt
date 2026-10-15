package no.nav.pto.veilarbportefolje.ungdomsprogram

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSER_UNGDOMSPROGRAM
import no.nav.pto.veilarbportefolje.ungdomsprogram.domene.UngdomsprogramPeriodeEntity
import no.nav.pto.veilarbportefolje.ungdomsprogram.dto.Periode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@SpringBootTest(classes = [ApplicationConfigTest::class])
class UngdomsprogramRepositoryTest(
    @param:Autowired val ungdomsprogramRepository: UngdomsprogramRepository,
    @param:Autowired val jdbcTemplate: JdbcTemplate
) {

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE TABLE ${YTELSER_UNGDOMSPROGRAM.TABLE_NAME}")
    }

    @Test
    fun `upsert med ny bruker skal inserte ny rad`() {
        val ident = "123456789"
        val periode = ungdomsprogramPeriodeDto

        ungdomsprogramRepository.upsertUngdomsprogram(ident, periode)

        val resultatAvHenting = ungdomsprogramRepository.hentUngdomsprogram(ident)
        assertThat(resultatAvHenting).isNotNull
        assertThat(resultatAvHenting!!.fraOgMed).isEqualTo(LocalDate.of(2024, 1, 1))
        assertThat(resultatAvHenting.tilOgMed).isEqualTo(LocalDate.of(2024, 12, 31))
        assertThat(resultatAvHenting.harForlengetPeriode).isEqualTo(false)
    }

    @Test
    fun `upsert med eksisterende bruker skal oppdatere ny rad`() {
        val ident = "123456789"
        val periode = ungdomsprogramPeriodeDto
        val periode_nr2 = periode.copy(
            fraOgMed = LocalDate.of(2025, 12, 31),
            tilOgMed = LocalDate.of(2026, 12, 31),
        )

        ungdomsprogramRepository.upsertUngdomsprogram(ident, periode)
        ungdomsprogramRepository.upsertUngdomsprogram(ident, periode_nr2)

        val resultatAvHenting = ungdomsprogramRepository.hentUngdomsprogram(ident)
        assertThat(resultatAvHenting).isNotNull
        assertThat(resultatAvHenting!!.fraOgMed).isEqualTo(LocalDate.of(2025, 12, 31))
        assertThat(resultatAvHenting.tilOgMed).isEqualTo(LocalDate.of(2026, 12, 31))
    }


    @Test
    fun `hentUngdomsprogram med ikke-eksisterende bruker skal returnere null`() {
        val ident = "123456789"
        val resultatAvHenting = ungdomsprogramRepository.hentUngdomsprogram(ident)
        assertThat(resultatAvHenting).isNull()
    }

    @Test
    fun `slettUngdomsprogramForBruker skal slette rad`() {
        val ident = "123456789"
        val periode = ungdomsprogramPeriodeDto

        ungdomsprogramRepository.upsertUngdomsprogram(ident, periode)
        val resultatAvHenting = ungdomsprogramRepository.hentUngdomsprogram(ident)
        assertThat(resultatAvHenting).isNotNull
        ungdomsprogramRepository.slettUngdomsprogramForBruker(ident)
        val resultatEtterSletting = ungdomsprogramRepository.hentUngdomsprogram(ident)
        assertThat(resultatEtterSletting).isNull()
    }

    @Test
    fun `hentUngdomsprogramPerioder skal returnere perioder kun for brukere med ungdomsprogram`() {
        val identMedUngdomsprogram1 = "11111111111"
        val identMedUngdomsprogram2 = "22222222222"
        val identUtenUngdomsprogram = "33333333333"

        ungdomsprogramRepository.upsertUngdomsprogram(identMedUngdomsprogram1, ungdomsprogramPeriodeDto)
        ungdomsprogramRepository.upsertUngdomsprogram(
            identMedUngdomsprogram2,
            ungdomsprogramPeriodeDto.copy(tilOgMed = null, harForlengetPeriode = true)
        )

        val resultat = ungdomsprogramRepository.hentUngdomsprogramPerioder(
            listOf(identMedUngdomsprogram1, identMedUngdomsprogram2, identUtenUngdomsprogram)
        )

        assertThat(resultat).containsOnlyKeys(identMedUngdomsprogram1, identMedUngdomsprogram2)
        assertThat(resultat[identMedUngdomsprogram1]!!.radSistEndret).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(resultat[identMedUngdomsprogram2]!!.radSistEndret).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(resultat[identMedUngdomsprogram1]).usingRecursiveComparison().ignoringFields("radSistEndret").isEqualTo(
            UngdomsprogramPeriodeEntity(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                false,
                LocalDate.of(2025, 12, 31),
                LocalDateTime.MIN
            )
        )
        assertThat(resultat[identMedUngdomsprogram2]).usingRecursiveComparison().ignoringFields("radSistEndret").isEqualTo(
            UngdomsprogramPeriodeEntity(
                LocalDate.of(2024, 1, 1),
                null,
                true,
                LocalDate.of(2025, 12, 31),
                LocalDateTime.MIN
            )
        )
    }


    val ungdomsprogramPeriodeDto = Periode(
        fraOgMed = LocalDate.of(2024, 1, 1),
        tilOgMed = LocalDate.of(2024, 12, 31),
        harForlengetPeriode = false,
        periodeMaksDato = LocalDate.of(2025, 12, 31)
    )

}
