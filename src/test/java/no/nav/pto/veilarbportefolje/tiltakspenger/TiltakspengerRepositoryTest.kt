package no.nav.pto.veilarbportefolje.tiltakspenger

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSER_TILTAKSPENGER
import no.nav.pto.veilarbportefolje.tiltakspenger.dto.TiltakspengerResponseDto
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerVedtakEntity
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
class TiltakspengerRepositoryTest(
    @param:Autowired val tiltakspengerRepository: TiltakspengerRespository,
    @param:Autowired val jdbcTemplate: JdbcTemplate
) {

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE TABLE ${YTELSER_TILTAKSPENGER.TABLE_NAME}")
    }

    @Test
    fun `upsert med ny bruker skal inserte ny rad`() {
        val ident = "123456789"
        val clientReponse = tiltakspengerResponseDto

        tiltakspengerRepository.upsertTiltakspenger(ident, clientReponse)

        val resultatAvHenting = tiltakspengerRepository.hentTiltakspenger(ident)
        assertThat(resultatAvHenting).isNotNull
        assertThat(resultatAvHenting?.fom).isEqualTo(LocalDate.of(2024, 1, 1))
        assertThat(resultatAvHenting?.tom).isEqualTo(LocalDate.of(2025, 12, 13))
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

        tiltakspengerRepository.upsertTiltakspenger(ident, vedtak_nr1)
        tiltakspengerRepository.upsertTiltakspenger(ident, vedtak_nr2)

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

        tiltakspengerRepository.upsertTiltakspenger(ident, vedtak)
        val resultatAvHenting = tiltakspengerRepository.hentTiltakspenger(ident)
        assertThat(resultatAvHenting).isNotNull

        tiltakspengerRepository.slettTiltakspengerForBruker(ident)
        val resultatEtterSletting = tiltakspengerRepository.hentTiltakspenger(ident)
        assertThat(resultatEtterSletting).isNull()
    }

    @Test
    fun `hentTiltakspengerVedtak skal returnere vedtak kun for brukere med tiltakspenger`() {
        val identMedTiltakspenger1 = "11111111111"
        val identMedTiltakspenger2 = "22222222222"
        val identUtenTiltakspenger = "33333333333"

        tiltakspengerRepository.upsertTiltakspenger(identMedTiltakspenger1, tiltakspengerResponseDto)
        tiltakspengerRepository.upsertTiltakspenger(
            identMedTiltakspenger2,
            tiltakspengerResponseDto.copy(
                sakId = "SAK-2",
                tom = LocalDate.of(2026, 12, 31),
                rettighet = TiltakspengerRettighet.TILTAKSPENGER_OG_BARNETILLEGG
            )
        )

        val resultat = tiltakspengerRepository.hentTiltakspengerVedtak(
            listOf(identMedTiltakspenger1, identMedTiltakspenger2, identUtenTiltakspenger)
        )

        assertThat(resultat).containsOnlyKeys(identMedTiltakspenger1, identMedTiltakspenger2)
        assertThat(resultat[identMedTiltakspenger1]!!.radSistEndret).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(resultat[identMedTiltakspenger2]!!.radSistEndret).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(resultat[identMedTiltakspenger1]).usingRecursiveComparison().ignoringFields("radSistEndret").isEqualTo(
            TiltakspengerVedtakEntity(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2025, 12, 13),
                TiltakspengerRettighet.TILTAKSPENGER,
                LocalDateTime.MIN
            )
        )
        assertThat(resultat[identMedTiltakspenger2]).usingRecursiveComparison().ignoringFields("radSistEndret").isEqualTo(
            TiltakspengerVedtakEntity(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2026, 12, 31),
                TiltakspengerRettighet.TILTAKSPENGER_OG_BARNETILLEGG,
                LocalDateTime.MIN
            )
        )
    }

    val tiltakspengerResponseDto = TiltakspengerResponseDto(
        sakId = "SAK-1",
        fom = LocalDate.of(2024, 1, 1),
        tom = LocalDate.of(2025, 12, 13),
        rettighet = TiltakspengerRettighet.TILTAKSPENGER,
        kilde = "tp"
    )


}
