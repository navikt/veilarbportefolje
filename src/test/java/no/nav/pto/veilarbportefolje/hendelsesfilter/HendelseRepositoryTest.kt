package no.nav.pto.veilarbportefolje.hendelsesfilter

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.database.PostgresTable.HENDELSE
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNorskIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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

    @ParameterizedTest
    @EnumSource(Kategori::class)
    fun `skal lagre Hendelse når hendelse med ID ikke eksisterer fra før`(kategori: Kategori) {
        // Given
        val hendelse = genererRandomHendelse(kategori = kategori)

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

    @ParameterizedTest
    @EnumSource(Kategori::class)
    fun `skal kaste HendelseIdEksistererAlleredeException ved opprettelse når hendelse med ID eksisterer fra før`(
        kategori: Kategori
    ) {
        // Given
        val opprinneligHendelse = genererRandomHendelse(kategori = kategori)
        hendelseRepository.insert(opprinneligHendelse)

        // When
        val nyHendelseMedSammeId = genererRandomHendelse(id = opprinneligHendelse.id, kategori = kategori)
        val resultatAvOpprettelse = try {
            hendelseRepository.insert(nyHendelseMedSammeId)
        } catch (ex: RuntimeException) {
            ex
        }

        // Then
        assertThat(resultatAvOpprettelse).isNotNull
        assertThat(resultatAvOpprettelse).isInstanceOf(HendelseIdEksistererAlleredeException::class.java)
    }

    @ParameterizedTest
    @EnumSource(Kategori::class)
    fun `skal hente Hendelse når hendelse med ID eksisterer`(kategori: Kategori) {
        // Given
        val hendelse = genererRandomHendelse(kategori = kategori)
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

    @ParameterizedTest
    @EnumSource(Kategori::class)
    fun `skal hente eldste Hendelse når det bare er en hendelse på person`(kategori: Kategori) {
        // Given
        val personIdent = randomNorskIdent()
        val hendelse = genererRandomHendelse(personIdent = personIdent, kategori = kategori)
        hendelseRepository.insert(hendelse = hendelse)

        // When
        val resultatAvHenting = hendelseRepository.getEldste(personIdent = personIdent, kategori = kategori)

        // Then
        val forventetHendelse = hendelse.copy()
        assertThat(resultatAvHenting).isEqualTo(forventetHendelse)
    }

    @ParameterizedTest
    @EnumSource(Kategori::class)
    fun `skal hente eldste Hendelse når det eksisterer flere hendelser med en bestemt kategori på person`(kategori: Kategori) {
        // Given
        val personIdent = randomNorskIdent()
        val naa = ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS)
        val hendelseEldste =
            genererRandomHendelse(personIdent = personIdent, hendelseDato = naa.minusDays(2), kategori = kategori)
        val hendelseNestEldste =
            genererRandomHendelse(personIdent = personIdent, hendelseDato = naa.minusDays(1), kategori = kategori)
        val hendelseNyeste = genererRandomHendelse(personIdent = personIdent, hendelseDato = naa, kategori = kategori)
        hendelseRepository.insert(hendelse = hendelseNyeste)
        hendelseRepository.insert(hendelse = hendelseNestEldste)
        hendelseRepository.insert(hendelse = hendelseEldste)

        // When
        val resultatAvHenting = hendelseRepository.getEldste(personIdent = personIdent, kategori = kategori)

        // Then
        val forventetHendelse = hendelseEldste.copy()
        assertThat(resultatAvHenting).isEqualTo(forventetHendelse)
    }

    @ParameterizedTest
    @EnumSource(Kategori::class)
    fun `skal hente eldste Hendelse av en bestemt kategori for bruker, selv når det finnes hendelser av andre kategorier som er eldre`(kategori: Kategori) {
        // Given
        val personIdent = randomNorskIdent()
        val naa = ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS)
        val hendelseEldsteAvKategori =
            genererRandomHendelse(personIdent = personIdent, hendelseDato = naa.minusDays(1), kategori = kategori)
        val hendelseNyesteAvKategori =
            genererRandomHendelse(personIdent = personIdent, hendelseDato = naa, kategori = kategori)
        hendelseRepository.insert(hendelseNyesteAvKategori)
        hendelseRepository.insert(hendelseEldsteAvKategori)

        Kategori.values().filter { it !== kategori }.forEach {
            hendelseRepository.insert(
                genererRandomHendelse(
                    personIdent = personIdent,
                    hendelseDato = naa.minusDays(2),
                    kategori = it
                )
            )
        }

        // When
        val resultatAvHenting = hendelseRepository.getEldste(personIdent = personIdent, kategori = kategori)

        // Then
        val forventetHendelse = hendelseEldsteAvKategori.copy()
        assertThat(resultatAvHenting).isEqualTo(forventetHendelse)
    }

    @ParameterizedTest
    @EnumSource(Kategori::class)
    fun `skal kaste IngenHendelseForPersonException ved henting av eldste når ingen hendelser eksisterer for person`(kategori: Kategori) {
        // Given
        val personIdent = randomNorskIdent()

        Kategori.values().filter { it !== kategori }.forEach {
            hendelseRepository.insert(
                genererRandomHendelse(
                    personIdent = personIdent,
                    kategori = it
                )
            )
        }

        // When
        val resultatAvHenting = try {
            hendelseRepository.getEldste(personIdent = personIdent, kategori = kategori)
        } catch (ex: IngenHendelseForPersonException) {
            ex
        }

        // Then
        assertThat(resultatAvHenting).isInstanceOf(IngenHendelseForPersonException::class.java)
    }

    @ParameterizedTest
    @EnumSource(Kategori::class)
    fun `skal kaste IngenHendelseForPersonException ved henting av eldste når ingen hendelser med rett kategori eksisterer for person`(kategori: Kategori) {
        // Given
        val personIdent = randomNorskIdent()

        // When
        val resultatAvHenting = try {
            hendelseRepository.getEldste(personIdent = personIdent, kategori = kategori)
        } catch (ex: IngenHendelseForPersonException) {
            ex
        }

        // Then
        assertThat(resultatAvHenting).isInstanceOf(IngenHendelseForPersonException::class.java)
    }

    @ParameterizedTest
    @EnumSource(Kategori::class)
    fun `skal oppdatere hendelse når hendelse med ID eksisterer`(kategori: Kategori) {
        // Given
        val opprinneligHendelse = genererRandomHendelse(kategori = kategori)
        hendelseRepository.insert(opprinneligHendelse)

        // When
        val hendelseMedSammeIDOgOppdatertData = opprinneligHendelse.copy(
            hendelse = opprinneligHendelse.hendelse.copy(
                beskrivelse = "En annen beskrivelse"
            )
        )
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

    @ParameterizedTest
    @EnumSource(Kategori::class)
    fun `skal kaste IngenHendelseMedIdException ved oppdatering når hendelse med ID ikke eksisterer`(kategori: Kategori) {
        // Given
        val hendelseIdSomIkkeEksisterer = randomUUID()

        // When
        val hendelseMedIdSomIkkeEksisterer = genererRandomHendelse(id = hendelseIdSomIkkeEksisterer, kategori = kategori)
        val resultatAvOppdatering = try {
            hendelseRepository.update(hendelseMedIdSomIkkeEksisterer)
        } catch (ex: RuntimeException) {
            ex
        }

        // Then
        assertThat(resultatAvOppdatering).isInstanceOf(IngenHendelseMedIdException::class.java)
    }

    @ParameterizedTest
    @EnumSource(Kategori::class)
    fun `skal slette hendelse når hendelse med ID eksisterer`(kategori: Kategori) {
        // Given
        val hendelse = genererRandomHendelse(kategori = kategori)
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

    @ParameterizedTest
    @EnumSource(Kategori::class)
    fun `skal kaste IngenHendelseMedIdException ved sletting når ingen hendelse med ID eksisterer`(kategori: Kategori) {
        // Given
        val hendelseIdSomIkkeEksisterer = genererRandomHendelse(kategori = kategori)

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
