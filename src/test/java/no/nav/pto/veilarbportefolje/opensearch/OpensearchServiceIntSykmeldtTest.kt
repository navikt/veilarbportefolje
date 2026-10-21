package no.nav.pto.veilarbportefolje.opensearch

import no.nav.pto.veilarbportefolje.domene.Sorteringsfelt
import no.nav.pto.veilarbportefolje.domene.Sorteringsrekkefolge
import no.nav.pto.veilarbportefolje.domene.filtervalg.Sykmeldt
import no.nav.pto.veilarbportefolje.domene.getFiltervalgDefaults
import no.nav.pto.veilarbportefolje.domene.opensearchmodell.SykmeldtForOpensearch
import no.nav.pto.veilarbportefolje.opensearch.OpensearchConfig.BRUKERINDEKS_ALIAS
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.OpensearchTestClient
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomVeilederId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.*

class OpensearchServiceIntSykmeldtTest @Autowired constructor(
    private val opensearchService: OpensearchService,
) : EndToEndTest() {

    private lateinit var TEST_ENHET: String
    private lateinit var TEST_VEILEDER_0: String

    @BeforeEach
    fun setup() {
        TEST_ENHET = randomNavKontor().value
        TEST_VEILEDER_0 = randomVeilederId().value!!
    }

    @Test
    fun skal_hente_ut_brukere_som_er_sykmeldt() {
        val brukerSomErSykmeldt = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            sykmeldt = SykmeldtForOpensearch(
                startdato = LocalDate.now().minusWeeks(2),
                sluttdato = LocalDate.now().plusWeeks(2),
                harArbeidsgiver = true
            )
        )

        val brukerSomIkkeErSykmeldt = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            sykmeldt = null,
        )

        val liste = listOf(brukerSomErSykmeldt, brukerSomIkkeErSykmeldt)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = getFiltervalgDefaults().copy(
            sykmeldt = listOf(Sykmeldt.ER_SYKMELDT)
        )

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        assertThat(response.antall).isEqualTo(1)
        assertThat(response.brukere).extracting<String> { it.fnr }.contains(brukerSomErSykmeldt.fnr)
        assertThat(response.brukere).extracting<String> { it.fnr }.doesNotContain(brukerSomIkkeErSykmeldt.fnr)
    }

    private fun skrivBrukereTilTestindeks(brukere: List<PortefoljebrukerOpensearchModell>) {
        opensearchIndexer.skrivBulkTilIndeks(BRUKERINDEKS_ALIAS, brukere)
    }
}
