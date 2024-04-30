package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.pto.veilarbportefolje.database.PostgresTable.SISTE_ARBEIDSSOEKER_PERIODE
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
        val opplysningerOmArbeidssoekerObjekt = OpplysningerOmArbeidssoekerResponse(
            opplysningerOmArbeidssoekerId = UUID.fromString("913161a3-dde9-4448-abf8-2a01a043f8cd"),
            periodeId = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16"),
            sendtInnAv = MetadataResponse(
                tidspunkt = ZonedDateTime.parse("2024-04-23T13:22:58.089Z"),
                utfoertAv = BrukerResponse(
                    type = BrukerType.SLUTTBRUKER
                ),
                kilde = "paw-arbeidssoekerregisteret-inngang",
                aarsak = "opplysning om arbeidssøker sendt inn"
            ),
            jobbsituasjon = listOf(
                BeskrivelseMedDetaljerResponse(
                    beskrivelse = JobbSituasjonBeskrivelse.ER_PERMITTERT,
                    detaljer = mapOf(Pair("prosent", "25"))
                ),
                BeskrivelseMedDetaljerResponse(
                    beskrivelse = JobbSituasjonBeskrivelse.MIDLERTIDIG_JOBB,
                    detaljer = mapOf(Pair("prosent", "75"))
                )
            ),
            utdanning = UtdanningResponse(
                nus = "3",
                bestaatt = JaNeiVetIkke.JA,
                godkjent = JaNeiVetIkke.JA
            ),
            helse = HelseResponse(
                helsetilstandHindrerArbeid = JaNeiVetIkke.NEI
            ),
            annet = AnnetResponse(
                andreForholdHindrerArbeid = JaNeiVetIkke.NEI
            )
        ).toOpplysningerOmArbeidssoeker()
        opplysningerOmArbeidssoeker.insertOpplysningerOmArbeidssoekerOgJobbsituasjon(opplysningerOmArbeidssoekerObjekt)


        val resultatOpplysninger: OpplysningerOmArbeidssoeker? =
            TestDataClient.getOpplysningerOmArbeidssoekerFraDb(db, opplysningerOmArbeidssoekerObjekt.periodeId)

        val resultatJobbsituasjon: OpplysningerOmArbeidssoekerJobbsituasjon? =
            TestDataClient.getOpplysningerOmArbeidssoekerJobbsituasjonFraDb(
                db,
                resultatOpplysninger!!.opplysningerOmArbeidssoekerId
            )

        assertThat(resultatOpplysninger.opplysningerOmArbeidssoekerId).isEqualTo(opplysningerOmArbeidssoekerObjekt.opplysningerOmArbeidssoekerId)
        assertThat(resultatJobbsituasjon?.jobbsituasjon?.size).isEqualTo(opplysningerOmArbeidssoekerObjekt.opplysningerOmJobbsituasjon.jobbsituasjon.size)
    }
}

