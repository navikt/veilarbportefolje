package no.nav.pto.veilarbportefolje.ungdomsprogram

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSER_UNGDOMSPROGRAM
import no.nav.pto.veilarbportefolje.ungdomsprogram.dto.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate

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


    val ungdomsprogramPeriodeDto = Periode(
        fraOgMed = LocalDate.of(2024, 1, 1),
        tilOgMed = LocalDate.of(2024, 12, 31),
        harForlengetPeriode = false,
        periodeMaksDato = LocalDate.of(2025, 12, 31)
    )

}
