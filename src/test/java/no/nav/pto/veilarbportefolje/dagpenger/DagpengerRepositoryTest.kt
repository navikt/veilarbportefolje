package no.nav.pto.veilarbportefolje.dagpenger

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.dagpenger.domene.DagpengerRettighetstype
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerBeregningerResponseDto
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerPeriodeDto
import no.nav.pto.veilarbportefolje.database.PostgresTable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate


@SpringBootTest(classes = [ApplicationConfigTest::class])
class DagpengerRepositoryTest(
    @param:Autowired val dagpengerRepository: DagpengerRepository,
    @param:Autowired val jdbcTemplate: JdbcTemplate
) {

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE TABLE ${PostgresTable.YTELSER_DAGPENGER.TABLE_NAME}")
    }

    @Test
    fun `upsert med ny bruker skal inserte ny rad`() {
        val ident = "123456789"
        val periode = dagpengerPeriodeDto
        val beregning = dagpengerBeregningDto

        dagpengerRepository.upsertDagpengerPerioder(ident, periode, beregning)

        val resultatAvHenting = dagpengerRepository.hentDagpenger(ident)
        assertThat(resultatAvHenting).isNotNull
        assertThat(resultatAvHenting!!.rettighetstype).isEqualTo(DagpengerRettighetstype.DAGPENGER_ARBEIDSSOKER_ORDINAER)
        assertThat(resultatAvHenting.fom).isEqualTo(LocalDate.of(2025, 1, 1))
        assertThat(resultatAvHenting.tom).isEqualTo(LocalDate.of(2026, 12, 31))
        assertThat(resultatAvHenting.antallDagerResterende).isEqualTo(100)
        assertThat(resultatAvHenting.datoAntallDagerBleBeregnet).isEqualTo(LocalDate.of(2025, 1, 1))

    }

    @Test
    fun `upsert med eksisterende bruker skal oppdatere ny rad`() {
        val ident = "123456789"

        val periode_nr1 = dagpengerPeriodeDto
        val periode_nr2 = periode_nr1.copy(
            fraOgMedDato = LocalDate.of(2025, 1, 1),
            tilOgMedDato = LocalDate.of(2027, 6, 1),
        )

        dagpengerRepository.upsertDagpengerPerioder(ident, periode_nr1, null)
        dagpengerRepository.upsertDagpengerPerioder(ident, periode_nr2, dagpengerBeregningDto)

        val resultatAvHenting = dagpengerRepository.hentDagpenger(ident)
        assertThat(resultatAvHenting).isNotNull
        assertThat(resultatAvHenting!!.rettighetstype).isEqualTo(DagpengerRettighetstype.DAGPENGER_ARBEIDSSOKER_ORDINAER)
        assertThat(resultatAvHenting.fom).isEqualTo(LocalDate.of(2025, 1, 1))
        assertThat(resultatAvHenting.tom).isEqualTo(LocalDate.of(2027, 6, 1))
        assertThat(resultatAvHenting.antallDagerResterende).isEqualTo(100)
        assertThat(resultatAvHenting.datoAntallDagerBleBeregnet).isEqualTo(LocalDate.of(2025, 1, 1))

    }

    @Test
    fun `hentDagpenger med ikke-eksisterende bruker skal returnere null`() {
        val ident = "123456789"
        val resultatAvHenting = dagpengerRepository.hentDagpenger(ident)
        assertThat(resultatAvHenting).isNull()
    }

    @Test
    fun `slettDagpengerForBruker skal slette rad`() {
        val ident = "123456789"
        val periode = dagpengerPeriodeDto

        dagpengerRepository.upsertDagpengerPerioder(ident, periode, null)

        val resultatAvHenting = dagpengerRepository.hentDagpenger(ident)
        assertThat(resultatAvHenting).isNotNull

        dagpengerRepository.slettDagpengerForBruker(ident)
        val resultatEtterSletting = dagpengerRepository.hentDagpenger(ident)
        assertThat(resultatEtterSletting).isNull()
    }


    val dagpengerPeriodeDto = DagpengerPeriodeDto(
        fraOgMedDato = LocalDate.of(2025, 1, 1),
        tilOgMedDato = LocalDate.of(2026, 12, 31),
        ytelseType = DagpengerRettighetstype.DAGPENGER_ARBEIDSSOKER_ORDINAER,
        kilde = "DP_SAK",
    )

    val dagpengerBeregningDto = DagpengerBeregningerResponseDto(
        fraOgMed = LocalDate.of(2025, 1, 1),
        gjenståendeDager = 100,
        sats = 600,
        utbetaltBeløp = 400
    )


}
