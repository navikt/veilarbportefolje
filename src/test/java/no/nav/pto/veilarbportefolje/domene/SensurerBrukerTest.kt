package no.nav.pto.veilarbportefolje.domene

import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.poao_tilgang.client.Decision.Deny
import no.nav.poao_tilgang.client.Decision.Permit
import no.nav.pto.veilarbportefolje.auth.AuthService
import no.nav.pto.veilarbportefolje.auth.PoaoTilgangWrapper
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModell
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`


class SensurerBrukerTest {
    private lateinit var authService: AuthService
    private val azureAdOnBehalfOfTokenClient: AzureAdOnBehalfOfTokenClient = mock()
    private val azureAdMachineToMachineTokenClient: AzureAdMachineToMachineTokenClient = mock()
    private val poaoTilgangWrapper: PoaoTilgangWrapper = mock()

    @Before
    fun setUp() {
        authService = AuthService(
            azureAdOnBehalfOfTokenClient,
            azureAdMachineToMachineTokenClient,
            poaoTilgangWrapper
        )
    }

    @Test
    fun skalIkkeSeKode6Bruker() {
        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Deny("", ""))
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(kode6Bruker())
        sjekkAtKonfidensiellDataErVasket(filtrerteBrukere)
    }

    @Test
    fun skalIkkeSeKode7Bruker() {
        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(Deny("", ""))
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(kode7Bruker())
        sjekkAtKonfidensiellDataErVasket(filtrerteBrukere)
    }

    @Test
    fun skalIkkeSeEgenAnsatt() {
        `when`(poaoTilgangWrapper.harVeilederTilgangTilEgenAnsatt()).thenReturn(Deny("", ""))
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(egenAnsatt())
        sjekkAtKonfidensiellDataErVasket(filtrerteBrukere)
    }

    @Test
    fun skalSeKode6Bruker() {
        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Permit)
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(kode6Bruker())
        assertThat(filtrerteBrukere.fnr).isEqualTo("11111111111")
        assertThat(filtrerteBrukere.fornavn).isEqualTo("fornavnKode6")
        assertThat(filtrerteBrukere.etternavn).isEqualTo("etternanvKode6")
    }

    @Test
    fun skalSeKode7Bruker() {
        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(Permit)
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(kode7Bruker())
        assertThat(filtrerteBrukere.fnr).isEqualTo("11111111111")
        assertThat(filtrerteBrukere.fornavn).isEqualTo("fornavnKode7")
        assertThat(filtrerteBrukere.etternavn).isEqualTo("etternanvKode7")
    }

    @Test
    fun skalSeEgenAnsatt() {
        `when`(poaoTilgangWrapper.harVeilederTilgangTilEgenAnsatt()).thenReturn(Permit)
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(egenAnsatt())
        assertThat(filtrerteBrukere.fnr).isEqualTo("11111111111")
        assertThat(filtrerteBrukere.fornavn).isEqualTo("fornavnKodeEgenAnsatt")
        assertThat(filtrerteBrukere.etternavn).isEqualTo("etternanvEgenAnsatt")
    }

    @Test
    fun skalSeIkkeKonfidensiellBruker() {
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(ikkeKonfidensiellBruker())
        assertThat(filtrerteBrukere.fnr).isEqualTo("11111111111")
        assertThat(filtrerteBrukere.fornavn).isEqualTo("fornavnIkkeKonfidensiellBruker")
        assertThat(filtrerteBrukere.etternavn).isEqualTo("etternanvIkkeKonfidensiellBruker")
    }

    @Test
    fun skalIkkeSeKode6Barn() {
        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Deny("", ""))
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(brukerMedKode6Barn())
        sjekkAtBarnMedKode6ErFjernet(filtrerteBrukere)
    }

    @Test
    fun skalIkkeSeKode7Barn() {
        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(Deny("", ""))
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(brukerMedKode7Barn())
        sjekkAtBarnMedKode7ErFjernet(filtrerteBrukere)
    }

    @Test
    fun skalFjerneKode7BarnMenIkkeKode6() {
        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Permit)
        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(Deny("", ""))
        val filtrertBruker = authService.fjernKonfidensiellInfoDersomIkkeTilgang(brukerMedKode6og7Barn())
        sjekkAtBarnMedKode7ErFjernet(filtrertBruker)
        sjekkAtBarnMedKode6IkkeErFjernet(filtrertBruker)
        assertThat(filtrertBruker.barnUnder18AarData).hasSize(2)
    }

    @Test
    fun skalIkkeSeKode19Barn() {
        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Deny("", ""))
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(brukerMedKode19Barn())
        sjekkAtBarnMedKode19ErFjernet(filtrerteBrukere)
    }

    @Test
    fun skalSeKode19Barn() {
        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Permit)
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(brukerMedKode19Barn())
        sjekkAtBarnMedKode19ErIkkeFjernet(filtrerteBrukere)
    }


    private fun sjekkAtKonfidensiellDataErVasket(bruker: PortefoljebrukerFrontendModell) {
        assertThat(bruker.fnr).isEqualTo("")
        assertThat(bruker.etternavn).isEqualTo("")
        assertThat(bruker.fornavn).isEqualTo("")
        assertThat(bruker.skjermetTil).isNull()
        assertThat(bruker.foedeland).isNull()
        assertThat(bruker.tolkebehov?.talespraaktolk).isEqualTo("")
        assertThat(bruker.tolkebehov?.tegnspraaktolk).isEqualTo("")
        assertThat(bruker.hovedStatsborgerskap).isNull()
        assertThat(bruker.bostedBydel).isNull()
        assertThat(bruker.bostedKommune).isNull()
        assertThat(bruker.harUtelandsAddresse).isEqualTo(false)
    }

    private fun sjekkAtBarnMedKode6ErFjernet(bruker: PortefoljebrukerFrontendModell) {
        assertThat(bruker.barnUnder18AarData)
            .allSatisfy { barn ->
                assertThat(barn.diskresjonskode).isNotEqualTo("6")
            }
    }

    private fun sjekkAtBarnMedKode7ErFjernet(bruker: PortefoljebrukerFrontendModell) {
        assertThat(bruker.barnUnder18AarData)
            .allSatisfy { barn ->
                assertThat(barn.diskresjonskode).isNotEqualTo("7")
            }
    }

    private fun sjekkAtBarnMedKode6IkkeErFjernet(bruker: PortefoljebrukerFrontendModell) {
        assertThat(bruker.barnUnder18AarData).anySatisfy { barn ->
            if (barn.diskresjonskode != null) {
                assertThat(barn.diskresjonskode).isEqualTo("6")
            }
        }
    }

    private fun sjekkAtBarnMedKode19ErFjernet(bruker: PortefoljebrukerFrontendModell) {
        assertThat(bruker.barnUnder18AarData).allSatisfy { barn ->
            assertThat(barn.diskresjonskode).isNotEqualTo("19")
        }
    }

    private fun sjekkAtBarnMedKode19ErIkkeFjernet(bruker: PortefoljebrukerFrontendModell) {
        assertThat(bruker.barnUnder18AarData).anySatisfy { barn ->
            assertThat(barn.diskresjonskode).isEqualTo("19")
        }
    }

    private fun kode6Bruker(): PortefoljebrukerFrontendModell {
        return frontendbrukerDefaults.copy(
            fnr = "11111111111",
            fornavn = "fornavnKode6",
            etternavn = "etternanvKode6",
            diskresjonskode = "6",
            barnUnder18AarData = emptyList()
        )
    }

    private fun kode7Bruker(): PortefoljebrukerFrontendModell =
        frontendbrukerDefaults.copy(
            fnr = "11111111111",
            fornavn = "fornavnKode7",
            etternavn = "etternanvKode7",
            diskresjonskode = "7",
            barnUnder18AarData = emptyList()
        )

    private fun egenAnsatt(): PortefoljebrukerFrontendModell =
        frontendbrukerDefaults.copy(
            fnr = "11111111111",
            fornavn = "fornavnKodeEgenAnsatt",
            etternavn = "etternanvEgenAnsatt",
            egenAnsatt = true
        )

    private fun ikkeKonfidensiellBruker(): PortefoljebrukerFrontendModell =
        frontendbrukerDefaults.copy(
            fnr = "11111111111",
            fornavn = "fornavnIkkeKonfidensiellBruker",
            etternavn = "etternanvIkkeKonfidensiellBruker"
        )

    private fun brukerMedKode6Barn(): PortefoljebrukerFrontendModell =
        frontendbrukerDefaults.copy(
            fnr = "11111111111",
            barnUnder18AarData = listOf(
                BarnUnder18AarData(15, "6"),
                BarnUnder18AarData(12, "6")
            )
        )

    private fun brukerMedKode7Barn(): PortefoljebrukerFrontendModell =
        frontendbrukerDefaults.copy(
            fnr = "11111111111",
            barnUnder18AarData = listOf(
                BarnUnder18AarData(1, "7")
            )
        )

    private fun brukerMedKode19Barn(): PortefoljebrukerFrontendModell =
        frontendbrukerDefaults.copy(
            fnr = "11111111111",
            barnUnder18AarData = listOf(
                BarnUnder18AarData(15, "19"),
                BarnUnder18AarData(12, null),
                BarnUnder18AarData(3, null)
            )
        )

    private fun brukerMedKode6og7Barn(): PortefoljebrukerFrontendModell =
        frontendbrukerDefaults.copy(
            fnr = "11111111111",
            barnUnder18AarData = listOf(
                BarnUnder18AarData(11, "6"),
                BarnUnder18AarData(15, "7"),
                BarnUnder18AarData(3, null)
            )
        )
}
