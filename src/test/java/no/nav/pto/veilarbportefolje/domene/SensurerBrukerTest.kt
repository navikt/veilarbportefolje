package no.nav.pto.veilarbportefolje.domene

import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.pto.veilarbportefolje.auth.AuthService
import no.nav.pto.veilarbportefolje.auth.PoaoTilgangWrapper
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModell
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock


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

    // Merk: fjernKonfidensiellInfoDersomIkkeTilgang tar nå de tre tilgangsavgjørelsene
    // (kode6/kode7/egenAnsatt) som parametre i stedet for å slå dem opp selv via
    // poaoTilgangWrapper (se plan.md punkt 1 - memoisering). Testene sender derfor
    // avgjørelsen direkte som true/false (Permit/Deny) i stedet for å mocke
    // poaoTilgangWrapper - metoden under test kaller den ikke lenger. De to andre
    // booleanene er irrelevante der bare én av de tre sjekkene faktisk kan slå inn for
    // en gitt bruker, men sendes med for å holde signaturen komplett og lesbar.

    @Test
    fun skalIkkeSeKode6Bruker() {
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(kode6Bruker(), {false}, {true}, {true})
        sjekkAtKonfidensiellDataErVasket(filtrerteBrukere)
    }

    @Test
    fun skalIkkeSeKode7Bruker() {
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(kode7Bruker(), {true}, {false}, {true})
        sjekkAtKonfidensiellDataErVasket(filtrerteBrukere)
    }

    @Test
    fun skalIkkeSeEgenAnsatt() {
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(egenAnsatt(), {true}, {true}, {false})
        sjekkAtKonfidensiellDataErVasket(filtrerteBrukere)
    }

    @Test
    fun skalSeKode6Bruker() {
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(kode6Bruker(), {true}, {true}, {true})
        assertThat(filtrerteBrukere.fnr).isEqualTo("11111111111")
        assertThat(filtrerteBrukere.fornavn).isEqualTo("fornavnKode6")
        assertThat(filtrerteBrukere.etternavn).isEqualTo("etternanvKode6")
    }

    @Test
    fun skalSeKode7Bruker() {
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(kode7Bruker(), {true}, {true}, {true})
        assertThat(filtrerteBrukere.fnr).isEqualTo("11111111111")
        assertThat(filtrerteBrukere.fornavn).isEqualTo("fornavnKode7")
        assertThat(filtrerteBrukere.etternavn).isEqualTo("etternanvKode7")
    }

    @Test
    fun skalSeEgenAnsatt() {
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(egenAnsatt(), {true}, {true}, {true})
        assertThat(filtrerteBrukere.fnr).isEqualTo("11111111111")
        assertThat(filtrerteBrukere.fornavn).isEqualTo("fornavnKodeEgenAnsatt")
        assertThat(filtrerteBrukere.etternavn).isEqualTo("etternanvEgenAnsatt")
    }

    @Test
    fun skalSeIkkeKonfidensiellBruker() {
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(ikkeKonfidensiellBruker(), {true}, {true}, {true})
        assertThat(filtrerteBrukere.fnr).isEqualTo("11111111111")
        assertThat(filtrerteBrukere.fornavn).isEqualTo("fornavnIkkeKonfidensiellBruker")
        assertThat(filtrerteBrukere.etternavn).isEqualTo("etternanvIkkeKonfidensiellBruker")
    }

    @Test
    fun skalIkkeSeKode6Barn() {
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(brukerMedKode6Barn(), {false}, {true}, {true})
        sjekkAtBarnMedKode6ErFjernet(filtrerteBrukere)
    }

    @Test
    fun skalIkkeSeKode7Barn() {
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(brukerMedKode7Barn(), {true}, {false}, {true})
        sjekkAtBarnMedKode7ErFjernet(filtrerteBrukere)
    }

    @Test
    fun skalFjerneKode7BarnMenIkkeKode6() {
        val filtrertBruker = authService.fjernKonfidensiellInfoDersomIkkeTilgang(brukerMedKode6og7Barn(), {true}, {false}, {true})
        sjekkAtBarnMedKode7ErFjernet(filtrertBruker)
        sjekkAtBarnMedKode6IkkeErFjernet(filtrertBruker)
        assertThat(filtrertBruker.barnUnder18AarData).hasSize(2)
    }

    @Test
    fun skalIkkeSeKode19Barn() {
        // Kode "19" = STRENGT_FORTROLIG_UTLAND, som behandles likt som kode6 i
        // harVeilederTilgangTilBarn - se Adressebeskyttelse.java.
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(brukerMedKode19Barn(), {false}, {true}, {true})
        sjekkAtBarnMedKode19ErFjernet(filtrerteBrukere)
    }

    @Test
    fun skalSeKode19Barn() {
        val filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(brukerMedKode19Barn(), {true}, {true}, {true})
        sjekkAtBarnMedKode19ErIkkeFjernet(filtrerteBrukere)
    }


    private fun sjekkAtKonfidensiellDataErVasket(bruker: PortefoljebrukerFrontendModell) {
        assertThat(bruker.fnr).isEqualTo("")
        assertThat(bruker.etternavn).isEqualTo("")
        assertThat(bruker.fornavn).isEqualTo("")
        assertThat(bruker.skjermetTil).isNull()
        assertThat(bruker.foedeland).isNull()
        assertThat(bruker.tolkebehov.talespraaktolk).isEqualTo("")
        assertThat(bruker.tolkebehov.tegnspraaktolk).isEqualTo("")
        assertThat(bruker.hovedStatsborgerskap).isNull()
        assertThat(bruker.geografiskBosted.bostedBydel).isNull()
        assertThat(bruker.geografiskBosted.bostedKommune).isNull()
        assertThat(bruker.geografiskBosted.bostedKommuneUkjentEllerUtland).isEqualTo("-")
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
            etiketter = frontendbrukerDefaults.etiketter.copy(diskresjonskodeFortrolig = "6"),
            barnUnder18AarData = emptyList()
        )
    }

    private fun kode7Bruker(): PortefoljebrukerFrontendModell =
        frontendbrukerDefaults.copy(
            fnr = "11111111111",
            fornavn = "fornavnKode7",
            etternavn = "etternanvKode7",
            etiketter = frontendbrukerDefaults.etiketter.copy(diskresjonskodeFortrolig = "7"),
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
