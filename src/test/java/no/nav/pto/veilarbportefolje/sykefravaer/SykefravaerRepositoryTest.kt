package no.nav.pto.veilarbportefolje.sykefravaer

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.database.PostgresTable
import no.nav.pto.veilarbportefolje.sykefravaer.domene.SykefravaerEntity
import no.nav.pto.veilarbportefolje.sykefravaer.dto.OppfolgingstilfelleDTO
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
class SykefravaerRepositoryTest(
    @param:Autowired val sykefravaerRepository: SykefravaerRepository,
    @param:Autowired val jdbcTemplate: JdbcTemplate
) {

    private val ident = "12345678910"
    private val sykefravaersperiode = OppfolgingstilfelleDTO(
        arbeidstakerAtTilfelleEnd = true,
        start = LocalDate.of(2026, 9, 1),
        end = LocalDate.of(2026, 11, 30),
    )

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE TABLE ${PostgresTable.SYKEFRAVAER_OPPFOLGING.TABLE_NAME}")
    }

    @Test
    fun `upsert med ny bruker skal inserte ny rad`() {
        sykefravaerRepository.upsertSykefravaerOppfolgingsperiode(ident, sykefravaersperiode)

        val resultat = sykefravaerRepository.hentSykefravaerPeriode(ident)

        assertThat(resultat).isNotNull
        assertThat(resultat!!.start).isEqualTo(LocalDate.of(2026, 9, 1))
        assertThat(resultat.slutt).isEqualTo(LocalDate.of(2026, 11, 30))
        assertThat(resultat.harArbeidsgiver).isTrue()
        assertThat(resultat.radSistEndret).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES))
    }

    @Test
    fun `upsert med eksisterende bruker skal oppdatere raden`() {
        val oppdatertTilfelle = sykefravaersperiode.copy(
            arbeidstakerAtTilfelleEnd = false,
            start = LocalDate.of(2026, 10, 1),
            end = LocalDate.of(2026, 12, 31),
        )

        sykefravaerRepository.upsertSykefravaerOppfolgingsperiode(ident, sykefravaersperiode)
        sykefravaerRepository.upsertSykefravaerOppfolgingsperiode(ident, oppdatertTilfelle)

        val resultat = sykefravaerRepository.hentSykefravaerPeriode(ident)

        assertThat(resultat).isNotNull
        assertThat(resultat!!.start).isEqualTo(LocalDate.of(2026, 10, 1))
        assertThat(resultat.slutt).isEqualTo(LocalDate.of(2026, 12, 31))
        assertThat(resultat.harArbeidsgiver).isFalse()
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ${PostgresTable.SYKEFRAVAER_OPPFOLGING.TABLE_NAME}",
                Int::class.java
            )
        ).isEqualTo(1)
    }

    @Test
    fun `hentSykefravaerPeriode med ikke-eksisterende bruker skal returnere null`() {
        assertThat(sykefravaerRepository.hentSykefravaerPeriode(ident)).isNull()
    }

    @Test
    fun `slettSykefravaerForBruker skal slette rad`() {
        sykefravaerRepository.upsertSykefravaerOppfolgingsperiode(ident, sykefravaersperiode)
        assertThat(sykefravaerRepository.hentSykefravaerPeriode(ident)).isNotNull

        sykefravaerRepository.slettSykefravaerForBruker(ident)

        assertThat(sykefravaerRepository.hentSykefravaerPeriode(ident)).isNull()
    }

    @Test
    fun `hentSykefravaerPerioder skal returnere perioder kun for brukere med sykefravaer`() {
        val identMedSykefravaer1 = "11111111111"
        val identMedSykefravaer2 = "22222222222"
        val identUtenSykefravaer = "33333333333"

        sykefravaerRepository.upsertSykefravaerOppfolgingsperiode(identMedSykefravaer1, sykefravaersperiode)
        sykefravaerRepository.upsertSykefravaerOppfolgingsperiode(
            identMedSykefravaer2,
            sykefravaersperiode.copy(arbeidstakerAtTilfelleEnd = false, end = LocalDate.of(2027, 1, 15))
        )

        val resultat = sykefravaerRepository.hentSykefravaerPerioder(
            listOf(identMedSykefravaer1, identMedSykefravaer2, identUtenSykefravaer)
        )

        assertThat(resultat).containsOnlyKeys(identMedSykefravaer1, identMedSykefravaer2)
        assertThat(resultat[identMedSykefravaer1]).usingRecursiveComparison().ignoringFields("radSistEndret").isEqualTo(
            SykefravaerEntity(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 30), true, LocalDateTime.MIN)
        )
        assertThat(resultat[identMedSykefravaer2]).usingRecursiveComparison().ignoringFields("radSistEndret").isEqualTo(
            SykefravaerEntity(LocalDate.of(2026, 9, 1), LocalDate.of(2027, 1, 15), false, LocalDateTime.MIN)
        )
    }

    @Test
    fun `hentSykefravaerPerioder med tom liste skal returnere tomt map`() {
        assertThat(sykefravaerRepository.hentSykefravaerPerioder(emptyList())).isEmpty()
    }
}
