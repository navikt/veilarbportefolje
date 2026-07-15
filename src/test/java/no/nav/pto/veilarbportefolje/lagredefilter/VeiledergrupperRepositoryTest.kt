package no.nav.pto.veilarbportefolje.lagredefilter

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.database.PostgresTable.LAGREDE_FILTER_VEILEDERGRUPPER
import no.nav.pto.veilarbportefolje.lagredefilter.domene.NyVeiledergruppeRequest
import no.nav.pto.veilarbportefolje.lagredefilter.domene.OppdaterVeiledergruppeRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate

@SpringBootTest(classes = [ApplicationConfigTest::class])
class VeiledergrupperRepositoryTest(
    @param:Autowired val veiledergrupperRepository: VeiledergrupperRepository,
    @param:Autowired val jdbcTemplate: JdbcTemplate
) {

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE TABLE ${LAGREDE_FILTER_VEILEDERGRUPPER.TABLE_NAME}")
    }

    @Test
    fun `lagre ny veildergruppe for enhet skal inserte ny rad`() {
        val veiledergruppeRequest = NyVeiledergruppeRequest(
            filterNavn = "Test Gruppe",
            veiledere = listOf("veileder1", "veileder2")
        )

        val lagretGruppe = veiledergrupperRepository.lagreNyVeiledergruppeForEnhet("1234", veiledergruppeRequest)

        assertThat(lagretGruppe).isNotNull
        assertThat(lagretGruppe.filterNavn).isEqualTo("Test Gruppe")
        assertThat(lagretGruppe.veiledere).isEqualTo(listOf("veileder1", "veileder2"))
    }


    @Test
    fun `hente veiledergrupper for enhet skal hente alle rader fra enhet`() {
        val enhet1 = "1111"
        val enhet2 = "2222"
        val veiledergruppeRequest1 = NyVeiledergruppeRequest(
            filterNavn = "Test Gruppe 1",
            veiledere = listOf("veileder1", "veileder2")
        )
        val veiledergruppeRequest2 = NyVeiledergruppeRequest(
            filterNavn = "Test Gruppe 2",
            veiledere = listOf("veileder5", "veileder6")
        )
        val veiledergruppeRequest3 = NyVeiledergruppeRequest(
            filterNavn = "Test Gruppe 3",
            veiledere = listOf("veileder8", "veileder9")
        )

        veiledergrupperRepository.lagreNyVeiledergruppeForEnhet(enhet1, veiledergruppeRequest1)
        veiledergrupperRepository.lagreNyVeiledergruppeForEnhet(enhet1, veiledergruppeRequest2)
        veiledergrupperRepository.lagreNyVeiledergruppeForEnhet(enhet2, veiledergruppeRequest3)

        val lagretGruppePåEnhet1 = veiledergrupperRepository.hentVeiledergrupperForEnhet(enhet1)
        val lagretGruppePåEnhet2 = veiledergrupperRepository.hentVeiledergrupperForEnhet(enhet2)

        assertThat(lagretGruppePåEnhet1).isNotNull
        assertThat(lagretGruppePåEnhet1.size).isEqualTo(2)
        assertThat(lagretGruppePåEnhet1.first().filterNavn).isEqualTo("Test Gruppe 1")
        assertThat(lagretGruppePåEnhet1.first().veiledere).isEqualTo(listOf("veileder1", "veileder2"))

        assertThat(lagretGruppePåEnhet2).isNotNull
        assertThat(lagretGruppePåEnhet2.size).isEqualTo(1)
        assertThat(lagretGruppePåEnhet2.first().filterNavn).isEqualTo("Test Gruppe 3")
        assertThat(lagretGruppePåEnhet2.first().veiledere).isEqualTo(listOf("veileder8", "veileder9"))

    }

    @Test
    fun `oppdater veiledergruppe skal oppdatere filterNavn og veiledere for eksisterende rad`() {
        val enhetId = "1234"
        val lagretGruppe = veiledergrupperRepository.lagreNyVeiledergruppeForEnhet(
            enhetId,
            NyVeiledergruppeRequest(
                filterNavn = "Original navn",
                veiledere = listOf("veileder1", "veileder2")
            )
        )

        val oppdatertGruppe = veiledergrupperRepository.oppdaterVeiledergruppeForEnhet(
            enhetId,
            OppdaterVeiledergruppeRequest(
                filterId = lagretGruppe.filterId,
                filterNavn = "Oppdatert navn",
                veiledere = listOf("veileder3", "veileder4", "veileder5")
            )
        )

        assertThat(oppdatertGruppe.filterId).isEqualTo(lagretGruppe.filterId)
        assertThat(oppdatertGruppe.filterNavn).isEqualTo("Oppdatert navn")
        assertThat(oppdatertGruppe.veiledere).isEqualTo(listOf("veileder3", "veileder4", "veileder5"))

        val hentetEtterOppdatering = veiledergrupperRepository.hentVeiledergrupperForEnhet(enhetId)
        assertThat(hentetEtterOppdatering).hasSize(1)
        assertThat(hentetEtterOppdatering.first().filterNavn).isEqualTo("Oppdatert navn")
        assertThat(hentetEtterOppdatering.first().veiledere).isEqualTo(listOf("veileder3", "veileder4", "veileder5"))
    }

    @Test
    fun `oppdater veiledergruppe skal ikke oppdatere rad som tilhoerer en annen enhet`() {
        val enhet1 = "1111"
        val enhet2 = "2222"
        val lagretGruppePåEnhet1 = veiledergrupperRepository.lagreNyVeiledergruppeForEnhet(
            enhet1,
            NyVeiledergruppeRequest(
                filterNavn = "Enhet1 gruppe",
                veiledere = listOf("veileder1")
            )
        )

        assertThatThrownBy {
            veiledergrupperRepository.oppdaterVeiledergruppeForEnhet(
                enhet2,
                OppdaterVeiledergruppeRequest(
                    filterId = lagretGruppePåEnhet1.filterId,
                    filterNavn = "Forsoek paa oppdatering",
                    veiledere = listOf("veileder99")
                )
            )
        }.isInstanceOf(NoSuchElementException::class.java)

        val uendretGruppe = veiledergrupperRepository.hentVeiledergrupperForEnhet(enhet1).first()
        assertThat(uendretGruppe.filterNavn).isEqualTo("Enhet1 gruppe")
        assertThat(uendretGruppe.veiledere).isEqualTo(listOf("veileder1"))
    }

    @Test
    fun `oppdater veiledergruppe skal kaste feil naar filterId ikke finnes`() {
        assertThatThrownBy {
            veiledergrupperRepository.oppdaterVeiledergruppeForEnhet(
                "1234",
                OppdaterVeiledergruppeRequest(
                    filterId = 999999,
                    filterNavn = "Finnes ikke",
                    veiledere = listOf("veileder1")
                )
            )
        }.isInstanceOf(NoSuchElementException::class.java)
    }

}
