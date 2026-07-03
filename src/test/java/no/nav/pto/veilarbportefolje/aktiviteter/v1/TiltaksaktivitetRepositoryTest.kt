package no.nav.pto.veilarbportefolje.aktiviteter.v1

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.aktiviteter.domene.InaktivAktivitetStatus
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.database.PostgresTable.AO_KONTOR.*
import no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.FODSELSNR
import no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.NAV_KONTOR
import no.nav.pto.veilarbportefolje.database.PostgresTable.TILTAKKODEVERK.KODE
import no.nav.pto.veilarbportefolje.database.PostgresTable.TILTAKKODEVERK.VERDI
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import no.nav.pto.veilarbportefolje.database.PostgresTable.AO_KONTOR.TABLE_NAME as AO_KONTOR_TABLE
import no.nav.pto.veilarbportefolje.database.PostgresTable.KAFKA_AKTIVITET_MELDING.TABLE_NAME as KAFKA_AKTIVITET_MELDING_TABLE
import no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.TABLE_NAME as ARENA_OPPFOLGINGSBRUKER_TABLE
import no.nav.pto.veilarbportefolje.database.PostgresTable.TILTAKKODEVERK.TABLE_NAME as TILTAKKODEVERK_TABLE

@SpringBootTest(classes = [ApplicationConfigTest::class])
class TiltaksaktivitetRepositoryTest (
    @param:Autowired private val jdbcTemplate: JdbcTemplate,
    @param:Autowired private val brukertiltakRepository: BrukertiltakRepository,
    @param:Autowired private val portefoljeAktivitetKafkaMeldingRepository: PortefoljeAktivitetKafkaMeldingRepository
){
    private lateinit var jdbcNameParameter: NamedParameterJdbcTemplate

    val fnr = Fnr.ofValidFnr("12345678911")
    val aktorid = AktorId.of("1234567891234")

    @BeforeEach
    fun setupData(){
        jdbcNameParameter = NamedParameterJdbcTemplate(jdbcTemplate)

        jdbcTemplate.execute("TRUNCATE TABLE BRUKER_IDENTER")
        jdbcTemplate.execute("TRUNCATE TABLE $TILTAKKODEVERK_TABLE")
        jdbcTemplate.execute("TRUNCATE TABLE $KAFKA_AKTIVITET_MELDING_TABLE")
        jdbcTemplate.execute("TRUNCATE TABLE $AO_KONTOR_TABLE")
        jdbcTemplate.execute("TRUNCATE TABLE $ARENA_OPPFOLGINGSBRUKER_TABLE")

        jdbcTemplate.execute("""INSERT INTO BRUKER_IDENTER (PERSON,IDENT,HISTORISK, GRUPPE) VALUES('12345678', '${fnr.get()}', false, 'FOLKEREGISTERIDENT')""")
        jdbcTemplate.execute("""INSERT INTO BRUKER_IDENTER (PERSON,IDENT,HISTORISK, GRUPPE) VALUES('12345678', '${aktorid.get()}',false, 'AKTORID')""")

        jdbcTemplate.execute("""INSERT INTO $TILTAKKODEVERK_TABLE ($KODE, $VERDI) VALUES('TILTAK1', 'Tiltak 1')""")
        jdbcTemplate.execute("""INSERT INTO $TILTAKKODEVERK_TABLE ($KODE, $VERDI) VALUES('TILTAK2', 'Tiltak 2')""")
        jdbcTemplate.execute("""INSERT INTO $TILTAKKODEVERK_TABLE ($KODE, $VERDI) VALUES('TILTAK4', 'Tiltak 4')""")

        jdbcTemplate.execute("""INSERT INTO $AO_KONTOR_TABLE ($IDENT, $AKTORID, $KONTOR_ID) VALUES('${fnr.get()}', '${aktorid.get()}', '1234')""")
        jdbcTemplate.execute("""INSERT INTO $ARENA_OPPFOLGINGSBRUKER_TABLE ($FODSELSNR, $NAV_KONTOR) VALUES('${fnr.get()}', '1234')""")

       // jdbcTemplate.execute("""INSERT INTO $KAFKA_AKTIVITET_MELDING_TABLE ($, $VERDI) VALUES('TILTAK1', 'Tiltak 1')""")
        val aktivitetEntity = aktivitetEntity(aktivitetId = "aktivitetId-1", aktorId = "${aktorid.get()}", tiltakskode = "TILTAK1")
        val aktivitetEntity2 = aktivitetEntity(aktivitetId = "aktivitetId-2", aktorId = "${aktorid.get()}", tiltakskode = "TILTAK2")
        val aktivitetEntity3 = aktivitetEntity(aktivitetId = "aktivitetId-3", aktorId = "${aktorid.get()}", tiltakskode = "TILTAK3")
        val aktivitetEntity4 = aktivitetEntity(aktivitetId = "aktivitetId-4", aktorId = "${aktorid.get()}", tiltakskode = "TILTAK4", aktivitetStatus = InaktivAktivitetStatus.AVBRUTT.name)

        val aktiviteterListe = listOf(aktivitetEntity, aktivitetEntity2, aktivitetEntity3, aktivitetEntity4)

        portefoljeAktivitetKafkaMeldingRepository.behandleAktivitetsKafkaMeldinger(aktiviteterListe)
    }

    @Test
    fun `Tiltakstype returneres når tiltakskode eksisterer i tiltakskodeverket-tabellen`() {
        val enhetId = EnhetId("1234")

        val tiltakskodeMapping = brukertiltakRepository.hentTiltakstyperForEnhet(enhetId)

        assertThat(tiltakskodeMapping.tiltak).hasSize(2)
        assertThat(tiltakskodeMapping.tiltak.containsKey("TILTAK1")).isTrue
        assertThat(tiltakskodeMapping.tiltak.containsValue("Tiltak 1")).isTrue

        assertThat(tiltakskodeMapping.tiltak.containsKey("TILTAK2")).isTrue
        assertThat(tiltakskodeMapping.tiltak.containsValue("Tiltak 2")).isTrue
    }

    @Test
    fun `Tiltakstype returneres ikke når tiltakskode ikke eksisterer i tiltakskodeverket-tabellen`() {
        val enhetId = EnhetId("1234")

        val tiltakskodeMapping = brukertiltakRepository.hentTiltakstyperForEnhet(enhetId)

        assertThat(tiltakskodeMapping.tiltak).hasSize(2)
        assertThat(tiltakskodeMapping.tiltak.containsKey("TILTAK1")).isTrue
        assertThat(tiltakskodeMapping.tiltak.containsValue("Tiltak 1")).isTrue

        assertThat(tiltakskodeMapping.tiltak.containsKey("TILTAK2")).isTrue
        assertThat(tiltakskodeMapping.tiltak.containsValue("Tiltak 2")).isTrue

        assertThat(tiltakskodeMapping.tiltak.containsKey("TILTAK3")).isFalse
        assertThat(tiltakskodeMapping.tiltak.containsValue("Tiltak 3")).isFalse
    }

    @Test
    fun `Tiltakstype returneres ikke for inaktive tiltaksaktiviteter`() {
        val enhetId = EnhetId("1234")

        val tiltakskodeMapping = brukertiltakRepository.hentTiltakstyperForEnhet(enhetId)

        assertThat(tiltakskodeMapping.tiltak).hasSize(2)
        assertThat(tiltakskodeMapping.tiltak.containsKey("TILTAK1")).isTrue
        assertThat(tiltakskodeMapping.tiltak.containsValue("Tiltak 1")).isTrue

        assertThat(tiltakskodeMapping.tiltak.containsKey("TILTAK2")).isTrue
        assertThat(tiltakskodeMapping.tiltak.containsValue("Tiltak 2")).isTrue

        assertThat(tiltakskodeMapping.tiltak.containsKey("TILTAK4")).isFalse
        assertThat(tiltakskodeMapping.tiltak.containsValue("Tiltak 4")).isFalse
    }

    fun aktivitetEntity(
        aktivitetId: String = "aktivitet-1",
        aktorId: String = "aktor-1",
        version: Long = 1,
        historisk: Boolean = false,
        aktivitetStatus: String = "GJENNOMFORES",
        tiltakskode: String = "ARBFORB",
        aktivitetType: String = "TILTAK",
        recordOffset: Long = 15,
        recordPartition: Int = 3,
        recordKey: String = aktivitetId,
    ) = KafkaAktivitetMeldingEntity(
        aktivitetId = aktivitetId,
        aktorId = aktorId,
        aktivitetType = aktivitetType,
        aktivitetStatus = aktivitetStatus,
        endringsType = "OPPRETTET",
        fraDato = "2024-01-01T10:15:30Z",
        tilDato = "2024-02-01T10:15:30Z",
        endretDato = "2024-03-01T10:15:30Z",
        tiltakskode = tiltakskode,
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