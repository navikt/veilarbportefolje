package no.nav.pto.veilarbportefolje.beslutteroversikt

import no.nav.pto.veilarbportefolje.api.beslutteroversikt.BeslutteroversiktService
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class BeslutteroversiktServiceTest {

    private val opensearchService: OpensearchService = mock(OpensearchService::class.java)
    private val service = BeslutteroversiktService(opensearchService)

    @Test
    fun skal_returnere_etiketter_for_brukere_som_finnes() {
        val egenAnsattBruker = PortefoljebrukerOpensearchModell(fnr = randomFnr().get(), egen_ansatt = true)
        val kode7Bruker = PortefoljebrukerOpensearchModell(fnr = randomFnr().get(), diskresjonskode = "7")
        val brukerSomIkkeFinnes = randomFnr().get()
        val fnrListe = listOf(egenAnsattBruker.fnr!!, kode7Bruker.fnr!!, brukerSomIkkeFinnes)

        `when`(opensearchService.hentBrukereMedFnr(fnrListe)).thenReturn(
            mapOf(egenAnsattBruker.fnr!! to egenAnsattBruker, kode7Bruker.fnr!! to kode7Bruker)
        )

        val resultat = service.hentEtiketter(fnrListe).associateBy { it.fnr }

        assertThat(resultat.keys).containsExactlyInAnyOrder(egenAnsattBruker.fnr, kode7Bruker.fnr)
        assertThat(resultat.getValue(egenAnsattBruker.fnr!!).etiketter.egenAnsatt).isTrue()
        assertThat(resultat.getValue(kode7Bruker.fnr!!).etiketter.adressebeskyttelse).isEqualTo("Fortrolig")
    }
}
