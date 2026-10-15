package no.nav.pto.veilarbportefolje.aap

import no.nav.pto.veilarbportefolje.aap.domene.AapEntity
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakEntity
import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakStatus
import no.nav.pto.veilarbportefolje.aap.dto.AapVedtakResponseDto
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSER_AAP
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
class AapRepositoryTest(
    @param:Autowired val aapRepository: AapRepository,
    @param:Autowired val jdbcTemplate: JdbcTemplate
) {

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE TABLE ${YTELSER_AAP.TABLE_NAME}")
    }

    @Test
    fun `upsert med ny bruker skal inserte ny rad`() {
        val ident = "123456789"
        val vedtak = aapVedtakDto
        val sakstatus = "FERDIGBEHANDLET"
        val maksdato = LocalDate.of(2028, 12, 31)

        aapRepository.upsertAap(ident, AapEntity(sakstatus, maksdato, vedtak))

        val resultatAvHenting = aapRepository.hentAap(ident)
        assertThat(resultatAvHenting).isNotNull
        assertThat(resultatAvHenting!!.status).isEqualTo(AapVedtakStatus.LØPENDE)
        assertThat(resultatAvHenting.periodeFom).isEqualTo(LocalDate.of(2024, 1, 1))
        assertThat(resultatAvHenting.periodeTom).isEqualTo(LocalDate.of(2024, 12, 31))
        assertThat(resultatAvHenting.maksdato).isEqualTo(maksdato)
        assertThat(resultatAvHenting.sakstatus).isEqualTo(sakstatus)
    }

    @Test
    fun `upsert med eksisterende bruker skal oppdatere ny rad`() {
        val ident = "123456789"
        val vedtak_nr1 = aapVedtakDto
        val sakstatus = "FERDIGBEHANDLET"
        val maksdato = LocalDate.of(2028, 12, 31)

        val vedtak_nr2 = vedtak_nr1.copy(
            periode = AapVedtakResponseDto.Periode(
                fraOgMedDato = LocalDate.of(2025, 1, 1),
                tilOgMedDato = LocalDate.of(2025, 12, 31)
            )
        )

        aapRepository.upsertAap(ident, AapEntity(sakstatus, maksdato, vedtak_nr1))
        aapRepository.upsertAap(ident, AapEntity(sakstatus, maksdato, vedtak_nr2))

        val resultatAvHenting = aapRepository.hentAap(ident)
        assertThat(resultatAvHenting).isNotNull
        assertThat(resultatAvHenting!!.status).isEqualTo(AapVedtakStatus.LØPENDE)
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
        val sakstatus = "FERDIGBEHANDLET"
        val maksdato = LocalDate.of(2028, 12, 31)

        aapRepository.upsertAap(ident, AapEntity(sakstatus, maksdato, vedtak))
        val resultatAvHenting = aapRepository.hentAap(ident)
        assertThat(resultatAvHenting).isNotNull

        aapRepository.slettAapForBruker(ident)
        val resultatEtterSletting = aapRepository.hentAap(ident)
        assertThat(resultatEtterSletting).isNull()
    }

    @Test
    fun `hentAapKelvinVedtak skal returnere vedtak kun for brukere med aap`() {
        val identMedAap1 = "11111111111"
        val identMedAap2 = "22222222222"
        val identUtenAap = "33333333333"
        val maksdato = LocalDate.of(2028, 12, 31)

        aapRepository.upsertAap(identMedAap1, AapEntity("FERDIGBEHANDLET", maksdato, aapVedtakDto))
        aapRepository.upsertAap(
            identMedAap2,
            AapEntity("FERDIGBEHANDLET", null, aapVedtakDto.copy(rettighetsType = AapRettighetstype.STUDENT))
        )

        val resultat = aapRepository.hentAapKelvinVedtak(listOf(identMedAap1, identMedAap2, identUtenAap))

        assertThat(resultat).containsOnlyKeys(identMedAap1, identMedAap2)
        assertThat(resultat[identMedAap1]!!.radSistEndret).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(resultat[identMedAap2]!!.radSistEndret).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(resultat[identMedAap1]).usingRecursiveComparison().ignoringFields("radSistEndret").isEqualTo(
            AapVedtakEntity(LocalDate.of(2024, 12, 31), AapRettighetstype.ARBEIDSSØKER, maksdato, LocalDateTime.MIN)
        )
        assertThat(resultat[identMedAap2]).usingRecursiveComparison().ignoringFields("radSistEndret").isEqualTo(
            AapVedtakEntity(LocalDate.of(2024, 12, 31), AapRettighetstype.STUDENT, null, LocalDateTime.MIN)
        )
    }


    val aapVedtakDto = AapVedtakResponseDto.Vedtak(
        status = AapVedtakStatus.LØPENDE,
        saksnummer = "SAK-1",
        periode = AapVedtakResponseDto.Periode(
            fraOgMedDato = LocalDate.of(2024, 1, 1),
            tilOgMedDato = LocalDate.of(2024, 12, 31)
        ),
        rettighetsType = AapRettighetstype.ARBEIDSSØKER
    )

}
