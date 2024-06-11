package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.pto.veilarbportefolje.database.PostgresTable.PROFILERING
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ProfileringRepository(private val db: JdbcTemplate) {
    fun insertProfilering(sisteProfileringEntity: ProfileringEntity) {
        db.update(
            """INSERT INTO ${PROFILERING.TABLE_NAME} (
                    ${PROFILERING.PERIODE_ID}, 
                    ${PROFILERING.PROFILERING_RESULTAT}, 
                    ${PROFILERING.SENDT_INN_TIDSPUNKT}
                ) VALUES (?,?,?)
                 ON CONFLICT (${PROFILERING.PERIODE_ID}) DO UPDATE SET
                 (${PROFILERING.PROFILERING_RESULTAT}, ${PROFILERING.SENDT_INN_TIDSPUNKT}) = (excluded.${PROFILERING.PROFILERING_RESULTAT}, excluded.${PROFILERING.SENDT_INN_TIDSPUNKT})
                """,
            sisteProfileringEntity.periodeId,
            sisteProfileringEntity.profileringsresultat,
            sisteProfileringEntity.sendtInnTidspunkt
        )
    }
}
