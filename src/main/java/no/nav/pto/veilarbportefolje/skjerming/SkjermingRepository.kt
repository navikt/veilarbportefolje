package no.nav.pto.veilarbportefolje.skjerming

import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.database.PostgresTable.NOM_SKJERMING
import org.jetbrains.annotations.TestOnly
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForList
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*

@Service
class SkjermingRepository(private val db: JdbcTemplate) {
    fun settSkjermingPeriode(fnr: Fnr, skjermetFra: Timestamp?, skjermetTil: Timestamp?): Boolean {
        val updatedNum = db.update(
            """
                INSERT INTO  NOM_SKJERMING(FODSELSNR, SKJERMET_FRA, SKJERMET_TIL) VALUES (?,?,?)
                ON CONFLICT (FODSELSNR) DO UPDATE SET SKJERMET_FRA = EXCLUDED.SKJERMET_FRA, SKJERMET_TIL = EXCLUDED.SKJERMET_TIL
                """.trimIndent(),
            fnr.get(), skjermetFra, skjermetTil
        )
        return updatedNum > 0
    }

    fun settSkjerming(fnr: Fnr, erSkjermet: Boolean): Boolean {
        val updatedNum = db.update(
            """
                INSERT INTO NOM_SKJERMING (FODSELSNR, ER_SKJERMET) VALUES (?,?)
                ON CONFLICT (FODSELSNR) DO UPDATE SET ER_SKJERMET = EXCLUDED.ER_SKJERMET
                """.trimIndent(),
            fnr.get(), erSkjermet
        )
        return updatedNum > 0
    }


    fun deleteSkjermingData(fnr: Fnr) {
        db.update(
            """
                DELETE FROM NOM_SKJERMING WHERE FODSELSNR = ?
                """.trimIndent(), fnr.get()
        )
    }

    @TestOnly
    fun hentSkjermingData(fnr: Fnr): Optional<SkjermingData> {
        return Optional.ofNullable(
            db.queryForObject(
                """
                    SELECT ER_SKJERMET, SKJERMET_FRA, SKJERMET_TIL FROM NOM_SKJERMING WHERE FODSELSNR = ?
                    """.trimIndent(),
                { rs: ResultSet, _: Int ->
                    SkjermingData(
                        fnr,
                        rs.getBoolean(NOM_SKJERMING.ER_SKJERMET),
                        rs.getTimestamp(NOM_SKJERMING.SKJERMET_FRA),
                        rs.getTimestamp(NOM_SKJERMING.SKJERMET_TIL)
                    )
                }, fnr.get()
            )
        )
    }

    @TestOnly
    fun hentSkjermetPersoner(fnrs: List<Fnr>): Set<Fnr> {
        val fnrsCondition = fnrs.joinToString(",", "{", "}", transform = Fnr::toString)

        return db.queryForList<String>(
            """
                SELECT FODSELSNR FROM NOM_SKJERMING WHERE ER_SKJERMET AND FODSELSNR = ANY (?::varchar[])
                """.trimIndent(),
            fnrsCondition
        ).map(Fnr::of).toSet()
    }
}
