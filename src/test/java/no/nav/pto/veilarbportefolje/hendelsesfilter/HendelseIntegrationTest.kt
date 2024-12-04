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
        assertThat(brukerFraRespons.utgattVarsel.beskrivelse).isEqualTo(hendelse.hendelse.beskrivelse)
        assertThat(brukerFraRespons.utgattVarsel.detaljer).isEqualTo(hendelse.hendelse.detaljer)
        assertThat(brukerFraRespons.utgattVarsel.lenke).isEqualTo(hendelse.hendelse.lenke)
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
        val forventetHendelseInnhold = toHendelse(hendelseRecordValue, hendelseId).hendelse
        assertThat(brukerFraRespons.utgattVarsel.beskrivelse).isEqualTo(forventetHendelseInnhold.beskrivelse)
        assertThat(brukerFraRespons.utgattVarsel.detaljer).isEqualTo(forventetHendelseInnhold.detaljer)
        assertThat(brukerFraRespons.utgattVarsel.lenke).isEqualTo(forventetHendelseInnhold.lenke)
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
        val forventetHendelseInnhold = toHendelse(oppdatertHendelseRecordValue, hendelseId).hendelse
        assertThat(brukerFraRespons.utgattVarsel.beskrivelse).isEqualTo(forventetHendelseInnhold.beskrivelse)
        assertThat(brukerFraRespons.utgattVarsel.detaljer).isEqualTo(forventetHendelseInnhold.detaljer)
        assertThat(brukerFraRespons.utgattVarsel.lenke).isEqualTo(forventetHendelseInnhold.lenke)
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

    @Test
    fun `dersom ny hendelse med operasjon=START har eldst hendelse-dato, skal ny hendelse overskrive utgått varsel i OpenSearch for bruker`() {
        // Given
        val brukerAktorId = randomAktorId()
        val brukerFnr = randomFnr()
        val brukerNorskIdent = NorskIdent.of(brukerFnr.get())
        val brukerOppfolgingsEnhet = randomNavKontor()
        testDataClient.lagreBrukerUnderOppfolging(brukerAktorId, brukerFnr, brukerOppfolgingsEnhet.value, null)
        opensearchIndexer.indekser(brukerAktorId)
        val yngreHendelseId = "1d5cb509-1fa3-4b92-a552-f91c00c3aba7"
        val yngreHendelseRecordValue =
            genererRandomHendelseRecordValue(operasjon = Operasjon.START, personID = brukerNorskIdent)
        val yngreHendelseConsumerRecord =
            genererRandomHendelseConsumerRecord(recordValue = yngreHendelseRecordValue, key = yngreHendelseId)
        hendelseService.behandleKafkaRecord(yngreHendelseConsumerRecord)

        // When
        val eldreHendelseId = "56d9b9d1-0920-4a2f-bd62-0953d563ce2a"
        val eldreHendelseRecordValue =
            genererRandomHendelseRecordValue(
                operasjon = Operasjon.START,
                personID = brukerNorskIdent,
                hendelseDato = yngreHendelseRecordValue.hendelse.dato.minusDays(10)
            )
        val eldreHendelseConsumerRecord =
            genererRandomHendelseConsumerRecord(recordValue = eldreHendelseRecordValue, key = eldreHendelseId)
        hendelseService.behandleKafkaRecord(eldreHendelseConsumerRecord)

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
        val forventetHendelseInnhold = toHendelse(eldreHendelseRecordValue, eldreHendelseId).hendelse
        assertThat(brukerFraRespons.utgattVarsel.beskrivelse).isEqualTo(forventetHendelseInnhold.beskrivelse)
        assertThat(brukerFraRespons.utgattVarsel.detaljer).isEqualTo(forventetHendelseInnhold.detaljer)
        assertThat(brukerFraRespons.utgattVarsel.lenke).isEqualTo(forventetHendelseInnhold.lenke)
    }

    @Test
    fun `dersom ny hendelse med operasjon=STOPP har eldst hendelse-dato, skal utgått varsel i OpenSearch for bruker oppdateres hendelsen som har nest eldst hendelse-dato`() {
        // Given
        val brukerAktorId = randomAktorId()
        val brukerFnr = randomFnr()
        val brukerNorskIdent = NorskIdent.of(brukerFnr.get())
        val brukerOppfolgingsEnhet = randomNavKontor()
        testDataClient.lagreBrukerUnderOppfolging(brukerAktorId, brukerFnr, brukerOppfolgingsEnhet.value, null)
        opensearchIndexer.indekser(brukerAktorId)
        val yngreHendelseId = "1d5cb509-1fa3-4b92-a552-f91c00c3aba7"
        val yngreHendelseRecordValue =
            genererRandomHendelseRecordValue(operasjon = Operasjon.START, personID = brukerNorskIdent)
        val yngreHendelseConsumerRecord =
            genererRandomHendelseConsumerRecord(recordValue = yngreHendelseRecordValue, key = yngreHendelseId)
        hendelseService.behandleKafkaRecord(yngreHendelseConsumerRecord)
        val eldreHendelseId = "56d9b9d1-0920-4a2f-bd62-0953d563ce2a"
        val eldreHendelseRecordValueStart =
            genererRandomHendelseRecordValue(
                operasjon = Operasjon.START,
                personID = brukerNorskIdent,
                hendelseDato = yngreHendelseRecordValue.hendelse.dato.minusDays(10)
            )
        val eldreHendelseConsumerRecordStart =
            genererRandomHendelseConsumerRecord(recordValue = eldreHendelseRecordValueStart, key = eldreHendelseId)
        hendelseService.behandleKafkaRecord(eldreHendelseConsumerRecordStart)

        // When
        val eldreHendelseRecordValueStopp = eldreHendelseRecordValueStart.copy(operasjon = Operasjon.STOPP)
        val eldreHendelseConsumerRecordStopp =
            genererRandomHendelseConsumerRecord(recordValue = eldreHendelseRecordValueStopp, key = eldreHendelseId)
        hendelseService.behandleKafkaRecord(eldreHendelseConsumerRecordStopp)

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
        val forventetHendelseInnhold = toHendelse(yngreHendelseRecordValue, yngreHendelseId).hendelse
        assertThat(brukerFraRespons.utgattVarsel.beskrivelse).isEqualTo(forventetHendelseInnhold.beskrivelse)
        assertThat(brukerFraRespons.utgattVarsel.detaljer).isEqualTo(forventetHendelseInnhold.detaljer)
        assertThat(brukerFraRespons.utgattVarsel.lenke).isEqualTo(forventetHendelseInnhold.lenke)
    }
}
