package no.nav.pto.veilarbportefolje.hendelsesfilter

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.database.PostgresTable.HENDELSE
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNorskIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID.randomUUID

@SpringBootTest(classes = [ApplicationConfigTest::class])
class HendelseRepositoryTest(
    @Autowired val hendelseRepository: HendelseRepository,
    @Autowired val jdbcTemplate: JdbcTemplate
) {

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE TABLE ${HENDELSE.TABLE_NAME}")
    }

    @Test
    fun `skal lagre Hendelse når hendelse med ID ikke eksisterer fra før`() {
        // Given
        val hendelse = genererRandomHendelse()

        // When
        val resultatAvOpprettelse = try {
            hendelseRepository.insert(hendelse)
        } catch (ex: RuntimeException) {
            ex
        }

        // Then
        // Sjekker her bare at hendelse lagres - sjekk på innhold er i egen test
        val resultatAvHenting = hendelseRepository.get(hendelse.id)
        assertThat(resultatAvOpprettelse).isNotInstanceOf(RuntimeException::class.java)
        assertThat(resultatAvHenting).isNotNull
    }

    @Test
    fun `skal kaste HendelseIdEksistererAlleredeException ved opprettelse når hendelse med ID eksisterer fra før`() {
        // Given
        val opprinneligHendelse = genererRandomHendelse()
        hendelseRepository.insert(opprinneligHendelse)

        // When
        val nyHendelseMedSammeId = genererRandomHendelse(id = opprinneligHendelse.id)
        val resultatAvOpprettelse = try {
            hendelseRepository.insert(nyHendelseMedSammeId)
        } catch (ex: RuntimeException) {
            ex
        }

        // Then
        assertThat(resultatAvOpprettelse).isNotNull
        assertThat(resultatAvOpprettelse).isInstanceOf(HendelseIdEksistererAlleredeException::class.java)
    }

    @Test
    fun `skal hente Hendelse når hendelse med ID eksisterer`() {
        // Given
        val hendelse = genererRandomHendelse()
        hendelseRepository.insert(hendelse)

        // When
        val resultatAvHenting = hendelseRepository.get(hendelse.id)

        // Then
        val forventetHendelse = hendelse.copy()
        assertThat(resultatAvHenting).isEqualTo(forventetHendelse)
    }

    @Test
    fun `skal kaste IngenHendelseMedIdException ved henting når ingen hendelse med ID eksisterer`() {
        // Given
        val hendelseIdSomIkkeEksisterer = randomUUID()

        // When
        val resultatAvHenting = try {
            hendelseRepository.get(hendelseIdSomIkkeEksisterer)
        } catch (ex: IngenHendelseMedIdException) {
            ex
        }

        // Then
        assertThat(resultatAvHenting).isInstanceOf(IngenHendelseMedIdException::class.java)
    }

    @Test
    fun `skal hente eldste Hendelse med utgått varsel-kategori når det bare er en Utgått varsel-hendelse på person`() {
        // Given
        val personIdent = randomNorskIdent()
        val naa = ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS)
        val hendelse = genererRandomHendelse(personIdent = personIdent, kategori = Kategori.UTGATT_VARSEL)
        hendelseRepository.insert(hendelse)

        // When
        val resultatAvHenting = hendelseRepository.getEldsteUtgattVarsel(personIdent)

        // Then
        val forventetHendelse = hendelse.copy()
        assertThat(resultatAvHenting).isEqualTo(forventetHendelse)
    }

    @Test
    fun `skal hente eldste Hendelse med Utgått varsel-kategori når det eksisterer flere Utgått varsel-hendelser på person`() {
        // Given
        val personIdent = randomNorskIdent()
        val naa = ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS)
        val hendelseEldste = genererRandomHendelse(personIdent = personIdent, hendelseDato = naa.minusDays(2), kategori = Kategori.UTGATT_VARSEL)
        val hendelseNestEldste = genererRandomHendelse(personIdent = personIdent, hendelseDato = naa.minusDays(1), kategori = Kategori.UTGATT_VARSEL)
        val hendelseNyeste = genererRandomHendelse(personIdent = personIdent, hendelseDato = naa, kategori = Kategori.UTGATT_VARSEL)
        hendelseRepository.insert(hendelseNyeste)
        hendelseRepository.insert(hendelseNestEldste)
        hendelseRepository.insert(hendelseEldste)

        // When
        val resultatAvHenting = hendelseRepository.getEldsteUtgattVarsel(personIdent)

        // Then
        val forventetHendelse = hendelseEldste.copy()
        assertThat(resultatAvHenting).isEqualTo(forventetHendelse)
    }

    @Test
    fun `skal hente eldste Hendelse med utgått varsel-kategori når det eksisterer flere hendelser med ulike kategorier for person`() {
        // Given
        val personIdent = randomNorskIdent()
        val naa = ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS)
        val hendelseEldste = genererRandomHendelse(personIdent = personIdent, hendelseDato = naa.minusDays(2), kategori = Kategori.UDELT_SAMTALEREFERAT)
        val hendelseNestEldste = genererRandomHendelse(personIdent = personIdent, hendelseDato = naa.minusDays(1), kategori = Kategori.UTGATT_VARSEL)
        val hendelseNyeste = genererRandomHendelse(personIdent = personIdent, hendelseDato = naa, kategori = Kategori.UTGATT_VARSEL)
        hendelseRepository.insert(hendelseNyeste)
        hendelseRepository.insert(hendelseNestEldste)
        hendelseRepository.insert(hendelseEldste)

        // When
        val resultatAvHenting = hendelseRepository.getEldsteUtgattVarsel(personIdent)

        // Then
        val forventetHendelse = hendelseNestEldste.copy()
        assertThat(resultatAvHenting).isEqualTo(forventetHendelse)
    }

    @Test
    fun `skal kaste IngenHendelseForPersonException ved henting av eldste når ingen hendelser eksisterer for person`() {
        // Given
        val personIdent = randomNorskIdent()

        // When
        val resultatAvHenting = try {
            hendelseRepository.getEldsteUtgattVarsel(personIdent)
        } catch (ex: IngenHendelseForPersonException) {
            ex
        }

        // Then
        assertThat(resultatAvHenting).isInstanceOf(IngenHendelseForPersonException::class.java)
    }

    @Test
    fun `skal oppdatere hendelse når hendelse med ID eksisterer`() {
        // Given
        val opprinneligHendelse = genererRandomHendelse()
        hendelseRepository.insert(opprinneligHendelse)

        // When
        val hendelseMedSammeIDOgOppdatertData =
            genererRandomHendelse(id = opprinneligHendelse.id, personIdent = opprinneligHendelse.personIdent)
        val resultatAvOppdatering = try {
            hendelseRepository.update(hendelseMedSammeIDOgOppdatertData)
        } catch (ex: RuntimeException) {
            ex
        }

        // Then
        val resultatAvHenting = hendelseRepository.get(hendelseMedSammeIDOgOppdatertData.id)
        assertThat(resultatAvOppdatering).isNotInstanceOf(IngenHendelseMedIdException::class.java)
        assertThat(hendelseMedSammeIDOgOppdatertData).isEqualTo(resultatAvHenting)
    }

    @Test
    fun `skal kaste IngenHendelseMedIdException ved oppdatering når hendelse med ID ikke eksisterer`() {
        // Given
        val hendelseIdSomIkkeEksisterer = randomUUID()

        // When
        val hendelseMedIdSomIkkeEksisterer = genererRandomHendelse(id = hendelseIdSomIkkeEksisterer)
        val resultatAvOppdatering = try {
            hendelseRepository.update(hendelseMedIdSomIkkeEksisterer)
        } catch (ex: RuntimeException) {
            ex
        }

        // Then
        assertThat(resultatAvOppdatering).isInstanceOf(IngenHendelseMedIdException::class.java)
    }

    @Test
    fun `skal slette hendelse når hendelse med ID eksisterer`() {
        // Given
        val hendelse = genererRandomHendelse()
        hendelseRepository.insert(hendelse)

        // When
        val resultatAvSletting = try {
            hendelseRepository.delete(hendelse.id)
        } catch (ex: RuntimeException) {
            ex
        }

        // Then
        val resultatAvHenting = try {
            hendelseRepository.get(hendelse.id)
        } catch (ex: IngenHendelseMedIdException) {
            ex
        }
        assertThat(resultatAvSletting).isNotInstanceOf(IngenHendelseMedIdException::class.java)
        assertThat(resultatAvHenting).isInstanceOf(IngenHendelseMedIdException::class.java)
    }

    @Test
    fun `skal kaste IngenHendelseMedIdException ved sletting når ingen hendelse med ID eksisterer`() {
        // Given
        val hendelseIdSomIkkeEksisterer = genererRandomHendelse()

        // When
        val resultatAvSletting = try {
            hendelseRepository.delete(hendelseIdSomIkkeEksisterer.id)
        } catch (ex: RuntimeException) {
            ex
        }

        // Then
        assertThat(resultatAvSletting).isInstanceOf(IngenHendelseMedIdException::class.java)
    }
}
