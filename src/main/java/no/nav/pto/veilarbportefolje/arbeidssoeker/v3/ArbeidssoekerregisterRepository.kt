package no.nav.pto.veilarbportefolje.arbeidssoeker.v3

import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.arbeidssoeker.v3.domene.AggregertPeriodeEntity
import no.nav.pto.veilarbportefolje.arbeidssoeker.v3.domene.ArbeidssoekerregisterDataEntity
import no.nav.pto.veilarbportefolje.database.PostgresTable.ARBEIDSSOEKERREGISTER_DATA
import no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp
import no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime
import org.jetbrains.annotations.TestOnly
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Types
import java.time.ZonedDateTime
import java.util.*

@Repository
class ArbeidssoekerregisterRepository(private val db: JdbcTemplate) {
//    fun hentOpplysningerOmArbeidssoeker(fnrs: List<Fnr>): Map<String, OpplysningerOmArbeidssoeker> {
//
//    }
//
//    fun hentProfileringsresultat(fnrs: List<Fnr>): Map<String, Profilering> {
//
//    }
//
//    fun hentPeriode(fnrs: List<Fnr>): Map<String, Periode> {
//
//    }

    fun lagreSisteArbeidssoekerPeriode(fnr: Fnr, periode: AggregertPeriodeEntity) {
        val sql = """
            INSERT INTO ${ARBEIDSSOEKERREGISTER_DATA.TABLE_NAME} (
                ${ARBEIDSSOEKERREGISTER_DATA.IDENTITETSNUMMER},
                ${ARBEIDSSOEKERREGISTER_DATA.PERIODE_ID},
                ${ARBEIDSSOEKERREGISTER_DATA.PERIODE_START_TIDSPUNKT},
                ${ARBEIDSSOEKERREGISTER_DATA.PERIODE_AVSLUTTET_TIDSPUNKT},
                ${ARBEIDSSOEKERREGISTER_DATA.PERIODE_RAD_SIST_ENDRET},
                ${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_ID},
                ${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_JOBBSITUASJON_BESKRIVELSE},
                ${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_UTDANNING_NUS_KODE},
                ${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_UTDANNING_BESTATT},
                ${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_UTDANNING_GODKJENT},
                ${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_SENDT_INN_TIDSPUNKT},
                ${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_RAD_SIST_ENDRET},
                ${ARBEIDSSOEKERREGISTER_DATA.PROFILERING_ID},
                ${ARBEIDSSOEKERREGISTER_DATA.PROFILERING_RESULTAT},
                ${ARBEIDSSOEKERREGISTER_DATA.PROFILERING_SENDT_INN_TIDSPUNKT},
                ${ARBEIDSSOEKERREGISTER_DATA.PROFILERING_RAD_SIST_ENDRET},
                ${ARBEIDSSOEKERREGISTER_DATA.SYNKRONISERING_STATUS},
                ${ARBEIDSSOEKERREGISTER_DATA.RAD_OPPRETTET}
            )
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (${ARBEIDSSOEKERREGISTER_DATA.IDENTITETSNUMMER}) DO UPDATE SET
                ${ARBEIDSSOEKERREGISTER_DATA.PERIODE_ID} = excluded.${ARBEIDSSOEKERREGISTER_DATA.PERIODE_ID},
                ${ARBEIDSSOEKERREGISTER_DATA.PERIODE_START_TIDSPUNKT} = excluded.${ARBEIDSSOEKERREGISTER_DATA.PERIODE_START_TIDSPUNKT},
                ${ARBEIDSSOEKERREGISTER_DATA.PERIODE_AVSLUTTET_TIDSPUNKT} = excluded.${ARBEIDSSOEKERREGISTER_DATA.PERIODE_AVSLUTTET_TIDSPUNKT},
                ${ARBEIDSSOEKERREGISTER_DATA.PERIODE_RAD_SIST_ENDRET} = excluded.${ARBEIDSSOEKERREGISTER_DATA.PERIODE_RAD_SIST_ENDRET},
                ${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_ID} = excluded.${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_ID},
                ${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_JOBBSITUASJON_BESKRIVELSE} = excluded.${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_JOBBSITUASJON_BESKRIVELSE},
                ${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_UTDANNING_NUS_KODE} = excluded.${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_UTDANNING_NUS_KODE},
                ${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_UTDANNING_BESTATT} = excluded.${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_UTDANNING_BESTATT},
                ${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_UTDANNING_GODKJENT} = excluded.${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_UTDANNING_GODKJENT},
                ${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_SENDT_INN_TIDSPUNKT} = excluded.${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_SENDT_INN_TIDSPUNKT},
                ${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_RAD_SIST_ENDRET} = excluded.${ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_RAD_SIST_ENDRET},
                ${ARBEIDSSOEKERREGISTER_DATA.PROFILERING_ID} = excluded.${ARBEIDSSOEKERREGISTER_DATA.PROFILERING_ID},
                ${ARBEIDSSOEKERREGISTER_DATA.PROFILERING_RESULTAT} = excluded.${ARBEIDSSOEKERREGISTER_DATA.PROFILERING_RESULTAT},
                ${ARBEIDSSOEKERREGISTER_DATA.PROFILERING_SENDT_INN_TIDSPUNKT} = excluded.${ARBEIDSSOEKERREGISTER_DATA.PROFILERING_SENDT_INN_TIDSPUNKT},
                ${ARBEIDSSOEKERREGISTER_DATA.PROFILERING_RAD_SIST_ENDRET} = excluded.${ARBEIDSSOEKERREGISTER_DATA.PROFILERING_RAD_SIST_ENDRET},
                ${ARBEIDSSOEKERREGISTER_DATA.SYNKRONISERING_STATUS} = excluded.${ARBEIDSSOEKERREGISTER_DATA.SYNKRONISERING_STATUS},
                ${ARBEIDSSOEKERREGISTER_DATA.RAD_SIST_ENDRET} = CURRENT_TIMESTAMP
        """.trimIndent()

        db.update(sql) { ps ->
            ps.setString(1, fnr.get())
            ps.setObject(2, periode.periodeId)
            ps.setTimestamp(3, toTimestamp(periode.periodeStartTidspunkt))
            ps.setTimestamp(4, toTimestamp(periode.periodeAvsluttetTidspunkt))
            ps.setObject(5, periode.opplysningId)
            ps.setArray(
                6,
                periode.opplysningJobbsituasjonBeskrivelse?.let {
                    ps.connection.createArrayOf("text", it.toTypedArray())
                }
            )
            ps.setObject(7, periode.opplysningUtdanningNusKode, Types.INTEGER)
            ps.setString(8, periode.opplysningUtdanningBestatt)
            ps.setString(9, periode.opplysningUtdanningGodkjent)
            ps.setTimestamp(10, toTimestamp(periode.opplysningSendtInnTidspunkt))
            ps.setObject(11, periode.profileringId)
            ps.setString(12, periode.profileringResultat)
            ps.setTimestamp(13, toTimestamp(periode.profileringSendtInnTidspunkt))
            ps.setString(14, SynkroniseringStatus.SYNKRONISERT.name)
        }
    }

    fun lagreBrukerMedSynkroniseringStatusUsikker(fnr: Fnr) {
        val sql = """
            INSERT INTO ${ARBEIDSSOEKERREGISTER_DATA.TABLE_NAME} (${ARBEIDSSOEKERREGISTER_DATA.IDENTITETSNUMMER}, ${ARBEIDSSOEKERREGISTER_DATA.SYNKRONISERING_STATUS}, ${ARBEIDSSOEKERREGISTER_DATA.RAD_OPPRETTET})
            VALUES (?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (${ARBEIDSSOEKERREGISTER_DATA.IDENTITETSNUMMER}) DO UPDATE SET
                ${ARBEIDSSOEKERREGISTER_DATA.SYNKRONISERING_STATUS} = excluded.${ARBEIDSSOEKERREGISTER_DATA.SYNKRONISERING_STATUS},
                ${ARBEIDSSOEKERREGISTER_DATA.RAD_SIST_ENDRET} = CURRENT_TIMESTAMP
        """.trimIndent()

        db.update(sql, fnr.get(), SynkroniseringStatus.USIKKERT_OM_BRUKER_HAR_DATA.name)
    }

    @TestOnly
    fun hentArbeidssoekerregisterData(fnr: Fnr): ArbeidssoekerregisterDataEntity? {
        val sql = "SELECT * FROM ${ARBEIDSSOEKERREGISTER_DATA.TABLE_NAME} WHERE ${ARBEIDSSOEKERREGISTER_DATA.IDENTITETSNUMMER} = ?"
        return try {
            db.queryForObject(sql, { rs, _ -> rs.toArbeidssoekerregisterDataEntity() }, fnr.get())
        } catch (ex: EmptyResultDataAccessException) {
            null
        }
    }

    fun hentArbeidssoekerregisterData(fnrs: List<Fnr>): List<ArbeidssoekerregisterDataEntity> {
        if (fnrs.isEmpty()) {
            return emptyList()
        }
        val sql = "SELECT * FROM ${ARBEIDSSOEKERREGISTER_DATA.TABLE_NAME} WHERE ${ARBEIDSSOEKERREGISTER_DATA.IDENTITETSNUMMER} = ANY (?::varchar[])"
        return db.query(
            sql,
            { rs, _ -> rs.toArbeidssoekerregisterDataEntity() },
            fnrs.joinToString(",", "{", "}") { it.get() }
        )
    }

    fun slettArbeidssoekerregisterData(fnrs: List<Fnr>): Int {
        if (fnrs.isEmpty()) {
            return 0
        }
        val sql = "DELETE FROM ${ARBEIDSSOEKERREGISTER_DATA.TABLE_NAME} WHERE ${ARBEIDSSOEKERREGISTER_DATA.IDENTITETSNUMMER} = ANY (?::varchar[])"
        return db.update(sql, fnrs.joinToString(",", "{", "}") { it.get() })
    }

    private fun ResultSet.toArbeidssoekerregisterDataEntity(): ArbeidssoekerregisterDataEntity {
        val periodeId = getObject(ARBEIDSSOEKERREGISTER_DATA.PERIODE_ID, UUID::class.java)
        return ArbeidssoekerregisterDataEntity(
            identitetsnummer = getString(ARBEIDSSOEKERREGISTER_DATA.IDENTITETSNUMMER),
            aggregertPeriode = periodeId?.let {
                AggregertPeriodeEntity(
                    identitetsnummer = getString(ARBEIDSSOEKERREGISTER_DATA.IDENTITETSNUMMER),
                    periodeId = it,
                    periodeStartTidspunkt = getZonedDateTime(ARBEIDSSOEKERREGISTER_DATA.PERIODE_START_TIDSPUNKT)!!,
                    periodeAvsluttetTidspunkt = getZonedDateTime(ARBEIDSSOEKERREGISTER_DATA.PERIODE_AVSLUTTET_TIDSPUNKT),
                    opplysningId = getObject(ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_ID, UUID::class.java),
                    opplysningJobbsituasjonBeskrivelse = getArray(ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_JOBBSITUASJON_BESKRIVELSE)
                        ?.let { array -> (array.array as Array<*>).map { beskrivelse -> beskrivelse as String } },
                    opplysningUtdanningNusKode = getObject(ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_UTDANNING_NUS_KODE, Int::class.javaObjectType),
                    opplysningUtdanningBestatt = getString(ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_UTDANNING_BESTATT),
                    opplysningUtdanningGodkjent = getString(ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_UTDANNING_GODKJENT),
                    opplysningSendtInnTidspunkt = getZonedDateTime(ARBEIDSSOEKERREGISTER_DATA.OPPLYSNING_SENDT_INN_TIDSPUNKT),
                    profileringId = getObject(ARBEIDSSOEKERREGISTER_DATA.PROFILERING_ID, UUID::class.java),
                    profileringResultat = getString(ARBEIDSSOEKERREGISTER_DATA.PROFILERING_RESULTAT),
                    profileringSendtInnTidspunkt = getZonedDateTime(ARBEIDSSOEKERREGISTER_DATA.PROFILERING_SENDT_INN_TIDSPUNKT)
                )
            },
            synkroniseringStatus = SynkroniseringStatus.valueOf(getString(ARBEIDSSOEKERREGISTER_DATA.SYNKRONISERING_STATUS)),
            radOpprettet = getZonedDateTime(ARBEIDSSOEKERREGISTER_DATA.RAD_OPPRETTET)!!
        )
    }

    private fun ResultSet.getZonedDateTime(kolonne: String): ZonedDateTime? =
        getTimestamp(kolonne)?.let { toZonedDateTime(it.toInstant()) }
}

enum class SynkroniseringStatus {
    SYNKRONISERT,
    USIKKERT_OM_BRUKER_HAR_DATA,
    API_UTE_AV_SYNC
}
