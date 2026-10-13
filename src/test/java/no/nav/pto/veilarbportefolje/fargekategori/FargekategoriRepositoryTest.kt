package no.nav.pto.veilarbportefolje.fargekategori

import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.domene.VeilederId
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriController.OppdaterFargekategoriRequest
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate

class FargekategoriRepositoryTest {
    private val db: JdbcTemplate = SingletonPostgresContainer.init().createJdbcTemplate()
    private val fargekategoriRepository = FargekategoriRepository(db)

    private val gammeltFnr = Fnr.of("11111111111")
    private val nyttFnr = Fnr.of("22222222222")
    private val veileder = VeilederId.of("Z123456")
    private val enhet = EnhetId.of("0010")

    @BeforeEach
    fun reset() {
        db.update("TRUNCATE TABLE fargekategori")
    }

    @Test
    fun `oppdaterFnr skal flytte fargekategori fra gammelt til nytt fnr`() {
        fargekategoriRepository.upsertFargekateori(
            OppdaterFargekategoriRequest(gammeltFnr, FargekategoriVerdi.FARGEKATEGORI_A), veileder, enhet
        )

        fargekategoriRepository.oppdaterFnr(gammeltFnr, nyttFnr)

        assertThat(fargekategoriRepository.hentFargekategoriForBruker(gammeltFnr)).isEmpty
        assertThat(fargekategoriRepository.hentFargekategoriForBruker(nyttFnr).get().fargekategoriVerdi())
            .isEqualTo(FargekategoriVerdi.FARGEKATEGORI_A)
    }

    @Test
    fun `oppdaterFnr skal beholde fargekategori på nytt fnr og slette gammel når begge finnes`() {
        fargekategoriRepository.upsertFargekateori(
            OppdaterFargekategoriRequest(gammeltFnr, FargekategoriVerdi.FARGEKATEGORI_A), veileder, enhet
        )
        fargekategoriRepository.upsertFargekateori(
            OppdaterFargekategoriRequest(nyttFnr, FargekategoriVerdi.FARGEKATEGORI_B), veileder, enhet
        )

        fargekategoriRepository.oppdaterFnr(gammeltFnr, nyttFnr)

        assertThat(fargekategoriRepository.hentFargekategoriForBruker(gammeltFnr)).isEmpty
        assertThat(fargekategoriRepository.hentFargekategoriForBruker(nyttFnr).get().fargekategoriVerdi())
            .isEqualTo(FargekategoriVerdi.FARGEKATEGORI_B)
    }
}
