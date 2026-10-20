package no.nav.pto.veilarbportefolje.postgres

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
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
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest(classes = [ApplicationConfigTest::class])
class PostgresHuskelappOgFargekategoriMapperTest(
    @param:Autowired val postgresHuskelappOgFargekategoriMapper: PostgresHuskelappOgFargekategoriMapper,
    @param:Autowired val pdlIdentRepository: PdlIdentRepository,
    @param:Autowired val jdbcTemplate: JdbcTemplate
) {

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE TABLE huskelapp")
        jdbcTemplate.update("TRUNCATE TABLE fargekategori")
        jdbcTemplate.update("TRUNCATE TABLE bruker_identer")
    }

    @Test
    fun `flettInnHuskelapp skal finne aktiv huskelapp lagret på historisk fnr`() {
        val fnr = randomFnr().get()
        val historiskFnr = randomFnr().get()
        val huskelappId = UUID.randomUUID()
        val frist = LocalDate.now().plusDays(10)

        lagreIdenter(fnr, historisk = listOf(historiskFnr))
        lagreHuskelapp(huskelappId, historiskFnr, "0220", LocalDateTime.now(), frist, "kommentar")

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresHuskelappOgFargekategoriMapper.flettInnHuskelappOgFargekategori(listOf(bruker))

        assertThat(bruker.huskelapp?.huskelappId).isEqualTo(huskelappId.toString())
        assertThat(bruker.huskelapp?.frist).isEqualTo(frist)
        assertThat(bruker.huskelapp?.kommentar).isEqualTo("kommentar")
        assertThat(bruker.huskelapp?.enhetId).isEqualTo("0220")
        assertThat(bruker.huskelapp?.endretAv).isEqualTo("Z123456")
    }

    @Test
    fun `flettInnHuskelapp skal velge huskelapp på ikke-historisk fnr når det finnes aktiv huskelapp på flere fnr`() {
        val fnr = randomFnr().get()
        val historiskFnr = randomFnr().get()
        val huskelappPaAktivFnr = UUID.randomUUID()

        lagreIdenter(fnr, historisk = listOf(historiskFnr))
        lagreHuskelapp(huskelappPaAktivFnr, fnr, "0220", LocalDateTime.now().minusDays(5), null, "aktiv")
        lagreHuskelapp(UUID.randomUUID(), historiskFnr, "0220", LocalDateTime.now(), null, "historisk")

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresHuskelappOgFargekategoriMapper.flettInnHuskelappOgFargekategori(listOf(bruker))

        assertThat(bruker.huskelapp?.huskelappId).isEqualTo(huskelappPaAktivFnr.toString())
    }

    @Test
    fun `flettInnHuskelapp skal ignorere inaktive huskelapper`() {
        val fnr = randomFnr().get()

        lagreIdenter(fnr)
        lagreHuskelapp(UUID.randomUUID(), fnr, "0220", LocalDateTime.now(), null, "inaktiv", status = "IKKE_AKTIV")

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresHuskelappOgFargekategoriMapper.flettInnHuskelappOgFargekategori(listOf(bruker))

        assertThat(bruker.huskelapp).isNull()
    }

    @Test
    fun `flettInnFargekategori skal finne fargekategori lagret på historisk fnr`() {
        val fnr = randomFnr().get()
        val historiskFnr = randomFnr().get()

        lagreIdenter(fnr, historisk = listOf(historiskFnr))
        lagreFargekategori(historiskFnr, "FARGEKATEGORI_A", "0220", LocalDateTime.now())

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresHuskelappOgFargekategoriMapper.flettInnHuskelappOgFargekategori(listOf(bruker))

        assertThat(bruker.fargekategori).isEqualTo("FARGEKATEGORI_A")
        assertThat(bruker.fargekategori_enhetId).isEqualTo("0220")
    }

    @Test
    fun `flettInnFargekategori skal velge fargekategori på ikke-historisk fnr når det finnes på flere fnr`() {
        val fnr = randomFnr().get()
        val historiskFnr = randomFnr().get()

        lagreIdenter(fnr, historisk = listOf(historiskFnr))
        lagreFargekategori(fnr, "FARGEKATEGORI_A", "0220", LocalDateTime.now().minusDays(5))
        lagreFargekategori(historiskFnr, "FARGEKATEGORI_B", "0330", LocalDateTime.now())

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresHuskelappOgFargekategoriMapper.flettInnHuskelappOgFargekategori(listOf(bruker))

        assertThat(bruker.fargekategori).isEqualTo("FARGEKATEGORI_A")
        assertThat(bruker.fargekategori_enhetId).isEqualTo("0220")
    }

    @Test
    fun `skal sette huskelapp og fargekategori til null når bruker ikke har noen`() {
        val fnr = randomFnr().get()
        lagreIdenter(fnr)

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresHuskelappOgFargekategoriMapper.flettInnHuskelappOgFargekategori(listOf(bruker))

        assertThat(bruker.huskelapp).isNull()
        assertThat(bruker.fargekategori).isNull()
        assertThat(bruker.fargekategori_enhetId).isNull()
    }

    private fun lagreIdenter(fnr: String, historisk: List<String> = emptyList()) {
        pdlIdentRepository.upsertIdenter(
            listOf(
                PDLIdent(randomAktorId().get(), false, Gruppe.AKTORID),
                PDLIdent(fnr, false, Gruppe.FOLKEREGISTERIDENT)
            ) + historisk.map { PDLIdent(it, true, Gruppe.FOLKEREGISTERIDENT) }
        )
    }

    private fun lagreHuskelapp(
        huskelappId: UUID,
        fnr: String,
        enhetId: String,
        endretDato: LocalDateTime,
        frist: LocalDate?,
        kommentar: String?,
        status: String = "AKTIV"
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO huskelapp (huskelapp_id, fnr, enhet_id, endret_av_veileder, endret_dato, frist, kommentar, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            huskelappId, fnr, enhetId, "Z123456", Timestamp.valueOf(endretDato),
            frist?.let { Timestamp.valueOf(it.atStartOfDay()) }, kommentar, status
        )
    }

    private fun lagreFargekategori(fnr: String, verdi: String, enhetId: String, sistEndret: LocalDateTime) {
        jdbcTemplate.update(
            """
            INSERT INTO fargekategori (id, fnr, verdi, sist_endret, sist_endret_av_veilederident, enhet_id)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            UUID.randomUUID(), fnr, verdi, Timestamp.valueOf(sistEndret), "Z123456", enhetId
        )
    }
}
