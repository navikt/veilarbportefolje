package no.nav.pto.veilarbportefolje.aktiviteter.v1

import io.getunleash.DefaultUnleash
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.config.FeatureToggle
import no.nav.pto.veilarbportefolje.database.PostgresTable.KAFKA_AKTIVITET_MELDING.AKTIVITET_ID
import no.nav.pto.veilarbportefolje.database.PostgresTable.KAFKA_AKTIVITET_MELDING.TABLE_NAME
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.sql.Timestamp

@SpringBootTest(classes = [ApplicationConfigTest::class])
class PortefoljeAktivitetKafkaMeldingRepositoryTest(
    @param:Autowired private val jdbcTemplate: JdbcTemplate,
    @param:Autowired private val defaultUnleash: DefaultUnleash,
    @param:Autowired private val opensearchIndexer: OpensearchIndexer
) {
    private lateinit var repository: PortefoljeAktivitetKafkaMeldingRepository

    @BeforeEach
    fun setup() {
        repository = PortefoljeAktivitetKafkaMeldingRepository(
            NamedParameterJdbcTemplate(jdbcTemplate),
            opensearchIndexer,
            defaultUnleash
        )
        jdbcTemplate.update("TRUNCATE TABLE $TABLE_NAME")
        `when`(defaultUnleash.isEnabled(FeatureToggle.BRUK_TILTAKSAKTIVITET_FRA_AKTIVITETSPLAN)).thenReturn(false);
    }

    @Test
    fun `skal lagre ny aktivitet med kafka-metadata`() {
        val aktivitet = aktivitetEntity()

        val resultat = repository.tryLagreAktivitetDataBatch(listOf(aktivitet))
        val rad = hentRad(aktivitet.aktivitetId)

        assertThat(resultat.prosesserte).isEqualTo(1)
        assertThat(rad).isNotNull
        assertThat(rad!!["aktor_id"]).isEqualTo(aktivitet.aktorId)
        assertThat(rad["aktivitet_type"]).isEqualTo(aktivitet.aktivitetType)
        assertThat(rad["tiltakskode"]).isEqualTo(aktivitet.tiltakskode)
        assertThat(rad["record_offset"]).isEqualTo(aktivitet.recordOffset)
        assertThat(rad["record_partition"]).isEqualTo(aktivitet.recordPartition)
        assertThat(rad["record_key"]).isEqualTo(aktivitet.recordKey)
        assertThat(rad["rad_opprettet"]).isInstanceOf(Timestamp::class.java)
        assertThat(rad["rad_oppdatert"]).isInstanceOf(Timestamp::class.java)
    }

    @Test
    fun `skal oppdatere aktivitet naar version er nyere og oppdatere rad_oppdatert`() {
        val opprinnelig = aktivitetEntity(version = 1, aktivitetStatus = "PLANLAGT", recordOffset = 10)
        repository.tryLagreAktivitetDataBatch(listOf(opprinnelig))
        val radFoer = hentRad(opprinnelig.aktivitetId)!!
        val radOpprettetFoer = radFoer["rad_opprettet"] as Timestamp
        val radOppdatertFoer = radFoer["rad_oppdatert"] as Timestamp

        Thread.sleep(5)

        val oppdatert =
            aktivitetEntity(version = 2, aktivitetStatus = "FULLFORT", recordOffset = 11, recordKey = "ny-key")
        val resultat = repository.tryLagreAktivitetDataBatch(listOf(oppdatert))
        val radEtter = hentRad(opprinnelig.aktivitetId)!!

        assertThat(resultat.prosesserte).isEqualTo(1)
        assertThat(radEtter["version"]).isEqualTo(2L)
        assertThat(radEtter["aktivitet_status"]).isEqualTo("FULLFORT")
        assertThat(radEtter["record_offset"]).isEqualTo(11L)
        assertThat(radEtter["record_key"]).isEqualTo("ny-key")
        assertThat(radEtter["rad_opprettet"]).isEqualTo(radOpprettetFoer)
        assertThat((radEtter["rad_oppdatert"] as Timestamp)).isAfter(radOppdatertFoer)
    }

    @Test
    fun `skal ignorere eldre version`() {
        repository.tryLagreAktivitetDataBatch(
            listOf(aktivitetEntity(version = 2, aktivitetStatus = "FULLFORT", recordOffset = 20)),
        )

        val resultat = repository.tryLagreAktivitetDataBatch(
            listOf(aktivitetEntity(version = 1, aktivitetStatus = "PLANLAGT", recordOffset = 21)),
        )
        val rad = hentRad("aktivitet-1")!!

        assertThat(resultat.prosesserte).isZero()
        assertThat(resultat.ignorert).isEqualTo(1)
        assertThat(rad["version"]).isEqualTo(2L)
        assertThat(rad["aktivitet_status"]).isEqualTo("FULLFORT")
        assertThat(rad["record_offset"]).isEqualTo(20L)
    }

    @Test
    fun `skal ignorere historisk melding med eldre version enn lagret aktivitet`() {
        repository.tryLagreAktivitetDataBatch(
            listOf(aktivitetEntity(version = 3, aktivitetStatus = "FULLFORT", recordOffset = 20)),
        )

        val resultat = repository.tryLagreAktivitetDataBatch(
            listOf(aktivitetEntity(historisk = true, version = 2, recordOffset = 21)),
        )
        val rad = hentRad("aktivitet-1")!!

        assertThat(resultat.prosesserte).isZero()
        assertThat(resultat.ignorert).isEqualTo(1)
        assertThat(rad["version"]).isEqualTo(3L)
        assertThat(rad["aktivitet_status"]).isEqualTo("FULLFORT")
        assertThat(rad["record_offset"]).isEqualTo(20L)
    }

    @Test
    fun `skal slette aktivitet naar historisk er true og version er nyere`() {
        repository.tryLagreAktivitetDataBatch(listOf(aktivitetEntity()))

        val resultat = repository.tryLagreAktivitetDataBatch(
            listOf(aktivitetEntity(historisk = true, version = 2)),
        )

        assertThat(resultat.prosesserte).isEqualTo(1)
        assertThat(hentRad("aktivitet-1")).isNull()
    }

    @Test
    fun `skal deduplisere og beholde siste melding per aktivitet i batch`() {
        val resultat = repository.tryLagreAktivitetDataBatch(
            listOf(
                aktivitetEntity(
                    aktivitetId = "aktivitet-1",
                    version = 1,
                    aktivitetStatus = "PLANLAGT",
                    recordOffset = 10
                ),
                aktivitetEntity(
                    aktivitetId = "aktivitet-1",
                    version = 2,
                    aktivitetStatus = "FULLFORT",
                    recordOffset = 11
                ),
                aktivitetEntity(
                    aktivitetId = "aktivitet-2",
                    version = 5,
                    aktivitetStatus = "GJENNOMFORES",
                    recordOffset = 12
                ),
            ),
        )

        val aktivitet1 = hentRad("aktivitet-1")!!
        val aktivitet2 = hentRad("aktivitet-2")!!

        assertThat(resultat.mottatte).isEqualTo(3)
        assertThat(resultat.dedupliserte).isEqualTo(1)
        assertThat(resultat.prosesserte).isEqualTo(2)
        assertThat(resultat.ignorert).isZero()
        assertThat(aktivitet1["version"]).isEqualTo(2L)
        assertThat(aktivitet1["aktivitet_status"]).isEqualTo("FULLFORT")
        assertThat(aktivitet1["record_offset"]).isEqualTo(11L)
        assertThat(aktivitet2["version"]).isEqualTo(5L)
        assertThat(aktivitet2["record_offset"]).isEqualTo(12L)
    }

    @Test
    fun `skal bevare rekkefoelge ved dedup - siste melding per aktivitet vinn`() {
        val resultat = repository.tryLagreAktivitetDataBatch(
            listOf(
                aktivitetEntity(aktivitetId = "a1", version = 1, aktivitetStatus = "PLANLAGT", recordOffset = 1),
                aktivitetEntity(aktivitetId = "a2", version = 1, aktivitetStatus = "PLANLAGT", recordOffset = 2),
                aktivitetEntity(aktivitetId = "a1", version = 2, aktivitetStatus = "GJENNOMFORES", recordOffset = 3),
                aktivitetEntity(aktivitetId = "a3", version = 1, aktivitetStatus = "PLANLAGT", recordOffset = 4),
                aktivitetEntity(aktivitetId = "a2", version = 2, aktivitetStatus = "FULLFORT", recordOffset = 5),
            ),
        )

        assertThat(resultat.mottatte).isEqualTo(5)
        assertThat(resultat.dedupliserte).isEqualTo(2)
        assertThat(resultat.prosesserte).isEqualTo(3)

        val a1 = hentRad("a1")!!
        val a2 = hentRad("a2")!!
        val a3 = hentRad("a3")!!

        // a1: v1 (offset 1) og v2 (offset 3) → v2 vinn fordi den kjem sist i lista
        assertThat(a1["version"]).isEqualTo(2L)
        assertThat(a1["aktivitet_status"]).isEqualTo("GJENNOMFORES")
        assertThat(a1["record_offset"]).isEqualTo(3L)

        // a2: v1 (offset 2) og v2 (offset 5) → v2 vinn
        assertThat(a2["version"]).isEqualTo(2L)
        assertThat(a2["aktivitet_status"]).isEqualTo("FULLFORT")
        assertThat(a2["record_offset"]).isEqualTo(5L)

        // a3: berre éin melding
        assertThat(a3["version"]).isEqualTo(1L)
        assertThat(a3["record_offset"]).isEqualTo(4L)
    }

    @Test
    fun `skal haandtere tom batch`() {
        val resultat = repository.tryLagreAktivitetDataBatch(emptyList())

        assertThat(resultat.mottatte).isZero()
        assertThat(resultat.prosesserte).isZero()
    }

    @Test
    fun `skal haandtere batch med bade upserts og slettinger`() {
        repository.tryLagreAktivitetDataBatch(
            listOf(
                aktivitetEntity(aktivitetId = "aktivitet-1", version = 1),
                aktivitetEntity(aktivitetId = "aktivitet-2", version = 1),
            ),
        )

        val resultat = repository.tryLagreAktivitetDataBatch(
            listOf(
                aktivitetEntity(aktivitetId = "aktivitet-1", version = 2, aktivitetStatus = "FULLFORT"),
                aktivitetEntity(aktivitetId = "aktivitet-2", version = 2, historisk = true),
            ),
        )

        assertThat(resultat.mottatte).isEqualTo(2)
        assertThat(resultat.prosesserte).isEqualTo(2)
        assertThat(hentRad("aktivitet-1")!!["aktivitet_status"]).isEqualTo("FULLFORT")
        assertThat(hentRad("aktivitet-2")).isNull()
    }

    @Test
    fun `skal ved dedup velge historisk sletting over tidligere upsert for samme aktivitet`() {
        repository.tryLagreAktivitetDataBatch(
            listOf(aktivitetEntity(aktivitetId = "aktivitet-1", version = 1)),
        )

        val resultat = repository.tryLagreAktivitetDataBatch(
            listOf(
                aktivitetEntity(aktivitetId = "aktivitet-1", version = 2, aktivitetStatus = "FULLFORT"),
                aktivitetEntity(aktivitetId = "aktivitet-1", version = 3, historisk = true),
            ),
        )

        assertThat(resultat.mottatte).isEqualTo(2)
        assertThat(resultat.dedupliserte).isEqualTo(1)
        assertThat(resultat.prosesserte).isEqualTo(1)
        assertThat(hentRad("aktivitet-1")).isNull()
    }

    private fun hentRad(aktivitetId: String): Map<String, Any?>? =
        jdbcTemplate.queryForList(
            "SELECT * FROM $TABLE_NAME WHERE $AKTIVITET_ID = ?",
            aktivitetId
        ).firstOrNull()

    private fun aktivitetEntity(
        aktivitetId: String = "aktivitet-1",
        version: Long = 1,
        historisk: Boolean = false,
        aktivitetStatus: String = "GJENNOMFORES",
        recordOffset: Long = 15,
        recordPartition: Int = 3,
        recordKey: String = aktivitetId,
    ) = KafkaAktivitetMeldingEntity(
        aktivitetId = aktivitetId,
        aktorId = "aktor-1",
        aktivitetType = "TILTAK",
        aktivitetStatus = aktivitetStatus,
        endringsType = "OPPRETTET",
        fraDato = "2024-01-01T10:15:30Z",
        tilDato = "2024-02-01T10:15:30Z",
        endretDato = "2024-03-01T10:15:30Z",
        tiltakskode = "ARBFORB",
        lagtInnAv = "NAV",
        avtalt = true,
        version = version,
        historisk = historisk,
        cvKanDelesStatus = "IKKE_SVART",
        svarfristStillingFraNav = "2024-04-01",
        recordOffset = recordOffset,
        recordPartition = recordPartition,
        recordKey = recordKey,
    )
}
