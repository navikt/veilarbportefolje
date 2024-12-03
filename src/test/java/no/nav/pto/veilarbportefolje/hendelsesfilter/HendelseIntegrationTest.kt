package no.nav.pto.veilarbportefolje.hendelsesfilter

import no.nav.common.types.identer.NorskIdent
import no.nav.pto.veilarbportefolje.database.PostgresTable.HENDELSE
import no.nav.pto.veilarbportefolje.domene.Bruker
import no.nav.pto.veilarbportefolje.domene.Filtervalg
import no.nav.pto.veilarbportefolje.domene.Sorteringsfelt
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.OpensearchTestClient.pollOpensearchUntil
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.*

class HendelseIntegrationTest(
    @Autowired private val opensearchService: OpensearchService,
    @Autowired private val hendelseService: HendelseService,
    @Autowired private val hendelseRepository: HendelseRepository,
    @Autowired private val jdbcTemplate: JdbcTemplate
) : EndToEndTest() {

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE aktiviteter")
        jdbcTemplate.update("TRUNCATE oppfolgingsbruker_arena_v2")
        jdbcTemplate.update("TRUNCATE bruker_identer")
        jdbcTemplate.update("TRUNCATE oppfolging_data")
        jdbcTemplate.update("TRUNCATE nom_skjerming")
        jdbcTemplate.update("TRUNCATE TABLE ${HENDELSE.TABLE_NAME}")
    }

    @Test
    fun `skal oppdatere data om utgått varsel på bruker i OpenSearch ved indeksering når vi har hendelse-data for bruker`() {
        // Given
        val brukerAktorId = randomAktorId()
        val brukerFnr = randomFnr()
        val brukerNorskIdent = NorskIdent.of(brukerFnr.get())
        val brukerOppfolgingsEnhet = randomNavKontor()
        val hendelse = genererRandomHendelse(personIdent = brukerNorskIdent)
        testDataClient.lagreBrukerUnderOppfolging(brukerAktorId, brukerFnr, brukerOppfolgingsEnhet.value, null)
        hendelseRepository.insert(hendelse)

        // When
        opensearchIndexer.indekser(brukerAktorId)

        // Then
        pollOpensearchUntil { opensearchTestClient.countDocuments() == 1 }
        val brukerFraRespons: Bruker = opensearchService.hentBrukere(
            brukerOppfolgingsEnhet.value,
            Optional.empty(),
            "asc",
            Sorteringsfelt.IKKE_SATT.sorteringsverdi,
            Filtervalg().setFerdigfilterListe(emptyList()),
            null,
            null
        ).brukere.first()
        assertThat(brukerFraRespons).isNotNull
        assertThat(brukerFraRespons.utgattVarsel).isNotNull
        assertThat(brukerFraRespons.utgattVarsel.avsender).isEqualTo(hendelse.avsender)
        assertThat(brukerFraRespons.utgattVarsel.kategori).isEqualTo(hendelse.kategori)
        assertThat(brukerFraRespons.utgattVarsel.personIdent).isEqualTo(hendelse.personIdent)
        assertThat(brukerFraRespons.utgattVarsel.hendelse.beskrivelse).isEqualTo(hendelse.hendelse.beskrivelse)
        assertThat(brukerFraRespons.utgattVarsel.hendelse.detaljer).isEqualTo(hendelse.hendelse.detaljer)
        assertThat(brukerFraRespons.utgattVarsel.hendelse.lenke).isEqualTo(hendelse.hendelse.lenke)
    }

    @Test
    fun `skal oppdatere data om utgått varsel på bruker i OpenSearch ved indeksering når vi ikke har hendelse-data for bruker`() {
        // Given
        val brukerAktorId = randomAktorId()
        val brukerFnr = randomFnr()
        val brukerOppfolgingsEnhet = randomNavKontor()
        testDataClient.lagreBrukerUnderOppfolging(brukerAktorId, brukerFnr, brukerOppfolgingsEnhet.value, null)

        // When
        opensearchIndexer.indekser(brukerAktorId)

        // Then
        pollOpensearchUntil { opensearchTestClient.countDocuments() == 1 }
        val brukerFraRespons: Bruker = opensearchService.hentBrukere(
            brukerOppfolgingsEnhet.value,
            Optional.empty(),
            "asc",
            Sorteringsfelt.IKKE_SATT.sorteringsverdi,
            Filtervalg().setFerdigfilterListe(emptyList()),
            null,
            null
        ).brukere.first()
        assertThat(brukerFraRespons).isNotNull
        assertThat(brukerFraRespons.utgattVarsel).isNull()
    }

    @Test
    fun `skal sette inn data om utgått varsel på bruker i OpenSearch når vi får START-melding`() {
        // Given
        val brukerAktorId = randomAktorId()
        val brukerFnr = randomFnr()
        val brukerNorskIdent = NorskIdent.of(brukerFnr.get())
        val brukerOppfolgingsEnhet = randomNavKontor()
        testDataClient.lagreBrukerUnderOppfolging(brukerAktorId, brukerFnr, brukerOppfolgingsEnhet.value, null)
        opensearchIndexer.indekser(brukerAktorId)

        // When
        val hendelseId = UUID.randomUUID().toString()
        val hendelseRecordValue =
            genererRandomHendelseRecordValue(operasjon = Operasjon.START, personID = brukerNorskIdent)
        val hendelseConsumerRecord =
            genererRandomHendelseConsumerRecord(recordValue = hendelseRecordValue, key = hendelseId)
        hendelseService.behandleKafkaRecord(hendelseConsumerRecord)

        // Then
        pollOpensearchUntil { opensearchTestClient.countDocuments() == 1 }
        val brukerFraRespons: Bruker = opensearchService.hentBrukere(
            brukerOppfolgingsEnhet.value,
            Optional.empty(),
            "asc",
            Sorteringsfelt.IKKE_SATT.sorteringsverdi,
            Filtervalg().setFerdigfilterListe(emptyList()),
            null,
            null
        ).brukere.first()
        assertThat(brukerFraRespons).isNotNull
        assertThat(brukerFraRespons.utgattVarsel).isNotNull
        val forventetHendelse = toHendelse(hendelseRecordValue, hendelseId)
        assertThat(brukerFraRespons.utgattVarsel.avsender).isEqualTo(forventetHendelse.avsender)
        assertThat(brukerFraRespons.utgattVarsel.kategori).isEqualTo(forventetHendelse.kategori)
        assertThat(brukerFraRespons.utgattVarsel.personIdent).isEqualTo(forventetHendelse.personIdent)
        assertThat(brukerFraRespons.utgattVarsel.hendelse.beskrivelse).isEqualTo(forventetHendelse.hendelse.beskrivelse)
        assertThat(brukerFraRespons.utgattVarsel.hendelse.detaljer).isEqualTo(forventetHendelse.hendelse.detaljer)
        assertThat(brukerFraRespons.utgattVarsel.hendelse.lenke).isEqualTo(forventetHendelse.hendelse.lenke)
    }

    @Test
    fun `skal oppdatere data om utgått varsel på bruker i OpenSearch når vi får OPPDATER-melding`() {
        // Given
        val brukerAktorId = randomAktorId()
        val brukerFnr = randomFnr()
        val brukerNorskIdent = NorskIdent.of(brukerFnr.get())
        val brukerOppfolgingsEnhet = randomNavKontor()
        testDataClient.lagreBrukerUnderOppfolging(brukerAktorId, brukerFnr, brukerOppfolgingsEnhet.value, null)
        opensearchIndexer.indekser(brukerAktorId)
        val hendelseId = UUID.randomUUID().toString()
        val hendelseRecordValue =
            genererRandomHendelseRecordValue(operasjon = Operasjon.START, personID = brukerNorskIdent)
        val hendelseConsumerRecord =
            genererRandomHendelseConsumerRecord(recordValue = hendelseRecordValue, key = hendelseId)
        hendelseService.behandleKafkaRecord(hendelseConsumerRecord)

        // When
        val oppdatertHendelseRecordValue = hendelseRecordValue.copy(
            operasjon = Operasjon.OPPDATER,
            hendelse = hendelseRecordValue.hendelse.copy(beskrivelse = "Ny beskrivelse")
        )
        val oppdatertHendelseConsumerRecord = genererRandomHendelseConsumerRecord(
            key = hendelseId,
            recordValue = oppdatertHendelseRecordValue
        )
        hendelseService.behandleKafkaRecord(oppdatertHendelseConsumerRecord)

        // Then
        pollOpensearchUntil { opensearchTestClient.countDocuments() == 1 }
        val brukerFraRespons: Bruker = opensearchService.hentBrukere(
            brukerOppfolgingsEnhet.value,
            Optional.empty(),
            "asc",
            Sorteringsfelt.IKKE_SATT.sorteringsverdi,
            Filtervalg().setFerdigfilterListe(emptyList()),
            null,
            null
        ).brukere.first()
        assertThat(brukerFraRespons).isNotNull
        assertThat(brukerFraRespons.utgattVarsel).isNotNull
        val forventetHendelse = toHendelse(oppdatertHendelseRecordValue, hendelseId)
        assertThat(brukerFraRespons.utgattVarsel.avsender).isEqualTo(forventetHendelse.avsender)
        assertThat(brukerFraRespons.utgattVarsel.kategori).isEqualTo(forventetHendelse.kategori)
        assertThat(brukerFraRespons.utgattVarsel.personIdent).isEqualTo(forventetHendelse.personIdent)
        assertThat(brukerFraRespons.utgattVarsel.hendelse.beskrivelse).isEqualTo(forventetHendelse.hendelse.beskrivelse)
        assertThat(brukerFraRespons.utgattVarsel.hendelse.detaljer).isEqualTo(forventetHendelse.hendelse.detaljer)
        assertThat(brukerFraRespons.utgattVarsel.hendelse.lenke).isEqualTo(forventetHendelse.hendelse.lenke)
    }

    @Test
    fun `skal fjerne data om utgått varsel på bruker i OpenSearch når vi får STOPP-melding`() {
        // Given
        val brukerAktorId = randomAktorId()
        val brukerFnr = randomFnr()
        val brukerNorskIdent = NorskIdent.of(brukerFnr.get())
        val brukerOppfolgingsEnhet = randomNavKontor()
        testDataClient.lagreBrukerUnderOppfolging(brukerAktorId, brukerFnr, brukerOppfolgingsEnhet.value, null)
        opensearchIndexer.indekser(brukerAktorId)
        val hendelseId = UUID.randomUUID().toString()
        val hendelseRecordValue =
            genererRandomHendelseRecordValue(operasjon = Operasjon.START, personID = brukerNorskIdent)
        val hendelseConsumerRecord =
            genererRandomHendelseConsumerRecord(recordValue = hendelseRecordValue, key = hendelseId)
        hendelseService.behandleKafkaRecord(hendelseConsumerRecord)

        // When
        val oppdatertHendelseRecordValue = hendelseRecordValue.copy(operasjon = Operasjon.STOPP)
        val oppdatertHendelseConsumerRecord = genererRandomHendelseConsumerRecord(
            key = hendelseId,
            recordValue = oppdatertHendelseRecordValue
        )
        hendelseService.behandleKafkaRecord(oppdatertHendelseConsumerRecord)

        // Then
        pollOpensearchUntil { opensearchTestClient.countDocuments() == 1 }
        val brukerFraRespons: Bruker = opensearchService.hentBrukere(
            brukerOppfolgingsEnhet.value,
            Optional.empty(),
            "asc",
            Sorteringsfelt.IKKE_SATT.sorteringsverdi,
            Filtervalg().setFerdigfilterListe(emptyList()),
            null,
            null
        ).brukere.first()
        assertThat(brukerFraRespons).isNotNull
        assertThat(brukerFraRespons.utgattVarsel).isNull()
    }
}
