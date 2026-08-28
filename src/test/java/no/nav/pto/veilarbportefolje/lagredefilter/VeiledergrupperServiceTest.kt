package no.nav.pto.veilarbportefolje.lagredefilter

import no.nav.common.types.identer.EnhetId
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient
import no.nav.pto.veilarbportefolje.database.PostgresTable.LAGREDE_FILTER_VEILEDERGRUPPER
import no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper.VeiledergrupperRepository
import no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper.VeiledergrupperService
import no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper.domene.NyVeiledergruppeRequest
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.reset
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

internal class VeiledergrupperServiceTest @Autowired constructor(
    private val veiledergrupperService: VeiledergrupperService,
    private val veiledergrupperRepository: VeiledergrupperRepository,
    private val veilarbVeilederClient: VeilarbVeilederClient,
    private val jdbcTemplate: JdbcTemplate,
) : EndToEndTest() {

    private val enhetId = "0123"

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE TABLE ${LAGREDE_FILTER_VEILEDERGRUPPER.TABLE_NAME}")
        reset(veilarbVeilederClient)
    }

    @Test
    fun `fjerner veiledere som ikke lenger er aktive fra veiledergruppe`() {
        val gruppe = veiledergrupperRepository.lagreNyVeiledergruppeForEnhet(
            enhetId,
            NyVeiledergruppeRequest(filterNavn = "Gruppe 1", veiledere = listOf("Z1", "Z2", "Z3"))
        )
        `when`(veilarbVeilederClient.hentVeilederePaaEnhetMachineToMachine(EnhetId.of(enhetId)))
            .thenReturn(listOf("Z1", "Z3"))

        veiledergrupperService.slettVeiledereSomIkkeErAktiveForHverEnhet()

        val grupperEtter = veiledergrupperRepository.hentVeiledergrupperForEnhet(enhetId)
        assertThat(grupperEtter).hasSize(1)
        assertThat(grupperEtter.first().filterId).isEqualTo(gruppe.filterId)
        assertThat(grupperEtter.first().veiledere).containsExactlyInAnyOrder("Z1", "Z3")
    }

    @Test
    fun `sletter hele veiledergruppen dersom ingen veiledere lenger er aktive`() {
        veiledergrupperRepository.lagreNyVeiledergruppeForEnhet(
            enhetId,
            NyVeiledergruppeRequest(filterNavn = "Gruppe 1", veiledere = listOf("Z1", "Z2"))
        )
        `when`(veilarbVeilederClient.hentVeilederePaaEnhetMachineToMachine(EnhetId.of(enhetId)))
            .thenReturn(listOf("Z9"))

        veiledergrupperService.slettVeiledereSomIkkeErAktiveForHverEnhet()

        assertThat(veiledergrupperRepository.hentVeiledergrupperForEnhet(enhetId)).isEmpty()
    }

    @Test
    fun `lar veiledergruppe være uendret dersom alle veiledere fortsatt er aktive`() {
        veiledergrupperRepository.lagreNyVeiledergruppeForEnhet(
            enhetId,
            NyVeiledergruppeRequest(filterNavn = "Gruppe 1", veiledere = listOf("Z1", "Z2"))
        )
        `when`(veilarbVeilederClient.hentVeilederePaaEnhetMachineToMachine(EnhetId.of(enhetId)))
            .thenReturn(listOf("Z1", "Z2", "Z3"))

        veiledergrupperService.slettVeiledereSomIkkeErAktiveForHverEnhet()

        val grupperEtter = veiledergrupperRepository.hentVeiledergrupperForEnhet(enhetId)
        assertThat(grupperEtter).hasSize(1)
        assertThat(grupperEtter.first().veiledere).containsExactlyInAnyOrder("Z1", "Z2")
    }

    @Test
    fun `hopper over enhet dersom klienten ikke returnerer noen aktive veiledere`() {
        veiledergrupperRepository.lagreNyVeiledergruppeForEnhet(
            enhetId,
            NyVeiledergruppeRequest(filterNavn = "Gruppe 1", veiledere = listOf("Z1", "Z2"))
        )
        `when`(veilarbVeilederClient.hentVeilederePaaEnhetMachineToMachine(EnhetId.of(enhetId)))
            .thenReturn(emptyList())

        veiledergrupperService.slettVeiledereSomIkkeErAktiveForHverEnhet()

        val grupperEtter = veiledergrupperRepository.hentVeiledergrupperForEnhet(enhetId)
        assertThat(grupperEtter).hasSize(1)
        assertThat(grupperEtter.first().veiledere).containsExactlyInAnyOrder("Z1", "Z2")
    }

    @Test
    fun `rydder opp i veiledergrupper på tvers av flere enheter`() {
        val enhetA = "1111"
        val enhetB = "2222"
        veiledergrupperRepository.lagreNyVeiledergruppeForEnhet(
            enhetA,
            NyVeiledergruppeRequest(filterNavn = "Gruppe A", veiledere = listOf("A1", "A2"))
        )
        veiledergrupperRepository.lagreNyVeiledergruppeForEnhet(
            enhetB,
            NyVeiledergruppeRequest(filterNavn = "Gruppe B", veiledere = listOf("B1", "B2"))
        )
        `when`(veilarbVeilederClient.hentVeilederePaaEnhetMachineToMachine(EnhetId.of(enhetA)))
            .thenReturn(listOf("A1"))
        `when`(veilarbVeilederClient.hentVeilederePaaEnhetMachineToMachine(EnhetId.of(enhetB)))
            .thenReturn(listOf("B9"))

        veiledergrupperService.slettVeiledereSomIkkeErAktiveForHverEnhet()

        val grupperA = veiledergrupperRepository.hentVeiledergrupperForEnhet(enhetA)
        assertThat(grupperA).hasSize(1)
        assertThat(grupperA.first().veiledere).containsExactly("A1")
        assertThat(veiledergrupperRepository.hentVeiledergrupperForEnhet(enhetB)).isEmpty()
    }

    @Test
    fun `fortsetter behandling av øvrige enheter selv om en enhet feiler`() {
        val enhetSomFeiler = "1111"
        val enhetSomLykkes = "2222"
        veiledergrupperRepository.lagreNyVeiledergruppeForEnhet(
            enhetSomFeiler,
            NyVeiledergruppeRequest(filterNavn = "Gruppe A", veiledere = listOf("A1", "A2"))
        )
        veiledergrupperRepository.lagreNyVeiledergruppeForEnhet(
            enhetSomLykkes,
            NyVeiledergruppeRequest(filterNavn = "Gruppe B", veiledere = listOf("B1", "B2"))
        )
        `when`(veilarbVeilederClient.hentVeilederePaaEnhetMachineToMachine(EnhetId.of(enhetSomFeiler)))
            .thenThrow(RuntimeException("Klientfeil"))
        `when`(veilarbVeilederClient.hentVeilederePaaEnhetMachineToMachine(EnhetId.of(enhetSomLykkes)))
            .thenReturn(listOf("B1"))

        veiledergrupperService.slettVeiledereSomIkkeErAktiveForHverEnhet()

        val grupperSomFeilet = veiledergrupperRepository.hentVeiledergrupperForEnhet(enhetSomFeiler)
        assertThat(grupperSomFeilet).hasSize(1)
        assertThat(grupperSomFeilet.first().veiledere).containsExactlyInAnyOrder("A1", "A2")

        val grupperSomLykkes = veiledergrupperRepository.hentVeiledergrupperForEnhet(enhetSomLykkes)
        assertThat(grupperSomLykkes).hasSize(1)
        assertThat(grupperSomLykkes.first().veiledere).containsExactly("B1")
    }
}
