package no.nav.pto.veilarbportefolje.tiltakshendelse

import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.database.PostgresTable.TILTAKSHENDELSE
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse
import no.nav.pto.veilarbportefolje.tiltakshendelse.dto.input.KafkaTiltakshendelse
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class TiltakshendelseRepository(private val db: JdbcTemplate) {

    fun tryLagreTiltakshendelseOgSjekkOmDenErEldst(tiltakshendelseData: KafkaTiltakshendelse): Boolean {
        verifiserTiltakshendelseTilhorerSammePerson(tiltakshendelseData.fnr, tiltakshendelseData.id)
        upsertTiltakshendelseOgSjekkOmDenErEldst(tiltakshendelseData)
        return hentEldsteTiltakshendelse(tiltakshendelseData.fnr)?.id?.equals(tiltakshendelseData.id) ?: false
    }

    fun upsertTiltakshendelseOgSjekkOmDenErEldst(tiltakshendelse: KafkaTiltakshendelse) {
        try {
            db.update(
                """
                    INSERT INTO tiltakshendelse
                       (${TILTAKSHENDELSE.ID}, 
                        ${TILTAKSHENDELSE.FNR}, 
                        ${TILTAKSHENDELSE.OPPRETTET}, 
                        ${TILTAKSHENDELSE.TEKST}, 
                        ${TILTAKSHENDELSE.LENKE}, 
                        ${TILTAKSHENDELSE.TILTAKSTYPE}, 
                        ${TILTAKSHENDELSE.AVSENDER}, 
                        ${TILTAKSHENDELSE.SIST_ENDRET}
                        )          
                    VALUES (?, ?, ?, ?, ?, ?, ?, now())
                    ON CONFLICT (id)
                    DO UPDATE SET (
                        ${TILTAKSHENDELSE.FNR}, 
                        ${TILTAKSHENDELSE.OPPRETTET}, 
                        ${TILTAKSHENDELSE.TEKST}, 
                        ${TILTAKSHENDELSE.LENKE}, 
                        ${TILTAKSHENDELSE.TILTAKSTYPE}, 
                        ${TILTAKSHENDELSE.AVSENDER}, 
                        ${TILTAKSHENDELSE.SIST_ENDRET}
                    ) = (excluded.${TILTAKSHENDELSE.FNR},
                        excluded.${TILTAKSHENDELSE.OPPRETTET},
                        excluded.${TILTAKSHENDELSE.TEKST},
                        excluded.${TILTAKSHENDELSE.LENKE},
                        excluded.${TILTAKSHENDELSE.TILTAKSTYPE},
                        excluded.${TILTAKSHENDELSE.AVSENDER}, 
                        excluded.${TILTAKSHENDELSE.SIST_ENDRET}
                    )""".trimIndent(),
                tiltakshendelse.id,
                tiltakshendelse.fnr.toString(),
                tiltakshendelse.opprettet,
                tiltakshendelse.tekst,
                tiltakshendelse.lenke,
                tiltakshendelse.tiltakstype.name,
                tiltakshendelse.avsender.name
            )
        } catch (e: Exception) {
            secureLog.error(e.message, e)
            throw RuntimeException("Kunne ikke lagre tiltakshendelse for hendelsesid: " + tiltakshendelse.id)
        }
    }

    fun slettTiltakshendelseOgHentEldste(tiltakshendelseId: UUID, fnr: Fnr): Tiltakshendelse? {
        verifiserTiltakshendelseTilhorerSammePerson(fnr, tiltakshendelseId)
        val sql = "DELETE FROM tiltakshendelse WHERE id = ?"
        try {
            db.update(sql, tiltakshendelseId)
            return hentEldsteTiltakshendelse(fnr)
        } catch (e: KunneIkkeHenteEldsteTiltakhendelseException) {
            throw e
        } catch (e: Exception) {
            secureLog.error(e.message, e)
            throw RuntimeException("Kunne ikke slette tiltakshendelse med id: $tiltakshendelseId")
        }
    }

    fun hentAlleTiltakshendelser(): List<Tiltakshendelse> {
        val sql = "SELECT * FROM tiltakshendelse"

        try {
            return db.queryForList(sql).stream()
                .map { rs: Map<String, Any> -> TiltakshendelseMapper.tiltakshendelseMapper(rs) }
                .toList()
        } catch (e: Exception) {
            secureLog.error(e.message, e)
            throw RuntimeException("Kunne ikke hente alle tiltakshendelser.")
        }
    }

    /**
     * "Eldste tiltakshendelse" er den hendelsen med eldst opprettet-dato,
     * ikkje nødvendigvis den fyrste som vart lagra i databasen.
     * */
    fun hentEldsteTiltakshendelse(fnr: Fnr): Tiltakshendelse? {
        val sql = """
            SELECT id, fnr, opprettet, tekst, lenke, tiltakstype_kode, avsender, sist_endret
              FROM tiltakshendelse
              WHERE fnr = ? 
            ORDER BY opprettet LIMIT 1
        """.trimIndent()

        return try {
            db.queryForObject(sql, TiltakshendelseMapper::tiltakshendelseMapper, fnr.toString())
        } catch (e: EmptyResultDataAccessException) {
            null
        } catch (e: Error) {
            secureLog.error(e.message, e)
            throw KunneIkkeHenteEldsteTiltakhendelseException("Kunne ikke hente eldste tiltakshendelse for bruker.")
        }
    }

    fun verifiserTiltakshendelseTilhorerSammePerson(fnrPaaNyTiltakshendelsemelding: Fnr, tiltakshendelseId: UUID) {
        val lagretFnrPaTiltakshendelse: String? = try {
            val sql = "SELECT fnr FROM tiltakshendelse WHERE id = ?"
            db.queryForObject(sql, String::class.java, tiltakshendelseId)
        } catch (_: Exception) {
            //finnes det ingen tiltakshendelse med id, så er alt ok
            return
        }
        if (lagretFnrPaTiltakshendelse != fnrPaaNyTiltakshendelsemelding.toString()) {
            secureLog.error("Tiltakshendelse med id $tiltakshendelseId tilhører ikke samme person som fnr $fnrPaaNyTiltakshendelsemelding")
            throw TiltakshendelseTilhorerIkkePersonException("Tiltakshendelse tilhører ikke samme person som allerede er lagret i database")
        }
    }
}

class KunneIkkeHenteEldsteTiltakhendelseException(message: String) : RuntimeException(message)
class TiltakshendelseTilhorerIkkePersonException(message: String) : RuntimeException(message)
