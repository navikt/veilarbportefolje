package no.nav.pto.veilarbportefolje.registrering.endring

import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.AktorId
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.pto.veilarbportefolje.postgres.PostgresUtils
import no.nav.pto.veilarbportefolje.util.DateUtils
import no.nav.pto.veilarbportefolje.util.SecureLog
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.time.Instant
import java.util.*

@Slf4j
@Repository
@RequiredArgsConstructor
class EndringIRegistreringRepository(private val db: JdbcTemplate) {
    fun upsertEndringIRegistrering(kafkaRegistreringMelding: ArbeidssokerBesvarelseEvent) {
        val brukersSituasjonSistEndret =
            Optional.ofNullable(kafkaRegistreringMelding.besvarelse?.dinSituasjon?.endretTidspunkt)
                .map { instant: Instant? -> Timestamp.from(instant) }
                .orElse(null)
        db.update(
            """
                INSERT INTO ENDRING_I_REGISTRERING
                (AKTOERID, BRUKERS_SITUASJON, BRUKERS_SITUASJON_SIST_ENDRET)
                VALUES (?, ?, ?)
                ON CONFLICT (AKTOERID)
                DO UPDATE SET
                (BRUKERS_SITUASJON, BRUKERS_SITUASJON_SIST_ENDRET) =
                (excluded.brukers_situasjon, excluded.brukers_situasjon_sist_endret)
            """.trimIndent(),
            kafkaRegistreringMelding.aktorId,
            kafkaRegistreringMelding.besvarelse?.dinSituasjon?.verdi?.toString(),
            brukersSituasjonSistEndret
        )
    }

    fun slettEndringIRegistrering(aktoerId: AktorId): Int {
        return db.update("DELETE FROM ENDRING_I_REGISTRERING WHERE AKTOERID = ?", aktoerId.get())
    }

    fun hentBrukerEndringIRegistrering(aktoerId: AktorId): Optional<EndringIRegistreringDTO> {
        SecureLog.secureLog.info("Hent endring i registreringsdata for bruker: {}", aktoerId.get())
        val sql = "SELECT * FROM ENDRING_I_REGISTRERING WHERE AKTOERID = ?"
        return Optional.ofNullable(
            PostgresUtils.queryForObjectOrNull {
                db.queryForObject(
                    sql,
                    { rs: ResultSet, i: Int -> mapTilEndringIRegistreringDTO(rs) },
                    aktoerId.get()
                )
            }
        )
    }

    @Throws(SQLException::class)
    private fun mapTilEndringIRegistreringDTO(rs: ResultSet): EndringIRegistreringDTO {
        return EndringIRegistreringDTO(
            rs.getString("AKTOERID"), rs.getString("BRUKERS_SITUASJON"), DateUtils.toLocalDate(
                rs.getTimestamp("BRUKERS_SITUASJON_SIST_ENDRET")
            )
        )
    }
}
