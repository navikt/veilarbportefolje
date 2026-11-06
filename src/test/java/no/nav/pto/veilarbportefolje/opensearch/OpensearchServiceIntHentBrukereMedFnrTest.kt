package no.nav.pto.veilarbportefolje.opensearch

import no.nav.pto.veilarbportefolje.opensearch.OpensearchConfig.BRUKERINDEKS_ALIAS
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.OpensearchTestClient
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OpensearchServiceIntHentBrukereMedFnrTest @Autowired constructor(
    private val opensearchService: OpensearchService,
) : EndToEndTest() {

    @Test
    fun skal_hente_brukere_under_oppfolging_med_gitte_fnr() {
        val enhet = randomNavKontor().value
        val bruker1 = lagBruker(enhet)
        val bruker2 = lagBruker(enhet)
        val ikkeEtterspurt = lagBruker(enhet)
        val ikkeUnderOppfolging = lagBruker(enhet).copy(oppfolging = false)
        val brukere = listOf(bruker1, bruker2, ikkeEtterspurt, ikkeUnderOppfolging)

        opensearchIndexer.skrivBulkTilIndeks(BRUKERINDEKS_ALIAS, brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        val resultat = opensearchService.hentBrukereMedFnr(
            listOf(bruker1.fnr!!, bruker2.fnr!!, ikkeUnderOppfolging.fnr!!, randomFnr().toString())
        )

        assertThat(resultat.map { it.fnr }).containsExactlyInAnyOrder(bruker1.fnr, bruker2.fnr)
    }

    @Test
    fun skal_returnere_tom_liste_naar_ingen_fnr() {
        assertThat(opensearchService.hentBrukereMedFnr(emptyList())).isEmpty()
    }

    private fun lagBruker(enhet: String) = PortefoljebrukerOpensearchModell(
        aktoer_id = randomAktorId().get(),
        fnr = randomFnr().toString(),
        oppfolging = true,
        enhet_id = enhet,
    )
}
