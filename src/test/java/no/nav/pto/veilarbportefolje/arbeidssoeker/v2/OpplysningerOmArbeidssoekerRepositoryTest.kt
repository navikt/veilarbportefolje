package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.pto.veilarbportefolje.database.PostgresTable.OPPLYSNINGER_OM_ARBEIDSSOEKER
import no.nav.pto.veilarbportefolje.database.PostgresTable.OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON
import no.nav.pto.veilarbportefolje.util.DateUtils
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.ResultSet
import java.time.ZonedDateTime
import java.util.*

class OpplysningerOmArbeidssoekerRepositoryTest {
    private val db: JdbcTemplate = SingletonPostgresContainer.init().createJdbcTemplate()
    private val opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoekerRepository =
        OpplysningerOmArbeidssoekerRepository(db)

    @Test
    fun upsertOpplysningerOmArbeidssoeker() {
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
        opplysningerOmArbeidssoeker.upsertOpplysningerOmArbeidssoeker(opplysningerOmArbeidssoekerObjekt)


        val resultatOpplysninger: OpplysningerOmArbeidssoeker? = db.queryForObject(
            """SELECT * FROM ${OPPLYSNINGER_OM_ARBEIDSSOEKER.TABLE_NAME} WHERE ${OPPLYSNINGER_OM_ARBEIDSSOEKER.PERIODE_ID} =?""",
            { rs: ResultSet, _ ->
                val opplysningerOmArbeidssoekerId =
                    rs.getObject(OPPLYSNINGER_OM_ARBEIDSSOEKER.OPPLYSNINGER_OM_ARBEIDSSOEKER_ID, UUID::class.java)
                OpplysningerOmArbeidssoeker(
                    opplysningerOmArbeidssoekerId,
                    rs.getObject(OPPLYSNINGER_OM_ARBEIDSSOEKER.PERIODE_ID, UUID::class.java),
                    DateUtils.toZonedDateTime(rs.getTimestamp(OPPLYSNINGER_OM_ARBEIDSSOEKER.SENDT_INN_TIDSPUNKT)),
                    rs.getString(OPPLYSNINGER_OM_ARBEIDSSOEKER.UTDANNING_NUS_KODE),
                    rs.getString(OPPLYSNINGER_OM_ARBEIDSSOEKER.UTDANNING_BESTATT),
                    rs.getString(OPPLYSNINGER_OM_ARBEIDSSOEKER.UTDANNING_GODKJENT),
                    OpplysningerOmArbeidssoekerJobbsituasjon(opplysningerOmArbeidssoekerId, emptyList())
                )
            },
            opplysningerOmArbeidssoekerObjekt.periodeId
        )

        val resultatJobbsituasjon: List<OpplysningerOmArbeidssoekerJobbsituasjonTest> = db.queryForList(
            """SELECT * FROM ${OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON.TABLE_NAME} WHERE ${OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON.OPPLYSNINGER_OM_ARBEIDSSOEKER_ID} =?""",
            opplysningerOmArbeidssoekerObjekt.opplysningerOmArbeidssoekerId
        ).map { rs ->
                OpplysningerOmArbeidssoekerJobbsituasjonTest(
                    rs[OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON.OPPLYSNINGER_OM_ARBEIDSSOEKER_ID] as UUID,
                    rs[OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON.JOBBSITUASJON] as String
                )
        }

        assertThat(resultatOpplysninger?.opplysningerOmArbeidssoekerId).isEqualTo(opplysningerOmArbeidssoekerObjekt.opplysningerOmArbeidssoekerId)
        assertThat(resultatJobbsituasjon.size).isEqualTo(opplysningerOmArbeidssoekerObjekt.opplysningerOmJobbsituasjon.jobbsituasjon.size)
    }
}

data class OpplysningerOmArbeidssoekerJobbsituasjonTest(
    val opplysningerOmArbeidssoekerId: UUID,
    val jobbsituasjon: String
)