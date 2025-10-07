package no.nav.pto.veilarbportefolje.tiltakspenger

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSER_TILTAKSPENGER
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerResponseDto
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate


@SpringBootTest(classes = [ApplicationConfigTest::class])
class TiltakspengerRepositoryTest(
    @Autowired val tiltakspengerRepository: TiltakspengerRespository,
    @Autowired val jdbcTemplate: JdbcTemplate
) {

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE TABLE ${YTELSER_TILTAKSPENGER.TABLE_NAME}")
    }

    @Test
    fun `upsert med ny bruker skal inserte ny rad`() {
        val ident = "123456789"
        val clientReponse = tiltakspengerResponseDto

        tiltakspengerRepository.upsertAap(ident, clientReponse)

        val resultatAvHenting = tiltakspengerRepository.hentTiltakspenger(ident)
        assertThat(resultatAvHenting).isNotNull
        assertThat(resultatAvHenting!!.sakId).isEqualTo("SAK-1")
        assertThat(resultatAvHenting.fom).isEqualTo(LocalDate.of(2024, 1, 1))
        assertThat(resultatAvHenting.tom).isEqualTo(LocalDate.of(2025, 12, 13))
    }

    @Test
    fun `upsert med eksisterende bruker skal oppdatere ny rad`() {
        val ident = "123456789"
        val vedtak_nr1 = tiltakspengerResponseDto

        val vedtak_nr2 = vedtak_nr1.copy(
            sakId = "SAK-2",
            fom = LocalDate.of(2025, 2, 1),
            tom = LocalDate.of(2026, 12, 31)
        )

        tiltakspengerRepository.upsertAap(ident, vedtak_nr1)
        tiltakspengerRepository.upsertAap(ident, vedtak_nr2)

        val resultatAvHenting = tiltakspengerRepository.hentTiltakspenger(ident)
        assertThat(resultatAvHenting).isNotNull
        assertThat(resultatAvHenting!!.fom).isEqualTo(LocalDate.of(2025, 2, 1))
        assertThat(resultatAvHenting.tom).isEqualTo(LocalDate.of(2026, 12, 31))
    }

    @Test
    fun `hentTiltakspenger med ikke-eksisterende bruker skal returnere null`() {
        val ident = "123456789"
        val resultatAvHenting = tiltakspengerRepository.hentTiltakspenger(ident)
        assertThat(resultatAvHenting).isNull()
    }

    @Test
    fun `slettTiltakspengerForBruker skal slette rad`() {
        val ident = "123456789"
        val vedtak = tiltakspengerResponseDto

        tiltakspengerRepository.upsertAap(ident, vedtak)
        val resultatAvHenting = tiltakspengerRepository.hentTiltakspenger(ident)
        assertThat(resultatAvHenting).isNotNull

        tiltakspengerRepository.slettTiltakspengerForBruker(ident)
        val resultatEtterSletting = tiltakspengerRepository.hentTiltakspenger(ident)
        assertThat(resultatEtterSletting).isNull()
    }

    val tiltakspengerResponseDto = TiltakspengerResponseDto(
        sakId = "SAK-1",
        fom = LocalDate.of(2024, 1, 1),
        tom = LocalDate.of(2025, 12, 13),
        rettighet = TiltakspengerRettighet.TILTAKSPENGER,
        kilde = "tp"
    )


}
