package no.nav.pto.veilarbportefolje.opensearch

import no.nav.pto.veilarbportefolje.domene.Sorteringsfelt
import no.nav.pto.veilarbportefolje.domene.Sorteringsrekkefolge
import no.nav.pto.veilarbportefolje.domene.filtervalg.ArbeidssokerregisteretFilter
import no.nav.pto.veilarbportefolje.domene.getFiltervalgDefaults
import no.nav.pto.veilarbportefolje.domene.opensearchmodell.ArbeidssoekerregisteretForOpensearch
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

class OpensearchServiceIntArbeidssoekerregisteretTest @Autowired constructor(
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
    fun skal_filtrere_paa_aktiv_periode_i_arbeidssoekerregisteret() {
        val aktivPeriode = lagBruker(
            ArbeidssoekerregisteretForOpensearch(periodeStartDato = LocalDate.now().minusMonths(2))
        )
        val avsluttetPeriode = lagBruker(
            ArbeidssoekerregisteretForOpensearch(
                periodeStartDato = LocalDate.now().minusMonths(2),
                periodeAvsluttetDato = LocalDate.now().minusDays(3)
            )
        )
        val ingenPeriode = lagBruker(null)
        skrivOgVent(listOf(aktivPeriode, avsluttetPeriode, ingenPeriode))

        val harAktiv = hentBrukere(listOf(ArbeidssokerregisteretFilter.HAR_AKTIV_PERIODE_I_ARBEIDSSOEKERREGISTERET))
        val harIkkeAktiv =
            hentBrukere(listOf(ArbeidssokerregisteretFilter.HAR_IKKE_AKTIV_PERIODE_I_ARBEIDSSOEKERREGISTERET))
        val alle = hentBrukere(ArbeidssokerregisteretFilter.entries)

        assertThat(harAktiv).containsExactly(aktivPeriode.fnr)
        assertThat(harIkkeAktiv).containsExactlyInAnyOrder(avsluttetPeriode.fnr, ingenPeriode.fnr)
        assertThat(alle).containsExactlyInAnyOrder(aktivPeriode.fnr, avsluttetPeriode.fnr, ingenPeriode.fnr)
    }

    private fun lagBruker(arbeidssoekerregisteret: ArbeidssoekerregisteretForOpensearch?) =
        PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            arbeidssoekerregisteret = arbeidssoekerregisteret
        )

    private fun skrivOgVent(brukere: List<PortefoljebrukerOpensearchModell>) {
        opensearchIndexer.skrivBulkTilIndeks(BRUKERINDEKS_ALIAS, brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }
    }

    private fun hentBrukere(filter: List<ArbeidssokerregisteretFilter>): List<String?> =
        opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            getFiltervalgDefaults().copy(arbeidssoekerregisteret = filter),
            null,
            null
        ).brukere.map { it.fnr }
}
