package no.nav.pto.veilarbportefolje.postgres

import no.nav.pto.veilarbportefolje.aap.AapRepository
import no.nav.pto.veilarbportefolje.aap.domene.AapEntity
import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakStatus
import no.nav.pto.veilarbportefolje.aap.dto.AapVedtakResponseDto
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSER_AAP
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest(classes = [ApplicationConfigTest::class])
class PostgresYtelserMapperTest(
    @param:Autowired val postgresYtelserMapper: PostgresYtelserMapper,
    @param:Autowired val aapRepository: AapRepository,
    @param:Autowired val pdlIdentRepository: PdlIdentRepository,
    @param:Autowired val jdbcTemplate: JdbcTemplate
) {

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE TABLE ${YTELSER_AAP.TABLE_NAME}")
        jdbcTemplate.update("TRUNCATE TABLE bruker_identer")
    }

    @Test
    fun `flettInnAapKelvin skal finne aap lagret på historisk fnr`() {
        val fnr = randomFnr().get()
        val historiskFnr = randomFnr().get()
        val tilOgMed = LocalDate.now().plusMonths(3)
        val maksdato = LocalDate.now().plusYears(2)

        pdlIdentRepository.upsertIdenter(
            listOf(
                PDLIdent(randomAktorId().get(), false, Gruppe.AKTORID),
                PDLIdent(fnr, false, Gruppe.FOLKEREGISTERIDENT),
                PDLIdent(historiskFnr, true, Gruppe.FOLKEREGISTERIDENT)
            )
        )
        aapRepository.upsertAap(historiskFnr, AapEntity("FERDIGBEHANDLET", maksdato, aapVedtak(tilOgMed)))

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresYtelserMapper.flettInnAapKelvin(listOf(bruker))

        assertThat(bruker.aap_kelvin).isTrue()
        assertThat(bruker.aap_kelvin_tom_vedtaksdato).isEqualTo(tilOgMed)
        assertThat(bruker.aap_kelvin_rettighetstype).isEqualTo(AapRettighetstype.BISTANDSBEHOV)
        assertThat(bruker.aap_kelvin_maksdato).isEqualTo(maksdato)
    }

    @Test
    fun `flettInnAapKelvin skal velge aap på ikke-historisk fnr når det finnes på flere fnr`() {
        val fnr = randomFnr().get()
        val historiskFnr = randomFnr().get()
        val tilOgMedGjeldende = LocalDate.now().plusMonths(3)
        val tilOgMedHistorisk = LocalDate.now().plusMonths(6)

        pdlIdentRepository.upsertIdenter(
            listOf(
                PDLIdent(randomAktorId().get(), false, Gruppe.AKTORID),
                PDLIdent(fnr, false, Gruppe.FOLKEREGISTERIDENT),
                PDLIdent(historiskFnr, true, Gruppe.FOLKEREGISTERIDENT)
            )
        )
        aapRepository.upsertAap(fnr, AapEntity("FERDIGBEHANDLET", null, aapVedtak(tilOgMedGjeldende)))
        aapRepository.upsertAap(historiskFnr, AapEntity("FERDIGBEHANDLET", null, aapVedtak(tilOgMedHistorisk)))

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresYtelserMapper.flettInnAapKelvin(listOf(bruker))

        assertThat(bruker.aap_kelvin).isTrue()
        assertThat(bruker.aap_kelvin_tom_vedtaksdato).isEqualTo(tilOgMedGjeldende)
    }

    @Test
    fun `flettInnAapKelvin skal velge sist endrede aap når det kun finnes på historiske fnr`() {
        val fnr = randomFnr().get()
        val historiskFnrEldst = randomFnr().get()
        val historiskFnrNyest = randomFnr().get()
        val tilOgMedEldst = LocalDate.now().plusMonths(6)
        val tilOgMedNyest = LocalDate.now().plusMonths(3)

        pdlIdentRepository.upsertIdenter(
            listOf(
                PDLIdent(randomAktorId().get(), false, Gruppe.AKTORID),
                PDLIdent(fnr, false, Gruppe.FOLKEREGISTERIDENT),
                PDLIdent(historiskFnrEldst, true, Gruppe.FOLKEREGISTERIDENT),
                PDLIdent(historiskFnrNyest, true, Gruppe.FOLKEREGISTERIDENT)
            )
        )
        aapRepository.upsertAap(historiskFnrEldst, AapEntity("FERDIGBEHANDLET", null, aapVedtak(tilOgMedEldst)))
        aapRepository.upsertAap(historiskFnrNyest, AapEntity("FERDIGBEHANDLET", null, aapVedtak(tilOgMedNyest)))
        settRadSistEndret(historiskFnrEldst, LocalDateTime.now().minusDays(2))
        settRadSistEndret(historiskFnrNyest, LocalDateTime.now().minusDays(1))

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresYtelserMapper.flettInnAapKelvin(listOf(bruker))

        assertThat(bruker.aap_kelvin).isTrue()
        assertThat(bruker.aap_kelvin_tom_vedtaksdato).isEqualTo(tilOgMedNyest)
    }

    private fun settRadSistEndret(norskIdent: String, radSistEndret: LocalDateTime) {
        jdbcTemplate.update(
            "UPDATE ${YTELSER_AAP.TABLE_NAME} SET ${YTELSER_AAP.RAD_SIST_ENDRET} = ? WHERE ${YTELSER_AAP.NORSK_IDENT} = ?",
            radSistEndret,
            norskIdent
        )
    }

    private fun aapVedtak(tilOgMed: LocalDate) = AapVedtakResponseDto.Vedtak(
        status = AapVedtakStatus.LØPENDE,
        saksnummer = "SAK-1",
        periode = AapVedtakResponseDto.Periode(
            fraOgMedDato = LocalDate.now().minusMonths(6),
            tilOgMedDato = tilOgMed
        ),
        rettighetsType = AapRettighetstype.BISTANDSBEHOV
    )
}
