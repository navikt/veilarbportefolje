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
    fun insertSisteArbeidssoekerPeriode(sisteArbeidssoekerPeriode: ArbeidssoekerPeriodeEntity) {
        db.update(
            """INSERT INTO $TABLE_NAME values (?,?)""",
            sisteArbeidssoekerPeriode.arbeidssoekerperiodeId,
            sisteArbeidssoekerPeriode.fnr
        )
    }

    fun hentSisteArbeidssoekerPeriode(periodeId: UUID): ArbeidssoekerPeriodeEntity? {
        return queryForObjectOrNull {
            db.queryForObject(
                """SELECT * FROM $TABLE_NAME WHERE $ARBEIDSSOKER_PERIODE_ID = ?""",
                { rs, _ ->
                    ArbeidssoekerPeriodeEntity(
                        rs.getObject(ARBEIDSSOKER_PERIODE_ID, UUID::class.java),
                        rs.getString(FNR)
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