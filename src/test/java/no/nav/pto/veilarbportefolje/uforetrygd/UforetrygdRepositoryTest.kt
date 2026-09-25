package no.nav.pto.veilarbportefolje.uforetrygd

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.database.PostgresTable
import no.nav.pto.veilarbportefolje.uforetrygd.dto.UforetrygdResponseDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate


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
            uføregrad = 60,
            virkningsdato = LocalDate.of(2025, 2, 1)
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

    val uforetrygdResponseDto = UforetrygdResponseDto(
        uføregrad = 50,
        virkningsdato = LocalDate.of(2024, 1, 1)
    )


}
