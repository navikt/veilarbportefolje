package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.database.PostgresTable.SISTE_ARBEIDSSOEKER_PERIODE
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class SisteArbeidssoekerPeriodeRepositoryTest {
    private val db: JdbcTemplate = SingletonPostgresContainer.init().createJdbcTemplate()
    private val sisteArbeidssoekerPeriodeRepository = SisteArbeidssoekerPeriodeRepository(db)

    private val gammeltFnr = Fnr.of("11111111111")
    private val nyttFnr = Fnr.of("22222222222")

    @BeforeEach
    fun reset() {
        db.update("TRUNCATE ${SISTE_ARBEIDSSOEKER_PERIODE.TABLE_NAME} CASCADE")
    }

    @Test
    fun `oppdaterFnr skal flytte periode fra gammelt til nytt fnr`() {
        val periodeId = UUID.randomUUID()
        sisteArbeidssoekerPeriodeRepository.insertSisteArbeidssoekerPeriode(
            ArbeidssoekerPeriodeEntity(periodeId, gammeltFnr.get())
        )

        sisteArbeidssoekerPeriodeRepository.oppdaterFnr(gammeltFnr, nyttFnr)

        assertThat(sisteArbeidssoekerPeriodeRepository.hentSisteArbeidssoekerPeriode(periodeId)!!.fnr)
            .isEqualTo(nyttFnr.get())
    }

    @Test
    fun `oppdaterFnr skal beholde periode på nytt fnr og slette gammel når begge finnes`() {
        val gammelPeriodeId = UUID.randomUUID()
        val nyPeriodeId = UUID.randomUUID()
        sisteArbeidssoekerPeriodeRepository.insertSisteArbeidssoekerPeriode(
            ArbeidssoekerPeriodeEntity(gammelPeriodeId, gammeltFnr.get())
        )
        sisteArbeidssoekerPeriodeRepository.insertSisteArbeidssoekerPeriode(
            ArbeidssoekerPeriodeEntity(nyPeriodeId, nyttFnr.get())
        )

        sisteArbeidssoekerPeriodeRepository.oppdaterFnr(gammeltFnr, nyttFnr)

        assertThat(sisteArbeidssoekerPeriodeRepository.hentSisteArbeidssoekerPeriode(gammelPeriodeId)).isNull()
        assertThat(sisteArbeidssoekerPeriodeRepository.hentSisteArbeidssoekerPeriode(nyPeriodeId)!!.fnr)
            .isEqualTo(nyttFnr.get())
    }
}
