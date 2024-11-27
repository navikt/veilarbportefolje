package no.nav.pto.veilarbportefolje.hendelsesfilter

import no.nav.pto.veilarbportefolje.database.PostgresTable.HENDELSE.*
import no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
class HendelseRepository(
    @Autowired private val jdbcTemplate: JdbcTemplate
) {

    fun upsert(hendelse: Hendelse) {
        val (id, personIdent, avsender, kategori, _, hendelseInnhold) = hendelse
        val (navn, dato, lenke, detaljer) = hendelseInnhold

        // language=postgresql
        val sql = """
            INSERT INTO $TABLE_NAME (
                $ID,
                $PERSON_IDENT,
                $HENDELSE_NAVN,
                $HENDELSE_DATO,
                $HENDELSE_LENKE,
                $HENDELSE_DETALJER,
                $KATEGORI,
                $AVSENDER,
                $OPPRETTET,
                $SIST_ENDRET
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT ($ID) DO UPDATE SET(
                $PERSON_IDENT,
                $HENDELSE_NAVN,
                $HENDELSE_DATO,
                $HENDELSE_LENKE,
                $HENDELSE_DETALJER,
                $KATEGORI,
                $AVSENDER,
                $SIST_ENDRET
            ) = (?, ?, ?, ?, ?, ?, ?, ?)
            """

        jdbcTemplate.update(
            sql,
            id, personIdent, navn, dato, lenke, detaljer, kategori, avsender, toTimestamp(LocalDateTime.now()), toTimestamp(LocalDateTime.now()),
            personIdent, navn, dato, lenke, detaljer, kategori, avsender, toTimestamp(LocalDateTime.now()),
        )
    }

    fun delete(id: UUID) {
        val sql = "DELETE FROM $TABLE_NAME WHERE $ID = ?"
        jdbcTemplate.update(sql, id)
    }
}

