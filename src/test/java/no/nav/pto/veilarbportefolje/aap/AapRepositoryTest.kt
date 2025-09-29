package no.nav.pto.veilarbportefolje.aap

import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakResponseDto
import no.nav.pto.veilarbportefolje.aap.repository.AapRepository
import no.nav.pto.veilarbportefolje.aap.repository.AapStatus
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSER_AAP
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate

@SpringBootTest(classes = [ApplicationConfigTest::class])
class AapRepositoryTest(
    @Autowired val aapRepository: AapRepository,
    @Autowired val jdbcTemplate: JdbcTemplate
) {

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE TABLE ${YTELSER_AAP.TABLE_NAME}")
    }

    @Test
    fun `upsert med ny bruker skal inserte ny rad`() {
        val ident = "123456789"
        val vedtak = aapVedtakDto

        aapRepository.upsertAap(ident, vedtak)

        val resultatAvHenting = aapRepository.hentAap(ident)
        assertThat(resultatAvHenting).isNotNull
        assertThat(resultatAvHenting!!.status).isEqualTo(AapStatus.LOPENDE)
        assertThat(resultatAvHenting.periodeFom).isEqualTo(LocalDate.of(2024, 1, 1))
        assertThat(resultatAvHenting.periodeTom).isEqualTo(LocalDate.of(2024, 12, 31))
    }

    @Test
    fun `upsert med eksisterende bruker skal oppdatere ny rad`() {
        val ident = "123456789"
        val vedtak_nr1 = aapVedtakDto

        val vedtak_nr2 = vedtak_nr1.copy(
            saksnummer = "SAK-2",
            periode = AapVedtakResponseDto.Periode(
                fraOgMedDato = LocalDate.of(2025, 1, 1),
                tilOgMedDato = LocalDate.of(2025, 12, 31)
            )
        )

        aapRepository.upsertAap(ident, vedtak_nr1)
        aapRepository.upsertAap(ident, vedtak_nr2)

        val resultatAvHenting = aapRepository.hentAap(ident)
        assertThat(resultatAvHenting).isNotNull
        assertThat(resultatAvHenting!!.status).isEqualTo(AapStatus.LOPENDE)
        assertThat(resultatAvHenting.periodeFom).isEqualTo(LocalDate.of(2025, 1, 1))
        assertThat(resultatAvHenting.periodeTom).isEqualTo(LocalDate.of(2025, 12, 31))
    }


    @Test
    fun `hentAap med ikke-eksisterende bruker skal returnere null`() {
        val ident = "123456789"
        val resultatAvHenting = aapRepository.hentAap(ident)
        assertThat(resultatAvHenting).isNull()
    }

    @Test
    fun `slettAapForBruker skal slette rad`() {
        val ident = "123456789"
        val vedtak = aapVedtakDto

        aapRepository.upsertAap(ident, vedtak)
        val resultatAvHenting = aapRepository.hentAap(ident)
        assertThat(resultatAvHenting).isNotNull

        aapRepository.slettAapForBruker(ident)
        val resultatEtterSletting = aapRepository.hentAap(ident)
        assertThat(resultatEtterSletting).isNull()
    }


    val aapVedtakDto = AapVedtakResponseDto.Vedtak(
        status = "LØPENDE",
        saksnummer = "SAK-1",
        periode = AapVedtakResponseDto.Periode(
            fraOgMedDato = LocalDate.of(2024, 1, 1),
            tilOgMedDato = LocalDate.of(2024, 12, 31)
        ),
        kildesystem = "KELVIN",
        rettighetsType = "AAP",
        opphorsAarsak = null
    )

}
