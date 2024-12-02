package no.nav.pto.veilarbportefolje.hendelsesfilter

import Hendelse
import Kategori
import no.nav.common.types.identer.NorskIdent
import no.nav.pto.veilarbportefolje.database.PostgresTable.HENDELSE
import no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp
import no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.net.URI
import java.sql.ResultSet
import java.time.ZonedDateTime
import java.util.*

@Repository
class HendelseRepository(
    @Autowired private val jdbcTemplate: JdbcTemplate
) {

    /**
     * Henter en hendelse:
     *
     * * dersom en hendelse med ID [id] eksisterer returneres denne
     * * dersom ingen hendelse med ID [id] eksisterer kastes en [IngenHendelseMedIdException]
     */
    fun get(id: UUID): Hendelse {
        // language=postgresql
        val sql = "SELECT * FROM ${HENDELSE.TABLE_NAME} WHERE ${HENDELSE.ID} = ?"
        val resultat = try {
            jdbcTemplate.queryForObject(sql, ::toHendelse, id)
        } catch (ex: EmptyResultDataAccessException) {
            throw IngenHendelseMedIdException(id = id.toString(), cause = ex)
        }

        return resultat
            ?: throw RuntimeException("Ukjent feil ved henting av hendelse med ID $id. Forventet å få en instans av ${Hendelse::class.simpleName} men fikk null.")
    }

    /**
     * Henter den eldste hendelsen:
     *
     * * dersom minst en hendelse eksisterer for [personIdent] returneres den eldste av disse
     * * dersom ingen hendelser eksisterer for [personIdent] kastes en [IngenHendelseForPersonException]
     */
    fun getEldste(personIdent: NorskIdent): Hendelse {
        // language=postgresql
        val sql = """
            SELECT * FROM ${HENDELSE.TABLE_NAME} WHERE ${HENDELSE.PERSON_IDENT} = ?
            ORDER BY ${HENDELSE.HENDELSE_DATO} LIMIT 1
            """.trimIndent()

        val resultat = try {
            jdbcTemplate.queryForObject(sql, ::toHendelse, personIdent.get())
        } catch (ex: EmptyResultDataAccessException) {
            throw IngenHendelseForPersonException(cause = ex)
        }

        return resultat
            ?: throw RuntimeException("Ukjent feil ved henting av hendelse for person. Forventet å få en instans av ${Hendelse::class.simpleName} men fikk null.")
    }

    /**
     * Lagre en hendelse:
     *
     * * dersom ingen hendelse med samme [Hendelse.id] eksisterer fra før lagres hendelsen
     * * dersom en hendelse med samme [Hendelse.id] eksisterer fra før kastes [HendelseIdEksistererAlleredeException].
     */
    fun insert(hendelse: Hendelse) {
        val (id, personIdent, avsender, kategori, hendelseInnhold) = hendelse
        val (navn, dato, lenke, detaljer) = hendelseInnhold

        // language=postgresql
        val sql = """
            INSERT INTO ${HENDELSE.TABLE_NAME} (
                ${HENDELSE.ID},
                ${HENDELSE.PERSON_IDENT},
                ${HENDELSE.HENDELSE_NAVN},
                ${HENDELSE.HENDELSE_DATO},
                ${HENDELSE.HENDELSE_LENKE},
                ${HENDELSE.HENDELSE_DETALJER},
                ${HENDELSE.KATEGORI},
                ${HENDELSE.AVSENDER},
                ${HENDELSE.OPPRETTET},
                ${HENDELSE.SIST_ENDRET}
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (${HENDELSE.ID}) DO NOTHING
            """

        val timestampAkkuratNaa = toTimestamp(ZonedDateTime.now())
        val affectedRows = jdbcTemplate.update(
            sql,
            id,
            personIdent.get(),
            navn,
            toTimestamp(dato),
            lenke.toString(),
            detaljer,
            kategori.name,
            avsender,
            timestampAkkuratNaa,
            timestampAkkuratNaa,
        )

        if (affectedRows == 0) {
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
        val (id, personIdent, avsender, kategori, hendelseInnhold) = hendelse
        val (navn, dato, lenke, detaljer) = hendelseInnhold

        // language=postgresql
        val sql = """
            UPDATE ${HENDELSE.TABLE_NAME} SET (
                ${HENDELSE.PERSON_IDENT},
                ${HENDELSE.HENDELSE_NAVN},
                ${HENDELSE.HENDELSE_DATO},
                ${HENDELSE.HENDELSE_LENKE},
                ${HENDELSE.HENDELSE_DETALJER},
                ${HENDELSE.KATEGORI},
                ${HENDELSE.AVSENDER},
                ${HENDELSE.SIST_ENDRET}
            ) = (?, ?, ?, ?, ?, ?, ?, ?)
            WHERE ${HENDELSE.ID} = ?
            """

        val affectedRows = jdbcTemplate.update(
            sql,
            personIdent.get(),
            navn,
            toTimestamp(dato),
            lenke.toString(),
            detaljer,
            kategori.name,
            avsender,
            toTimestamp(ZonedDateTime.now()),
            id
        )

        if (affectedRows == 0) {
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
        val sql = "DELETE FROM ${HENDELSE.TABLE_NAME} WHERE ${HENDELSE.ID} = ?"
        val affectedRows = jdbcTemplate.update(sql, id)

        if (affectedRows == 0) {
            throw IngenHendelseMedIdException(id.toString())
        }
    }

}

private fun toHendelse(resultSet: ResultSet, affectedRows: Int): Hendelse {
    return Hendelse(
        id = UUID.fromString(resultSet.getString(HENDELSE.ID)),
        personIdent = NorskIdent(resultSet.getString(HENDELSE.PERSON_IDENT)),
        avsender = resultSet.getString(HENDELSE.AVSENDER),
        kategori = Kategori.valueOf(resultSet.getString(HENDELSE.KATEGORI)),
        hendelse = Hendelse.HendelseInnhold(
            beskrivelse = resultSet.getString(HENDELSE.HENDELSE_NAVN),
            dato = toZonedDateTime(resultSet.getTimestamp(HENDELSE.HENDELSE_DATO)),
            lenke = URI.create(resultSet.getString(HENDELSE.HENDELSE_LENKE)).toURL(),
            detaljer = resultSet.getString(HENDELSE.HENDELSE_DETALJER),
        )
    )
}

data class HendelseIdEksistererAlleredeException(override val message: String) : RuntimeException(message)
data class IngenHendelseMedIdException(
    val id: String,
    override val message: String = "Fant ingen hendelse med id $id.",
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

data class IngenHendelseForPersonException(
    override val message: String = "Fant ingen hendelse for personen.",
    override val cause: Throwable
) : RuntimeException(message)