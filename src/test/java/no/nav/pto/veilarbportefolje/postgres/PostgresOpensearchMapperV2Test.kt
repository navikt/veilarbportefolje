package no.nav.pto.veilarbportefolje.postgres

import no.nav.common.types.identer.NorskIdent
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.database.PostgresTable.HENDELSE
import no.nav.pto.veilarbportefolje.hendelsesfilter.HendelseRepository
import no.nav.pto.veilarbportefolje.hendelsesfilter.Kategori
import no.nav.pto.veilarbportefolje.hendelsesfilter.genererRandomHendelse
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
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@SpringBootTest(classes = [ApplicationConfigTest::class])
class PostgresOpensearchMapperV2Test(
    @param:Autowired val postgresOpensearchMapperV2: PostgresOpensearchMapperV2,
    @param:Autowired val pdlIdentRepository: PdlIdentRepository,
    @param:Autowired val hendelseRepository: HendelseRepository,
    @param:Autowired val jdbcTemplate: JdbcTemplate
) {

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE TABLE huskelapp")
        jdbcTemplate.update("TRUNCATE TABLE fargekategori")
        jdbcTemplate.update("TRUNCATE TABLE bruker_identer")
        jdbcTemplate.update("TRUNCATE TABLE ${HENDELSE.TABLE_NAME}")
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
        postgresOpensearchMapperV2.flettInnData(listOf(bruker))

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
        postgresOpensearchMapperV2.flettInnData(listOf(bruker))

        assertThat(bruker.huskelapp?.huskelappId).isEqualTo(huskelappPaAktivFnr.toString())
    }

    @Test
    fun `flettInnHuskelapp skal ignorere inaktive huskelapper`() {
        val fnr = randomFnr().get()

        lagreIdenter(fnr)
        lagreHuskelapp(UUID.randomUUID(), fnr, "0220", LocalDateTime.now(), null, "inaktiv", status = "IKKE_AKTIV")

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresOpensearchMapperV2.flettInnData(listOf(bruker))

        assertThat(bruker.huskelapp).isNull()
    }

    @Test
    fun `flettInnFargekategori skal finne fargekategori lagret på historisk fnr`() {
        val fnr = randomFnr().get()
        val historiskFnr = randomFnr().get()

        lagreIdenter(fnr, historisk = listOf(historiskFnr))
        lagreFargekategori(historiskFnr, "FARGEKATEGORI_A", "0220", LocalDateTime.now())

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresOpensearchMapperV2.flettInnData(listOf(bruker))

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
        postgresOpensearchMapperV2.flettInnData(listOf(bruker))

        assertThat(bruker.fargekategori).isEqualTo("FARGEKATEGORI_A")
        assertThat(bruker.fargekategori_enhetId).isEqualTo("0220")
    }

    @Test
    fun `skal sette huskelapp og fargekategori til null når bruker ikke har noen`() {
        val fnr = randomFnr().get()
        lagreIdenter(fnr)

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresOpensearchMapperV2.flettInnData(listOf(bruker))

        assertThat(bruker.huskelapp).isNull()
        assertThat(bruker.fargekategori).isNull()
        assertThat(bruker.fargekategori_enhetId).isNull()
    }

    @Test
    fun `flettInnEldsteHendelsePerKategori skal velge eldste hendelse på tvers av aktive og historiske fnr`() {
        val fnr = randomFnr().get()
        val historiskFnr = randomFnr().get()
        val naa = ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS)
        lagreIdenter(fnr, historisk = listOf(historiskFnr))
        val hendelsePaaHistoriskFnr = genererRandomHendelse(
            personIdent = NorskIdent.of(historiskFnr),
            kategori = Kategori.UTGATT_VARSEL,
            hendelseDato = naa.minusDays(1)
        )
        val hendelsePaaAktivFnr = genererRandomHendelse(
            personIdent = NorskIdent.of(fnr),
            kategori = Kategori.UTGATT_VARSEL,
            hendelseDato = naa
        )
        val annenKategoriPaaAktivFnr = genererRandomHendelse(
            personIdent = NorskIdent.of(fnr),
            kategori = Kategori.UDELT_SAMTALEREFERAT
        )
        hendelseRepository.insert(hendelsePaaHistoriskFnr)
        hendelseRepository.insert(hendelsePaaAktivFnr)
        hendelseRepository.insert(annenKategoriPaaAktivFnr)

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresOpensearchMapperV2.flettInnData(listOf(bruker))

        assertThat(bruker.hendelser).containsOnlyKeys(Kategori.UTGATT_VARSEL, Kategori.UDELT_SAMTALEREFERAT)
        assertThat(bruker.hendelser!![Kategori.UTGATT_VARSEL]).isEqualTo(hendelsePaaHistoriskFnr.hendelse)
        assertThat(bruker.hendelser!![Kategori.UDELT_SAMTALEREFERAT]).isEqualTo(annenKategoriPaaAktivFnr.hendelse)
    }

    @Test
    fun `flettInnEldsteHendelsePerKategori skal sette tomt kart når bruker ikke har hendelser`() {
        val fnr = randomFnr().get()
        lagreIdenter(fnr)

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresOpensearchMapperV2.flettInnData(listOf(bruker))

        assertThat(bruker.hendelser).isEmpty()
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
