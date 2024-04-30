package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.pto.veilarbportefolje.database.PostgresTable.*
import no.nav.pto.veilarbportefolje.util.DateUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ProfileringRepository(private val db: JdbcTemplate) {
    fun insertProfilering(sisteProfilering: Profilering) {
        db.update(
            """INSERT INTO ${PROFILERING.TABLE_NAME} (
                    ${PROFILERING.PERIODE_ID}, 
                    ${PROFILERING.PROFILERING_RESULTAT}, 
                    ${PROFILERING.SENDT_INN_TIDSPUNKT}
                ) VALUES (?,?,?)""",
            sisteProfilering.periodeId,
            sisteProfilering.profileringsresultat.name,
            DateUtils.toTimestamp(sisteProfilering.sendtInnTidspunkt)
        )
    }
}
