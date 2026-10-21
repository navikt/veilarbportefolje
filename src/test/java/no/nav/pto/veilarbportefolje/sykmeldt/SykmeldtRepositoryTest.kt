package no.nav.pto.veilarbportefolje.sykmeldt

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.database.PostgresTable
import no.nav.pto.veilarbportefolje.sykmeldt.domene.SykmeldtEntity
import no.nav.pto.veilarbportefolje.sykmeldt.dto.OppfolgingstilfelleDTO
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
class SykmeldtRepositoryTest(
    @param:Autowired val sykmeldtRepository: SykmeldtRepository,
    @param:Autowired val jdbcTemplate: JdbcTemplate
) {

    private val ident = "12345678910"
    private val sykmeldtperiode = OppfolgingstilfelleDTO(
        arbeidstakerAtTilfelleEnd = true,
        start = LocalDate.of(2026, 9, 1),
        end = LocalDate.of(2026, 11, 30),
    )

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE TABLE ${PostgresTable.SYKMELDT.TABLE_NAME}")
    }

    @Test
    fun `upsert med ny bruker skal inserte ny rad`() {
        sykmeldtRepository.upsertSykmeldtperiode(ident, sykmeldtperiode)

        val resultat = sykmeldtRepository.hentSykmeldtPeriode(ident)

        assertThat(resultat).isNotNull
        assertThat(resultat!!.start).isEqualTo(LocalDate.of(2026, 9, 1))
        assertThat(resultat.slutt).isEqualTo(LocalDate.of(2026, 11, 30))
        assertThat(resultat.harArbeidsgiver).isTrue()
        assertThat(resultat.radSistEndret).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES))
    }

    @Test
    fun `upsert med eksisterende bruker skal oppdatere raden`() {
        val oppdatertTilfelle = sykmeldtperiode.copy(
            arbeidstakerAtTilfelleEnd = false,
            start = LocalDate.of(2026, 10, 1),
            end = LocalDate.of(2026, 12, 31),
        )

        sykmeldtRepository.upsertSykmeldtperiode(ident, sykmeldtperiode)
        sykmeldtRepository.upsertSykmeldtperiode(ident, oppdatertTilfelle)

        val resultat = sykmeldtRepository.hentSykmeldtPeriode(ident)

        assertThat(resultat).isNotNull
        assertThat(resultat!!.start).isEqualTo(LocalDate.of(2026, 10, 1))
        assertThat(resultat.slutt).isEqualTo(LocalDate.of(2026, 12, 31))
        assertThat(resultat.harArbeidsgiver).isFalse()
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ${PostgresTable.SYKMELDT.TABLE_NAME}",
                Int::class.java
            )
        ).isEqualTo(1)
    }

    @Test
    fun `hentSykmeldtPeriode med ikke-eksisterende bruker skal returnere null`() {
        assertThat(sykmeldtRepository.hentSykmeldtPeriode(ident)).isNull()
    }

    @Test
    fun `slettSykmeldtForBruker skal slette rad`() {
        sykmeldtRepository.upsertSykmeldtperiode(ident, sykmeldtperiode)
        assertThat(sykmeldtRepository.hentSykmeldtPeriode(ident)).isNotNull

        sykmeldtRepository.slettSykmeldtForBruker(ident)

        assertThat(sykmeldtRepository.hentSykmeldtPeriode(ident)).isNull()
    }

    @Test
    fun `hentSykmeldtPerioder skal returnere perioder kun for brukere med sykmeldt`() {
        val identSykemeldt1 = "11111111111"
        val identSykmeldt2 = "22222222222"
        val identIkkeSykmeldt = "33333333333"

        sykmeldtRepository.upsertSykmeldtperiode(identSykemeldt1, sykmeldtperiode)
        sykmeldtRepository.upsertSykmeldtperiode(
            identSykmeldt2,
            sykmeldtperiode.copy(arbeidstakerAtTilfelleEnd = false, end = LocalDate.of(2027, 1, 15))
        )

        val resultat = sykmeldtRepository.hentSykmeldtPerioder(
            listOf(identSykemeldt1, identSykmeldt2, identIkkeSykmeldt)
        )

        assertThat(resultat).containsOnlyKeys(identSykemeldt1, identSykmeldt2)
        assertThat(resultat[identSykemeldt1]).usingRecursiveComparison().ignoringFields("radSistEndret").isEqualTo(
            SykmeldtEntity(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 30), true, LocalDateTime.MIN)
        )
        assertThat(resultat[identSykmeldt2]).usingRecursiveComparison().ignoringFields("radSistEndret").isEqualTo(
            SykmeldtEntity(LocalDate.of(2026, 9, 1), LocalDate.of(2027, 1, 15), false, LocalDateTime.MIN)
        )
    }

    @Test
    fun `hentSykmeldtPerioder med tom liste skal returnere tomt map`() {
        assertThat(sykmeldtRepository.hentSykmeldtPerioder(emptyList())).isEmpty()
    }
}
