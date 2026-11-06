package no.nav.pto.veilarbportefolje.beslutteroversikt

import no.nav.common.auth.context.AuthContext
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.auth.context.UserRole
import no.nav.common.utils.fn.UnsafeSupplier
import no.nav.pto.veilarbportefolje.auth.AuthService
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.util.TestDataUtils.generateJWT
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class BeslutteroversiktServiceTest {

    private val opensearchService: OpensearchService = mock(OpensearchService::class.java)
    private val authService: AuthService = mock(AuthService::class.java)
    private val service = BeslutteroversiktService(opensearchService, authService)

    @Test
    fun skal_kun_returnere_brukere_paa_enheter_veileder_har_tilgang_til() {
        val veilederId = "Z123456"
        val brukerMedTilgang = PortefoljebrukerOpensearchModell(fnr = randomFnr().get(), enhet_id = "1234", egen_ansatt = true)
        val brukerUtenTilgang = PortefoljebrukerOpensearchModell(fnr = randomFnr().get(), enhet_id = "5678")
        val brukerUtenEnhet = PortefoljebrukerOpensearchModell(fnr = randomFnr().get(), enhet_id = null)
        val fnrListe = listOf(brukerMedTilgang.fnr!!, brukerUtenTilgang.fnr!!, brukerUtenEnhet.fnr!!)

        `when`(opensearchService.hentBrukereMedFnr(fnrListe))
            .thenReturn(listOf(brukerMedTilgang, brukerUtenTilgang, brukerUtenEnhet))
        `when`(authService.harVeilederTilgangTilEnhet(veilederId, "1234")).thenReturn(true)
        `when`(authService.harVeilederTilgangTilEnhet(veilederId, "5678")).thenReturn(false)

        val resultat = AuthContextHolderThreadLocal.instance().withContext(
            AuthContext(UserRole.INTERN, generateJWT(veilederId)),
            UnsafeSupplier { service.hentEtiketter(fnrListe) }
        )

        assertThat(resultat.map { it.fnr }).containsExactly(brukerMedTilgang.fnr)
        assertThat(resultat.single().etiketter.egenAnsatt).isTrue()
    }
}
