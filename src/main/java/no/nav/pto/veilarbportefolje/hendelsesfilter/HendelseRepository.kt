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

    /**
     * Lagre en hendelse:
     *
     * * dersom ingen hendelse med samme [Hendelse.id] eksisterer fra før lagres hendelsen
     * * dersom en hendelse med samme [Hendelse.id] eksisterer fra før kastes [HendelseIdEksistererAlleredeException].
     */
    fun insert(hendelse: Hendelse) {
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
            ON CONFLICT ($ID) DO NOTHING
            """

        val affectedRows = jdbcTemplate.update(
            sql,
            id,
            personIdent,
            navn,
            dato,
            lenke,
            detaljer,
            kategori,
            avsender,
            toTimestamp(LocalDateTime.now()),
            toTimestamp(LocalDateTime.now()),
        )

        if(affectedRows == 0) {
            throw HendelseIdEksistererAlleredeException("Hendelse med id $id eksisterer allerede.")
        }
    }

    /**
     * Oppdater en hendelse:
     *
     * * dersom en hendelse med samme [Hendelse.id] eksisterer fra før oppdateres denne med data fra [hendelse]
     * * dersom ingen hendelse med samme [Hendelse.id] eksisterer fra før kastes [IngenHendelseMedIdException]
     */
    fun update(hendelse: Hendelse) {
        val (id, personIdent, avsender, kategori, _, hendelseInnhold) = hendelse
        val (navn, dato, lenke, detaljer) = hendelseInnhold

        // language=postgresql
        val sql = """
            UPDATE $TABLE_NAME SET (
                $PERSON_IDENT,
                $HENDELSE_NAVN,
                $HENDELSE_DATO,
                $HENDELSE_LENKE,
                $HENDELSE_DETALJER,
                $KATEGORI,
                $AVSENDER,
                $SIST_ENDRET
            ) = (?, ?, ?, ?, ?, ?, ?, ?)
            WHERE $ID = ?
            """

        val affectedRows = jdbcTemplate.update(
            sql,
            personIdent,
            navn,
            dato,
            lenke,
            detaljer,
            kategori,
            avsender,
            toTimestamp(LocalDateTime.now()),
            id
        )

        if(affectedRows == 0) {
            throw IngenHendelseMedIdException(id.toString())
        }
    }

    /**
     * Slett en hendelse:
     *
     * * dersom en hendelse eksisterer med ID lik [id] slettes hendelsen
     * * dersom ingen hendelse eksisterer med ID lik [id] kastes [IngenHendelseMedIdException]
     */
    fun delete(id: UUID) {
        val sql = "DELETE FROM $TABLE_NAME WHERE $ID = ?"
        val affectedRows = jdbcTemplate.update(sql, id)

        if(affectedRows == 0) {
            throw IngenHendelseMedIdException(id.toString())
        }
    }
}

data class HendelseIdEksistererAlleredeException(override val message: String) : RuntimeException(message)
data class IngenHendelseMedIdException(val id: String, override val message: String = "Fant ingen hendelse med id $id.") : RuntimeException(message)