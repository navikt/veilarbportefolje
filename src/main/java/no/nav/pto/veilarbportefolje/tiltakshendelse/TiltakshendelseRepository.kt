package no.nav.pto.veilarbportefolje.tiltakshendelse

import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.database.PostgresTable.TILTAKSHENDELSE
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse
import no.nav.pto.veilarbportefolje.tiltakshendelse.dto.input.KafkaTiltakshendelse
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class TiltakshendelseRepository(private val db: JdbcTemplate) {
    @Transactional
    fun tryLagreTiltakshendelseData(tiltakshendelseData: KafkaTiltakshendelse): Boolean {
        return upsertTiltakshendelse(tiltakshendelseData)
    }

    fun upsertTiltakshendelse(tiltakshendelse: KafkaTiltakshendelse): Boolean {
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
            return true
        } catch (e: Exception) {
            secureLog.error(e.message, e)
            return false
        }
    }

    fun hentAlleTiltakshendelser(): List<Tiltakshendelse> {
        val sql = "SELECT * FROM tiltakshendelse"

        try {
            return db.queryForList(sql).stream().map { rs: Map<String, Any> -> TiltakshendelseMapper.tiltakshendelseMapper(rs) }
                .toList()
        } catch (e: Exception) {
            secureLog.error(e.message, e)
            throw RuntimeException(e)
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

        try {
            return db.queryForObject(sql, TiltakshendelseMapper::tiltakshendelseMapper, fnr.toString())
        } catch (e: EmptyResultDataAccessException) {
            return null
        } catch (e: Error) {
            secureLog.error(e.message, e)
            throw RuntimeException(e)
        }
    }
}
