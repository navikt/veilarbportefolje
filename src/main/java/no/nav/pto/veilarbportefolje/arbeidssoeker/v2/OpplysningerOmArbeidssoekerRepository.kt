package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.pto.veilarbportefolje.database.PostgresTable.OPPLYSNINGER_OM_ARBEIDSSOEKER
import no.nav.pto.veilarbportefolje.database.PostgresTable.OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON
import no.nav.pto.veilarbportefolje.util.DateUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class OpplysningerOmArbeidssoekerRepository(
    private val db: JdbcTemplate
) {
    @Transactional
    fun upsertOpplysningerOmArbeidssoeker(opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoeker) {
        insertOpplysningerOmArbeidssoeker(opplysningerOmArbeidssoeker)
        insertOpplysningerOmArbeidssoekerJobbsituasjon(opplysningerOmArbeidssoeker.jobbsituasjon)
    }

    fun insertOpplysningerOmArbeidssoeker(opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoeker) {
        val sqlString = """INSERT INTO ${OPPLYSNINGER_OM_ARBEIDSSOEKER.TABLE_NAME} ( 
                    ${OPPLYSNINGER_OM_ARBEIDSSOEKER.OPPLYSNINGER_OM_ARBEIDSSOEKER_ID}, 
                    ${OPPLYSNINGER_OM_ARBEIDSSOEKER.PERIODE_ID},
                    ${OPPLYSNINGER_OM_ARBEIDSSOEKER.SENDT_INN_TIDSPUNKT},
                    ${OPPLYSNINGER_OM_ARBEIDSSOEKER.UTDANNING_NUS_KODE},
                    ${OPPLYSNINGER_OM_ARBEIDSSOEKER.UTDANNING_BESTATT},
                    ${OPPLYSNINGER_OM_ARBEIDSSOEKER.UTDANNING_GODKJENT}
                )
                VALUES (?, ?, ?, ?, ?, ?)"""
        db.update(
            sqlString,
            opplysningerOmArbeidssoeker.opplysningerOmArbeidssoekerId,
            opplysningerOmArbeidssoeker.periodeId,
            DateUtils.toTimestamp(opplysningerOmArbeidssoeker.sendtInnTidspunkt),
            opplysningerOmArbeidssoeker.utdanningNusKode,
            opplysningerOmArbeidssoeker.utdanningBestatt,
            opplysningerOmArbeidssoeker.utdanningGodkjent
        )
    }

    fun insertOpplysningerOmArbeidssoekerJobbsituasjon(opplysningerOmArbeidssoekerJobbsituasjon: OpplysningerOmArbeidssoekerJobbsituasjon) {
        val sqlString = """INSERT INTO ${OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON.TABLE_NAME} ( 
                    ${OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON.OPPLYSNINGER_OM_ARBEIDSSOEKER_ID}, 
                    ${OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON.JOBBSITUASJON}
                )
                VALUES (?, ?)"""
        db.update(
            sqlString,
            opplysningerOmArbeidssoekerJobbsituasjon.opplysningerOmArbeidssoekerId,
            opplysningerOmArbeidssoekerJobbsituasjon.jobbsituasjon
        )
    }
}