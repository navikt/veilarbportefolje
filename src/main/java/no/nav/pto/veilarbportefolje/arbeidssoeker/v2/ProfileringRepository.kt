package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.pto.veilarbportefolje.database.PostgresTable
import no.nav.pto.veilarbportefolje.util.DateUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime
import java.util.*

@Repository
class ProfileringRepository(private val db: JdbcTemplate) {
    fun upsertProfilering(sisteProfilering: Profilering) {
        db.update(
            """INSERT INTO ${PostgresTable.PROFILERING.TABLE_NAME} values (?,?,?)""",
            sisteProfilering.periodeId,
            sisteProfilering.profileringsresultat.name,
            DateUtils.toTimestamp(sisteProfilering.sendtInnTidspunkt)
        )
    }
}

data class Profilering(
    val periodeId: UUID,
    val profileringsresultat: Profileringsresultat,
    val sendtInnTidspunkt: ZonedDateTime
)

enum class Profileringsresultat {
    UKJENT_VERDI,
    UDEFINERT,
    ANTATT_GODE_MULIGHETER,
    ANTATT_BEHOV_FOR_VEILEDNING,
    OPPGITT_HINDRINGER
}

fun ProfileringResponse.toProfilering(): Profilering {
    return Profilering(
        periodeId = this.periodeId,
        profileringsresultat = Profileringsresultat.valueOf(this.profilertTil.name),
        sendtInnTidspunkt = this.sendtInnAv.tidspunkt
    )
}