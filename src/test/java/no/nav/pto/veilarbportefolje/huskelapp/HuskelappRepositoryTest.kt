package no.nav.pto.veilarbportefolje.huskelapp

import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.domene.value.VeilederId
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappOpprettRequest
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappRedigerRequest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate

@SpringBootTest(classes = [ApplicationConfigTest::class])
class HuskelappRepositoryTest {
    @Autowired
    private val repo: HuskelappRepository? = null

    @Autowired
    private val jdbcTemplate: JdbcTemplate? = null


    private final var fnr1: Fnr = Fnr.ofValidFnr("01010101010")
    private final var fnr2: Fnr = Fnr.ofValidFnr("01010101015")
    private final var fnr4: Fnr = Fnr.ofValidFnr("01010101013")
    private final var frist1: LocalDate = LocalDate.of(2026, 1, 1)

    var enhet0010: EnhetId = EnhetId.of("0010")

    var veilederA: VeilederId = VeilederId.of("Z123456")
    var veilederB: VeilederId = VeilederId.of("Z987654")

    private val huskelapp1 = HuskelappOpprettRequest(
        fnr1,
        frist1, ("Huskelapp nr.1 sin kommentar")
    )

    private val huskelapp2 = HuskelappOpprettRequest(
        fnr2,
        LocalDate.of(2017, 2, 27), ("Huskelapp nr.2 sin kommentar")
    )

    private val huskelappUtenKommentar = HuskelappOpprettRequest(
        fnr4,
        LocalDate.of(2030, 1, 1), (null)
    )

    @BeforeEach
    fun setUp() {
        jdbcTemplate!!.execute("TRUNCATE TABLE HUSKELAPP")
        jdbcTemplate.execute("TRUNCATE TABLE oppfolging_data")
        jdbcTemplate.execute("TRUNCATE TABLE oppfolgingsbruker_arena_v2")
        jdbcTemplate.execute("TRUNCATE TABLE bruker_identer")
    }


    @Test
    fun skalKunneOppretteOgHenteHuskelapp() {
        repo!!.opprettHuskelapp(huskelapp1, veilederA, enhet0010)
        val result = repo.hentAktivHuskelapp(fnr1)
        Assertions.assertThat(result.isPresent).isTrue()
        val result2 = repo.hentAktivHuskelapp(result.get().huskelappId)
        Assertions.assertThat(result2.isPresent).isTrue()
        Assertions.assertThat(result.get().enhetId.toString()).isEqualTo("0010")
            .isEqualTo(result2.get().enhetId.toString())
        Assertions.assertThat(result.get().frist).isEqualTo(frist1).isEqualTo(result2.get().frist)
    }

    @Test
    fun skalKunneOppretteOgRedigereOgHenteHuskelappUtenKommentar() {
        repo!!.opprettHuskelapp(huskelappUtenKommentar, veilederA, enhet0010)
        val huskelappUtenKommentar = repo.hentAktivHuskelapp(huskelappUtenKommentar.brukerFnr)
        Assertions.assertThat(huskelappUtenKommentar.isPresent).isTrue()
        Assertions.assertThat(huskelappUtenKommentar.get().kommentar).isEqualTo(null)
        val nyFrist = LocalDate.of(2025, 10, 11)
        val huskelappRedigerRequest = HuskelappRedigerRequest(
            huskelappUtenKommentar.get().huskelappId,
            this.huskelappUtenKommentar.brukerFnr,
            nyFrist,
            null
        )
        repo.settSisteHuskelappRadIkkeAktiv(huskelappUtenKommentar.get().huskelappId)
        repo.redigerHuskelapp(huskelappRedigerRequest, veilederB, enhet0010)
        val oppdatertHuskelappUtenKommentar = repo.hentAktivHuskelapp(fnr4)
        Assertions.assertThat(oppdatertHuskelappUtenKommentar.isPresent).isTrue()
        Assertions.assertThat(oppdatertHuskelappUtenKommentar.get().kommentar).isEqualTo(null)
    }

    @Test
    fun skalKunneSletteHuskelapp() {
        repo!!.opprettHuskelapp(huskelapp2, veilederA, enhet0010)
        val huskelapp = repo.hentAktivHuskelapp(fnr2)
        Assertions.assertThat(huskelapp.isPresent).isTrue()
        Assertions.assertThat(huskelapp.get().kommentar).isEqualTo("Huskelapp nr.2 sin kommentar")
        repo.settSisteHuskelappRadIkkeAktiv(huskelapp.get().huskelappId)
        val huskelappEtter = repo.hentAktivHuskelapp(fnr2)
        Assertions.assertThat(huskelappEtter.isPresent).isFalse()
    }

    @Test
    fun skalKunneInaktivereNyesteHuskelappRadNaarFlereRader() {
        repo!!.opprettHuskelapp(huskelapp2, veilederA, enhet0010)
        val huskelappFoer = repo.hentAktivHuskelapp(fnr2)
        Assertions.assertThat(huskelappFoer.isPresent).isTrue()
        Assertions.assertThat(huskelappFoer.get().kommentar).isEqualTo("Huskelapp nr.2 sin kommentar")
        val huskelappRedigerRequest = HuskelappRedigerRequest(
            huskelappFoer.get().huskelappId,
            fnr2,
            huskelappFoer.get().frist,
            "ny kommentar på huskelapp nr.2"
        )
        repo.settSisteHuskelappRadIkkeAktiv(huskelappFoer.get().huskelappId)
        repo.redigerHuskelapp(huskelappRedigerRequest, veilederA, enhet0010)
        val alleHuskelappRader = repo.hentAlleRaderPaHuskelapp(huskelappFoer.get().huskelappId)
        Assertions.assertThat(alleHuskelappRader.size).isEqualTo(2)
        repo.settSisteHuskelappRadIkkeAktiv(huskelappFoer.get().huskelappId)
        val huskelappEtter = repo.hentAktivHuskelapp(fnr2)
        Assertions.assertThat(huskelappEtter.isPresent).isFalse()
    }


    @Test
    fun sletterAlleHuskelappRader() {
        repo!!.opprettHuskelapp(huskelapp2, veilederA, enhet0010)
        val huskelappFoer = repo.hentAktivHuskelapp(huskelapp2.brukerFnr)
        Assertions.assertThat(huskelappFoer.isPresent).isTrue()
        Assertions.assertThat(huskelappFoer.get().kommentar).isEqualTo("Huskelapp nr.2 sin kommentar")

        val huskelappRedigerRequest = HuskelappRedigerRequest(
            huskelappFoer.get().huskelappId,
            huskelapp2.brukerFnr,
            huskelappFoer.get().frist,
            "ny kommentar på huskelapp nr.2"
        )
        repo.settSisteHuskelappRadIkkeAktiv(huskelappFoer.get().huskelappId)
        repo.redigerHuskelapp(huskelappRedigerRequest, veilederA, enhet0010)
        repo.settSisteHuskelappRadIkkeAktiv(huskelappFoer.get().huskelappId)
        repo.redigerHuskelapp(huskelappRedigerRequest, veilederA, enhet0010)
        repo.settSisteHuskelappRadIkkeAktiv(huskelappFoer.get().huskelappId)
        repo.redigerHuskelapp(huskelappRedigerRequest, veilederA, enhet0010)

        val alleHuskelappRader = repo.hentAlleRaderPaHuskelapp(huskelappFoer.get().huskelappId)
        Assertions.assertThat(alleHuskelappRader.size).isEqualTo(4)

        repo.slettAlleHuskelappRaderPaaBruker(huskelapp2.brukerFnr)

        val alleHuskelappRader2 = repo.hentAlleRaderPaHuskelapp(huskelappFoer.get().huskelappId)
        Assertions.assertThat(alleHuskelappRader2.size).isEqualTo(0)
    }


    @Test
    fun faarHentetNavkontorPaHuskelapp() {
        repo!!.opprettHuskelapp(huskelapp2, veilederA, enhet0010)
        val enhetId = repo.hentNavkontorPaHuskelapp(huskelapp2.brukerFnr)
        Assertions.assertThat(enhetId.isPresent).isTrue()
    }

    @Test
    fun kasterExceptionVedInsertAvToAktiveHuskelapperSammeBruker() {
        repo!!.opprettHuskelapp(huskelapp1, veilederA, enhet0010)
        val huskelapp = repo.hentAktivHuskelapp(fnr1)
        Assertions.assertThat(huskelapp.isPresent).isTrue()
        val exception = assertThrows(Exception::class.java) {
            repo.opprettHuskelapp(huskelapp1, veilederA, enhet0010)
        }
        Assertions.assertThat(exception.message).contains("duplicate key value violates unique constraint \"unique_fnr_active_status\"")
    }
}
