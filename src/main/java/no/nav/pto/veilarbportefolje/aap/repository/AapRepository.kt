package no.nav.pto.veilarbportefolje.aap.repository

import no.nav.poao_tilgang.client.NorskIdent
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakResponseDto
import no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class AapRepository(private val db: JdbcTemplate) {

    fun upsertAap(norskIdent: NorskIdent, aap: AapVedtakResponseDto.Vedtak) {
        db.update(
            """INSERT INTO YTELSER_AAP 
                (NORSK_IDENT, STATUS, SAKSID, NYESTE_PERIODE_FOM, NYESTE_PERIODE_TOM, RETTIGHETSTYPE, OPPHORSAARSAK, RAD_SIST_ENDRET)
                 VALUES (?,?,?,?,?,?,?,current_timestamp) 
                ON CONFLICT (NORSK_IDENT) 
                DO UPDATE SET (SAKSID, STATUS, NYESTE_PERIODE_FOM, NYESTE_PERIODE_TOM, RETTIGHETSTYPE, OPPHORSAARSAK, RAD_SIST_ENDRET) =
                (excluded.SAKSID, excluded.STATUS, excluded.NYESTE_PERIODE_FOM, excluded.NYESTE_PERIODE_TOM, excluded.RETTIGHETSTYPE, excluded.OPPHORSAARSAK, current_timestamp)
            """,
            norskIdent,
            aap.status,
            aap.saksnummer,
            aap.periode.fraOgMedDato,
            aap.periode.tilOgMedDato,
            aap.rettighetsType,
            aap.opphorsAarsak
        )
    }

    fun hentAap(norskIdent: NorskIdent): AapVedtakPeriode? {
        return queryForObjectOrNull {
            db.queryForObject(
                """SELECT * FROM YTELSER_AAP WHERE NORSK_IDENT = ?""",
                { rs, _ ->
                    AapVedtakPeriode(
                        status = rs.getString("STATUS"),
                        saksid = rs.getString("SAKSID"),
                        periodeFom = rs.getDate("NYESTE_PERIODE_FOM").toLocalDate(),
                        periodeTom = rs.getDate("NYESTE_PERIODE_TOM").toLocalDate()
                    )
                }
            )
        }
    }
}

