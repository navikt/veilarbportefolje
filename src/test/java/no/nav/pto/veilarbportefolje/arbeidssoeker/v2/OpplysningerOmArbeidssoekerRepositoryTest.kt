package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.pto.veilarbportefolje.database.PostgresTable.SISTE_ARBEIDSSOEKER_PERIODE
import no.nav.pto.veilarbportefolje.util.DateUtils
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer
import no.nav.pto.veilarbportefolje.util.TestDataClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import java.time.ZonedDateTime
import java.util.*

class OpplysningerOmArbeidssoekerRepositoryTest {
    private val db: JdbcTemplate = SingletonPostgresContainer.init().createJdbcTemplate()
    private val opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoekerRepository =
        OpplysningerOmArbeidssoekerRepository(db)

    @BeforeEach
    fun reset() {
        db.update("""TRUNCATE ${SISTE_ARBEIDSSOEKER_PERIODE.TABLE_NAME} CASCADE""")
    }

    @Test
    fun upsertOpplysningerOmArbeidssoeker() {
        db.update(
            """INSERT INTO ${SISTE_ARBEIDSSOEKER_PERIODE.TABLE_NAME} VALUES (?,?)""",
            UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16"),
            "12345671231"
        )
        val opplysningerOmArbeidssoekerId = UUID.fromString("913161a3-dde9-4448-abf8-2a01a043f8cd")
        val opplysningerOmArbeidssoekerObjekt = OpplysningerOmArbeidssoekerEntity(
            opplysningerOmArbeidssoekerId = opplysningerOmArbeidssoekerId,
            periodeId = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16"),
            sendtInnTidspunkt = DateUtils.toTimestamp(ZonedDateTime.parse("2024-04-23T13:22:58.089Z")),
            utdanningNusKode = "3",
            utdanningBestatt = "JA",
            utdanningGodkjent = "JA",
            opplysningerOmJobbsituasjon = OpplysningerOmArbeidssoekerJobbsituasjonEntity(
                opplysningerOmArbeidssoekerId,
                listOf(
                    JobbSituasjonBeskrivelseResponse.ER_PERMITTERT.name,
                    JobbSituasjonBeskrivelseResponse.MIDLERTIDIG_JOBB.name
                )
            )
        )
        opplysningerOmArbeidssoeker.insertOpplysningerOmArbeidssoekerOgJobbsituasjon(opplysningerOmArbeidssoekerObjekt)


        val resultatOpplysninger: OpplysningerOmArbeidssoekerEntity? =
            TestDataClient.getOpplysningerOmArbeidssoekerFraDb(db, opplysningerOmArbeidssoekerObjekt.periodeId)

        val resultatJobbsituasjon: OpplysningerOmArbeidssoekerJobbsituasjonEntity? =
            TestDataClient.getOpplysningerOmArbeidssoekerJobbsituasjonFraDb(
                db,
                resultatOpplysninger!!.opplysningerOmArbeidssoekerId
            )

        assertThat(resultatOpplysninger.opplysningerOmArbeidssoekerId).isEqualTo(opplysningerOmArbeidssoekerObjekt.opplysningerOmArbeidssoekerId)
        assertThat(resultatJobbsituasjon?.jobbsituasjon?.size).isEqualTo(opplysningerOmArbeidssoekerObjekt.opplysningerOmJobbsituasjon.jobbsituasjon.size)
    }
}

