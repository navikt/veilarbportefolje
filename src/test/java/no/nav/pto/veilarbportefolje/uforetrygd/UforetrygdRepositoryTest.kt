package no.nav.pto.veilarbportefolje.uforetrygd

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.database.PostgresTable
import no.nav.pto.veilarbportefolje.uforetrygd.domene.UforetrygdEntity
import no.nav.pto.veilarbportefolje.uforetrygd.dto.UforetrygdResponseDto
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
class UforetrygdRepositoryTest(
    @param:Autowired val uforetrygdRepository: UforetrygdRepository,
    @param:Autowired val jdbcTemplate: JdbcTemplate
) {

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE TABLE ${PostgresTable.YTELSER_UFORETRYGD.TABLE_NAME}")
    }

    @Test
    fun `upsert med ny bruker skal inserte ny rad`() {
        val ident = "123456789"
        val clientReponse = uforetrygdResponseDto

        uforetrygdRepository.upsertUforetrygd(ident, clientReponse)

        val resultatAvHenting = uforetrygdRepository.hentUforetrygd(ident)
        assertThat(resultatAvHenting).isNotNull
        assertThat(resultatAvHenting!!.uføregrad).isEqualTo(50)
        assertThat(resultatAvHenting.virkningsdato).isEqualTo(LocalDate.of(2024, 1, 1))
    }

    @Test
    fun `upsert med eksisterende bruker skal oppdatere ny rad`() {
        val ident = "123456789"
        val vedtak_nr1 = uforetrygdResponseDto

        val vedtak_nr2 = vedtak_nr1.copy(
            uforegrad = 60,
            forsteVirkningstidspunkt = LocalDate.of(2025, 2, 1)
        )

        uforetrygdRepository.upsertUforetrygd(ident, vedtak_nr1)
        uforetrygdRepository.upsertUforetrygd(ident, vedtak_nr2)

        val resultatAvHenting = uforetrygdRepository.hentUforetrygd(ident)
        assertThat(resultatAvHenting).isNotNull
        assertThat(resultatAvHenting!!.uføregrad).isEqualTo(60)
        assertThat(resultatAvHenting.virkningsdato).isEqualTo(LocalDate.of(2025, 2, 1))
    }

    @Test
    fun `hentUforetrygd med ikke-eksisterende bruker skal returnere null`() {
        val ident = "123456789"
        val resultatAvHenting = uforetrygdRepository.hentUforetrygd(ident)
        assertThat(resultatAvHenting).isNull()
    }

    @Test
    fun `slettUforetrygdForBruker skal slette rad`() {
        val ident = "123456789"
        val vedtak = uforetrygdResponseDto

        uforetrygdRepository.upsertUforetrygd(ident, vedtak)
        val resultatAvHenting = uforetrygdRepository.hentUforetrygd(ident)
        assertThat(resultatAvHenting).isNotNull

        uforetrygdRepository.slettUforetrygdForBruker(ident)
        val resultatEtterSletting = uforetrygdRepository.hentUforetrygd(ident)
        assertThat(resultatEtterSletting).isNull()
    }

    @Test
    fun `hentUforetrygdVedtak skal returnere vedtak kun for brukere med uføretrygd`() {
        val identMedUforetrygd1 = "11111111111"
        val identMedUforetrygd2 = "22222222222"
        val identUtenUforetrygd = "33333333333"

        uforetrygdRepository.upsertUforetrygd(identMedUforetrygd1, uforetrygdResponseDto)
        uforetrygdRepository.upsertUforetrygd(
            identMedUforetrygd2,
            uforetrygdResponseDto.copy(uforegrad = 100, forsteVirkningstidspunkt = LocalDate.of(2025, 2, 1))
        )

        val resultat = uforetrygdRepository.hentUforetrygdVedtak(
            listOf(identMedUforetrygd1, identMedUforetrygd2, identUtenUforetrygd)
        )

        assertThat(resultat).containsOnlyKeys(identMedUforetrygd1, identMedUforetrygd2)
        assertThat(resultat[identMedUforetrygd1]!!.radSistEndret).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(resultat[identMedUforetrygd2]!!.radSistEndret).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(resultat[identMedUforetrygd1]).usingRecursiveComparison().ignoringFields("radSistEndret").isEqualTo(
            UforetrygdEntity(50, LocalDate.of(2024, 1, 1), LocalDateTime.MIN)
        )
        assertThat(resultat[identMedUforetrygd2]).usingRecursiveComparison().ignoringFields("radSistEndret").isEqualTo(
            UforetrygdEntity(100, LocalDate.of(2025, 2, 1), LocalDateTime.MIN)
        )
    }

    val uforetrygdResponseDto = UforetrygdResponseDto(
        lopendeUforetrygd = true,
        uforegrad = 50,
        forsteVirkningstidspunkt = LocalDate.of(2024, 1, 1)
    )


}
