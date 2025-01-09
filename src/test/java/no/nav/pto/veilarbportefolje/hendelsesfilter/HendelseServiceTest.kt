package no.nav.pto.veilarbportefolje.hendelsesfilter

import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.database.PostgresTable.HENDELSE
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNorskIdent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.*

class HendelseServiceTest(
    @Autowired private val hendelseService: HendelseService,
    @Autowired private val jdbcTemplate: JdbcTemplate
) : EndToEndTest() {

    @BeforeEach
    fun `reset data`() {
        jdbcTemplate.update("TRUNCATE TABLE ${HENDELSE.TABLE_NAME}")
    }

    @Test
    fun `skal starte hendelse når operasjon = START og hendelsen er ny`() {
        // Given
        val norskIdent = randomNorskIdent()
        val fnr = Fnr.of(norskIdent.get())
        val aktorId = randomAktorId()
        insertOppfolgingsInformasjon(aktorId, fnr)
        val key = "96463d56-019e-4b30-ae9b-7365cf002a09"
        val hendelseRecordValue = genererRandomHendelseRecordValue(personID = norskIdent, operasjon = Operasjon.START)
        val hendelseRecord =
            ConsumerRecord(
                KafkaConfigCommon.Topic.PORTEFOLJE_HENDELSESFILTER.topicName,
                0,
                0,
                key,
                hendelseRecordValue
            )

        // When
        hendelseService.behandleKafkaRecord(hendelseRecord)
        val lagretHendelse = hendelseService.hentHendelse(UUID.fromString(key))

        // Then
        assertThat(lagretHendelse).isNotNull
    }

    @Test
    fun `skal ikke starte hendelse når operasjon = START og hendelse-ID eksisterer fra før`() {
        // Given
        val norskIdent = randomNorskIdent()
        val fnr = Fnr.of(norskIdent.get())
        val aktorId = randomAktorId()
        insertOppfolgingsInformasjon(aktorId, fnr)
        val key = "96463d56-019e-4b30-ae9b-7365cf002a09"
        val opprinneligHendelseRecordValue =
            genererRandomHendelseRecordValue(personID = norskIdent, operasjon = Operasjon.START)
        val opprinneligHendelseRecord =
            genererRandomHendelseConsumerRecord(key = key, recordValue = opprinneligHendelseRecordValue)
        hendelseService.behandleKafkaRecord(opprinneligHendelseRecord)

        // When
        val hendelseRecordValueMedSammeIdOgOperasjonMenAndreData = opprinneligHendelseRecordValue.copy(
            hendelse = opprinneligHendelseRecordValue.hendelse.copy(
                beskrivelse = "Et annet hendelsesnavn",
                detaljer = "Andre hendelsesdetaljer"
            )
        )
        val nyHendelseRecord = genererRandomHendelseConsumerRecord(
            key = key,
            recordValue = hendelseRecordValueMedSammeIdOgOperasjonMenAndreData
        )
        hendelseService.behandleKafkaRecord(nyHendelseRecord)

        // Then
        val lagretHendelse = hendelseService.hentHendelse(UUID.fromString(key))
        val forventetHendelse = opprinneligHendelseRecordValue.let {
            Hendelse(
                id = UUID.fromString(key),
                personIdent = it.personID,
                avsender = it.avsender,
                kategori = it.kategori,
                hendelse = Hendelse.HendelseInnhold(
                    beskrivelse = it.hendelse.beskrivelse,
                    dato = it.hendelse.dato,
                    lenke = it.hendelse.lenke,
                    detaljer = it.hendelse.detaljer,
                ),
            )
        }
        assertThat(lagretHendelse).isEqualTo(forventetHendelse)
    }

    @Test
    fun `skal oppdatere hendelse når operasjon=OPPDATER og hendelse-ID eksisterer fra før`() {
        // Given
        val norskIdent = randomNorskIdent()
        val fnr = Fnr.of(norskIdent.get())
        val aktorId = randomAktorId()
        insertOppfolgingsInformasjon(aktorId, fnr)
        val key = "96463d56-019e-4b30-ae9b-7365cf002a09"
        val opprinneligHendelseRecordValue =
            genererRandomHendelseRecordValue(personID = norskIdent, operasjon = Operasjon.START)
        val opprinneligHendelseRecord =
            genererRandomHendelseConsumerRecord(key = key, recordValue = opprinneligHendelseRecordValue)
        hendelseService.behandleKafkaRecord(opprinneligHendelseRecord)

        // When
        val oppdatertHendelseRecordValue = opprinneligHendelseRecordValue.copy(
            operasjon = Operasjon.OPPDATER,
            hendelse = opprinneligHendelseRecordValue.hendelse.copy(
                beskrivelse = "Et annet hendelsesnavn",
                detaljer = "Andre hendelsesdetaljer"
            )
        )
        val nyHendelseRecord =
            genererRandomHendelseConsumerRecord(key = key, recordValue = oppdatertHendelseRecordValue)
        hendelseService.behandleKafkaRecord(nyHendelseRecord)

        // Then
        val lagretHendelse = hendelseService.hentHendelse(UUID.fromString(key))
        val forventetHendelse = toHendelse(oppdatertHendelseRecordValue, key)
        assertThat(lagretHendelse).isEqualTo(forventetHendelse)
    }

    @Test
    fun `skal slette hendelse når operasjon=STOPP og hendelse-ID eksisterer fra før`() {
        // Given
        val norskIdent = randomNorskIdent()
        val fnr = Fnr.of(norskIdent.get())
        val aktorId = randomAktorId()
        insertOppfolgingsInformasjon(aktorId, fnr)
        val key = "96463d56-019e-4b30-ae9b-7365cf002a09"
        val hendelseRecordValue = genererRandomHendelseRecordValue(personID = norskIdent, operasjon = Operasjon.START)
        val hendelseRecord = genererRandomHendelseConsumerRecord(key = key, recordValue = hendelseRecordValue)
        hendelseService.behandleKafkaRecord(hendelseRecord)

        // When
        val hendelseRecordValueMedSammeIdOgDataMenOperasjonStopp = hendelseRecordValue.copy(
            operasjon = Operasjon.STOPP
        )
        val nyHendelseRecord = genererRandomHendelseConsumerRecord(
            key = key,
            recordValue = hendelseRecordValueMedSammeIdOgDataMenOperasjonStopp
        )
        hendelseService.behandleKafkaRecord(nyHendelseRecord)
        val lagretHendelse = hendelseService.hentHendelse(UUID.fromString(key))

        // Then
        assertThat(lagretHendelse).isNull()
    }
}