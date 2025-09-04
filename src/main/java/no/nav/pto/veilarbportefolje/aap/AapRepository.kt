package no.nav.pto.veilarbportefolje.aap

import no.nav.poao_tilgang.client.NorskIdent
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakResponseDto
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class AapRepository(private val db: JdbcTemplate) {

    fun upsertAap(norskIdent: NorskIdent, aap: AapVedtakResponseDto.Vedtak) {
        db.update(
            """INSERT INTO YTELSER_AAP 
                (NORSK_IDENT, SAKSID, NYESTE_PERIODE_FOM, NYESTE_PERIODE_TOM, RETTIGHETSTYPE, OPPHORSAARSAK, RAD_SIST_ENDRET)
                 VALUES (?,?,?,?,?,?,current_timestamp) 
                ON CONFLICT (NORSK_IDENT) 
                DO UPDATE SET (SAKSID, NYESTE_PERIODE_FOM, NYESTE_PERIODE_TOM, RETTIGHETSTYPE, OPPHORSAARSAK, RAD_SIST_ENDRET) =
                (excluded.SAKSID, excluded.NYESTE_PERIODE_FOM, excluded.NYESTE_PERIODE_TOM, excluded.RETTIGHETSTYPE, excluded.OPPHORSAARSAK, current_timestamp)
            """,
            norskIdent,
            aap.saksnummer,
            aap.periode.fraOgMedDato,
            aap.periode.tilOgMedDato,
            aap.rettighetsType,
            aap.opphorsAarsak
        )
    }


}
