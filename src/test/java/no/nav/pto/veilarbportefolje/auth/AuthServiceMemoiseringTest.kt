package no.nav.pto.veilarbportefolje.auth

import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.poao_tilgang.client.Decision
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModellMapper
import no.nav.pto.veilarbportefolje.domene.getFiltervalgDefaults
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.persononinfo.domene.Adressebeskyttelse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

/**
 * Verifiserer memoiseringen av kode6/kode7/egenAnsatt-sjekk i [AuthService.sensurerBrukere]
 * (plan.md punkt 1): harVeilederTilgangTilKode6()/Kode7()/EgenAnsatt() på
 * [PoaoTilgangWrapper] avhenger kun av innlogget veileder, ikke av hvilken bruker som
 * sjekkes - og skal derfor kalles maks én gang per kall til sensurerBrukere(), uansett
 * hvor mange (skjermede) brukere lista inneholder.
 */
@ExtendWith(MockitoExtension::class)
class AuthServiceMemoiseringTest {

    @Mock
    lateinit var aadOboTokenClient: AzureAdOnBehalfOfTokenClient

    @Mock
    lateinit var aadM2MTokenClient: AzureAdMachineToMachineTokenClient

    @Mock
    lateinit var poaoTilgangWrapper: PoaoTilgangWrapper

    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        authService = AuthService(aadOboTokenClient, aadM2MTokenClient, poaoTilgangWrapper)
    }

    private fun brukerMedDiskresjonskode(diskresjonskode: String?) =
        PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = PortefoljebrukerOpensearchModell(diskresjonskode = diskresjonskode),
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )

    private fun egenAnsattBruker() =
        PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = PortefoljebrukerOpensearchModell(egen_ansatt = true),
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )

    @Test
    fun `skal kun kalle poao-tilgang for kode6-sjekk en gang uansett listestorrelse`() {
        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Decision.Deny("", ""))

        val femtenSkjermedeBrukere = (1..15).map {
            brukerMedDiskresjonskode(Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode)
        }

        authService.sensurerBrukere(femtenSkjermedeBrukere)

        verify(poaoTilgangWrapper, times(1)).harVeilederTilgangTilKode6()
    }

    @Test
    fun `skal kun kalle poao-tilgang for kode7-sjekk en gang uansett listestorrelse`() {
        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(Decision.Deny("", ""))

        val femtenSkjermedeBrukere = (1..15).map {
            brukerMedDiskresjonskode(Adressebeskyttelse.FORTROLIG.diskresjonskode)
        }

        authService.sensurerBrukere(femtenSkjermedeBrukere)

        verify(poaoTilgangWrapper, times(1)).harVeilederTilgangTilKode7()
    }

    @Test
    fun `skal kun kalle poao-tilgang for egenAnsatt-sjekk en gang uansett listestorrelse`() {
        `when`(poaoTilgangWrapper.harVeilederTilgangTilEgenAnsatt()).thenReturn(Decision.Deny("", ""))

        val femtenEgenAnsatteBrukere = (1..15).map { egenAnsattBruker() }

        authService.sensurerBrukere(femtenEgenAnsatteBrukere)

        verify(poaoTilgangWrapper, times(1)).harVeilederTilgangTilEgenAnsatt()
    }

    @Test
    fun `skal fortsatt gi riktig sensurering per bruker etter memoisering (ingen endring i semantikk)`() {
        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Decision.Deny("", ""))

        val strengtFortroligBruker = brukerMedDiskresjonskode(Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode)
        val ugradertBruker = brukerMedDiskresjonskode(Adressebeskyttelse.UGRADERT.diskresjonskode)

        val sensurert = authService.sensurerBrukere(listOf(strengtFortroligBruker, ugradertBruker))

        // Memoisering skal ikke endre HVEM som blir sensurert, kun hvor mange ganger vi
        // spør poao-tilgang: den strengt fortrolige brukeren skal fortsatt få fornavnet
        // sitt fjernet (deny), mens den ugraderte brukeren skal være uendret.
        org.junit.jupiter.api.Assertions.assertEquals("", sensurert[0].fornavn)
        org.junit.jupiter.api.Assertions.assertEquals(ugradertBruker.fornavn, sensurert[1].fornavn)
    }
}
