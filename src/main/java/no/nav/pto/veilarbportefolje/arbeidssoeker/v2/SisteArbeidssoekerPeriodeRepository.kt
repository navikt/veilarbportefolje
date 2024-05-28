package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.database.PostgresTable.SISTE_ARBEIDSSOEKER_PERIODE.*
import no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class SisteArbeidssoekerPeriodeRepository(
    private val db: JdbcTemplate
) {
    fun insertSisteArbeidssoekerPeriode(fnr: Fnr, periodeId: UUID) {
        db.update(
            """INSERT INTO $TABLE_NAME values (?,?)""",
            periodeId,
            fnr.get()
        )
    }

    fun hentSisteArbeidssoekerPeriode(periodeId: UUID): ArbeidssoekerPeriode? {
        return queryForObjectOrNull {
            db.queryForObject(
                """SELECT * FROM $TABLE_NAME WHERE $ARBEIDSSOKER_PERIODE_ID = ?""",
                { rs, _ ->
                    ArbeidssoekerPeriode(
                        rs.getObject(ARBEIDSSOKER_PERIODE_ID, UUID::class.java),
                        Fnr.of(rs.getString(FNR))
                    )
                },
                periodeId
            )
        }
    }

    fun slettSisteArbeidssoekerPeriode(fnr: Fnr) {
        db.update(
            """DELETE FROM $TABLE_NAME WHERE FNR = ?""",
            fnr.get()
        )
    }
}