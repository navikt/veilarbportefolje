package no.nav.pto.veilarbportefolje.opensearch

import no.nav.pto.veilarbportefolje.opensearch.OpensearchConfig.BRUKERINDEKS_ALIAS
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe
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
    fun skal_hente_brukere_med_gitte_fnr() {
        val enhet = randomNavKontor().value
        val bruker1 = lagBruker(enhet)
        val bruker2 = lagBruker(enhet)
        val ikkeEtterspurt = lagBruker(enhet)
        val brukere = listOf(bruker1, bruker2, ikkeEtterspurt)
        brukere.forEach { lagreIdenter(it) }

        opensearchIndexer.skrivBulkTilIndeks(BRUKERINDEKS_ALIAS, brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        val resultat = opensearchService.hentBrukereMedFnr(
            listOf(bruker1.fnr!!, bruker2.fnr!!, randomFnr().toString())
        )

        assertThat(resultat.keys).containsExactlyInAnyOrder(bruker1.fnr, bruker2.fnr)
        assertThat(resultat.getValue(bruker1.fnr!!).aktoer_id).isEqualTo(bruker1.aktoer_id)
        assertThat(resultat.getValue(bruker2.fnr!!).aktoer_id).isEqualTo(bruker2.aktoer_id)
    }

    @Test
    fun skal_hente_bruker_naar_etterspurt_fnr_er_historisk() {
        val bruker = lagBruker(randomNavKontor().value)
        val historiskFnr = randomFnr().toString()
        lagreIdenter(bruker, historiskeFnr = listOf(historiskFnr))

        opensearchIndexer.skrivBulkTilIndeks(BRUKERINDEKS_ALIAS, listOf(bruker))
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == 1 }

        val resultat = opensearchService.hentBrukereMedFnr(listOf(historiskFnr))

        assertThat(resultat.keys).containsExactly(historiskFnr)
        assertThat(resultat.getValue(historiskFnr).aktoer_id).isEqualTo(bruker.aktoer_id)
    }

    @Test
    fun skal_hente_bruker_naar_indeksert_fnr_er_historisk() {
        val historiskFnr = randomFnr().toString()
        val aktivtFnr = randomFnr().toString()
        val bruker = lagBruker(randomNavKontor().value).copy(fnr = historiskFnr)
        pdlIdentRepository.upsertIdenter(
            listOf(
                PDLIdent(bruker.aktoer_id, false, Gruppe.AKTORID),
                PDLIdent(aktivtFnr, false, Gruppe.FOLKEREGISTERIDENT),
                PDLIdent(historiskFnr, true, Gruppe.FOLKEREGISTERIDENT),
            )
        )

        opensearchIndexer.skrivBulkTilIndeks(BRUKERINDEKS_ALIAS, listOf(bruker))
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == 1 }

        val resultat = opensearchService.hentBrukereMedFnr(listOf(aktivtFnr))

        assertThat(resultat.keys).containsExactly(aktivtFnr)
        assertThat(resultat.getValue(aktivtFnr).aktoer_id).isEqualTo(bruker.aktoer_id)
    }

    @Test
    fun skal_returnere_tom_liste_naar_ingen_fnr() {
        assertThat(opensearchService.hentBrukereMedFnr(emptyList())).isEmpty()
    }

    private fun lagBruker(enhet: String) = PortefoljebrukerOpensearchModell(
        aktoer_id = randomAktorId().get(),
        fnr = randomFnr().toString(),
        enhet_id = enhet,
    )

    private fun lagreIdenter(bruker: PortefoljebrukerOpensearchModell, historiskeFnr: List<String> = emptyList()) {
        pdlIdentRepository.upsertIdenter(
            listOf(
                PDLIdent(bruker.aktoer_id, false, Gruppe.AKTORID),
                PDLIdent(bruker.fnr, false, Gruppe.FOLKEREGISTERIDENT),
            ) + historiskeFnr.map { PDLIdent(it, true, Gruppe.FOLKEREGISTERIDENT) }
        )
    }
}
