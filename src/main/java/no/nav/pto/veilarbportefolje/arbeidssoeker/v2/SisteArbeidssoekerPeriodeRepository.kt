package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.database.PostgresTable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class SisteArbeidssoekerPeriodeRepository(
    private val db: JdbcTemplate
) {
    fun upsertSisteArbeidssoekerPeriode(fnr: Fnr, periodeId: UUID) {
        db.update(
            """INSERT INTO ${PostgresTable.SISTE_ARBEIDSSOEKER_PERIODE.TABLE_NAME} values (?,?)""",
            periodeId,
            fnr.get()
        )
    }

    fun slettSisteArbeidssoekerPeriode(fnr: Fnr) {
        db.update(
            """DELETE FROM ${PostgresTable.SISTE_ARBEIDSSOEKER_PERIODE.TABLE_NAME} WHERE FNR = ?""",
            fnr.get()
        )
    }
}