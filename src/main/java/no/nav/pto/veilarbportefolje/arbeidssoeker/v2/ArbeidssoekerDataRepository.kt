package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.database.PostgresTable.OPPLYSNINGER_OM_ARBEIDSSOEKER
import no.nav.pto.veilarbportefolje.database.PostgresTable.OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON
import no.nav.pto.veilarbportefolje.database.PostgresTable.SISTE_ARBEIDSSOEKER_PERIODE
import no.nav.pto.veilarbportefolje.database.PostgresTable.PROFILERING
import no.nav.pto.veilarbportefolje.util.DateUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ArbeidssoekerDataRepository(
    @Qualifier("PostgresJdbcReadOnly") private val db: JdbcTemplate
) {

    fun hentOpplysningerOmArbeidssoeker(fnrs: List<Fnr>): Map<String, OpplysningerOmArbeidssoeker> {
        val arbeidssoekerDataPaaBruker = mutableMapOf<String, OpplysningerOmArbeidssoeker>()


        val fnrsPostgresArray = "{${fnrs.joinToString(",") { it.get() }}}"
        val sqlString = """
            SELECT 
                sap.${SISTE_ARBEIDSSOEKER_PERIODE.FNR},
                ooa.*,
                ooaj.${OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON.JOBBSITUASJON} 
            FROM ${OPPLYSNINGER_OM_ARBEIDSSOEKER.TABLE_NAME} ooa
            INNER JOIN ${SISTE_ARBEIDSSOEKER_PERIODE.TABLE_NAME} sap ON ooa.${OPPLYSNINGER_OM_ARBEIDSSOEKER.PERIODE_ID} = sap.${SISTE_ARBEIDSSOEKER_PERIODE.ARBEIDSSOKER_PERIODE_ID}
            INNER JOIN ${OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON.TABLE_NAME} ooaj ON ooa.${OPPLYSNINGER_OM_ARBEIDSSOEKER.OPPLYSNINGER_OM_ARBEIDSSOEKER_ID} = ooaj.${OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON.OPPLYSNINGER_OM_ARBEIDSSOEKER_ID}
            WHERE sap.${SISTE_ARBEIDSSOEKER_PERIODE.FNR} = ANY ('$fnrsPostgresArray'::varchar[])
        """.trimIndent()

        db.query(sqlString) { rs, _ ->
            val fnr = rs.getString(SISTE_ARBEIDSSOEKER_PERIODE.FNR)
            val opplysningPaaBruker = arbeidssoekerDataPaaBruker[fnr]
            if (opplysningPaaBruker != null) {
                arbeidssoekerDataPaaBruker[fnr] = opplysningPaaBruker.copy(
                    jobbsituasjoner = opplysningPaaBruker.jobbsituasjoner + JobbSituasjonBeskrivelse.valueOf(
                        rs.getString(
                            OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON.JOBBSITUASJON
                        )
                    )
                )
            } else {
                val utdanningBestatt = rs.getString(OPPLYSNINGER_OM_ARBEIDSSOEKER.UTDANNING_BESTATT)
                val utdanningGodkjent = rs.getString(OPPLYSNINGER_OM_ARBEIDSSOEKER.UTDANNING_GODKJENT)
                arbeidssoekerDataPaaBruker[fnr] = OpplysningerOmArbeidssoeker(
                    sendtInnTidspunkt = DateUtils.toZonedDateTime(rs.getTimestamp(OPPLYSNINGER_OM_ARBEIDSSOEKER.SENDT_INN_TIDSPUNKT)),
                    utdanning = mapTilUtdanning(rs.getString(OPPLYSNINGER_OM_ARBEIDSSOEKER.UTDANNING_NUS_KODE)),
                    utdanningBestatt = if(utdanningBestatt != null) JaNeiVetIkke.valueOf(utdanningBestatt) else null,
                    utdanningGodkjent = if (utdanningGodkjent != null) JaNeiVetIkke.valueOf(utdanningGodkjent) else null,
                    jobbsituasjoner = listOf(
                        JobbSituasjonBeskrivelse.valueOf(
                            rs.getString(
                                OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON.JOBBSITUASJON
                            )
                        )
                    )
                )
            }
        }
        return arbeidssoekerDataPaaBruker.toMap()
    }

    fun hentProfileringsresultatListe(fnrs: List<Fnr>): Map<String, Profilering> {
        val profileringsresultatPaaBruker = mutableMapOf<String, Profilering>()

        val fnrsPostgresArray = "{${fnrs.joinToString(",") { it.get() }}}"
        val sqlString = """
            SELECT 
                sap.${SISTE_ARBEIDSSOEKER_PERIODE.FNR},
                p.${PROFILERING.PROFILERING_RESULTAT},
                p.${PROFILERING.SENDT_INN_TIDSPUNKT}
            FROM ${PROFILERING.TABLE_NAME} p
            INNER JOIN ${SISTE_ARBEIDSSOEKER_PERIODE.TABLE_NAME} sap ON p.${PROFILERING.PERIODE_ID} = sap.${SISTE_ARBEIDSSOEKER_PERIODE.ARBEIDSSOKER_PERIODE_ID}
            WHERE sap.${SISTE_ARBEIDSSOEKER_PERIODE.FNR} = ANY ('$fnrsPostgresArray'::varchar[])
        """.trimIndent()

        db.query(sqlString) { rs, _ ->
            val fnr = rs.getString(SISTE_ARBEIDSSOEKER_PERIODE.FNR)
            profileringsresultatPaaBruker[fnr] = Profilering(
                profileringsresultat = Profileringsresultat.valueOf(rs.getString(PROFILERING.PROFILERING_RESULTAT)),
                sendtInnTidspunkt = DateUtils.toZonedDateTime(rs.getTimestamp(PROFILERING.SENDT_INN_TIDSPUNKT))
            )
        }
        return profileringsresultatPaaBruker.toMap()

    }


}