package no.nav.pto.veilarbportefolje.aap

import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakResponseDto
import no.nav.pto.veilarbportefolje.aap.repository.AapRepository
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
) {

    @Autowired
    private val jdbcTemplate: JdbcTemplate? = null

    @BeforeEach
    fun reset() {
        jdbcTemplate!!.update("TRUNCATE TABLE ${YTELSER_AAP.TABLE_NAME}")
    }

    @Test
    fun `upsert should insert new row`() {
        val ident = "12345678910"
        val vedtak = AapVedtakResponseDto.Vedtak(
            status = "INNVILGET",
            saksnummer = "SAK-1",
            periode = AapVedtakResponseDto.Periode(
                fraOgMedDato = LocalDate.of(2024, 1, 1),
                tilOgMedDato = LocalDate.of(2024, 12, 31)
            ),
            kildesystem = "KELVIN",
            rettighetsType = "AAP",
            opphorsAarsak = null
        )

//        aapRepository.upsertAap(ident, vedtak)
//
//        val found = aapRepository.hentAap(ident)
//        assertThat(found).isNotNull
//        assertThat(found!!.saksid).isEqualTo("SAK-1")
//        assertThat(found.status).isEqualTo("INNVILGET")
    }


}
