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

        val harAktiv = hentBrukere(ArbeidssokerregisteretFilter.HAR_AKTIV_PERIODE_I_ARBEIDSSOEKERREGISTERET)
        val harIkkeAktiv = hentBrukere(ArbeidssokerregisteretFilter.HAR_IKKE_AKTIV_PERIODE_I_ARBEIDSSOEKERREGISTERET)
        val alle = hentBrukere()

        assertThat(harAktiv).containsExactly(aktivPeriode.fnr)
        assertThat(harIkkeAktiv).containsExactlyInAnyOrder(avsluttetPeriode.fnr, ingenPeriode.fnr)
        assertThat(alle).containsExactlyInAnyOrder(aktivPeriode.fnr, avsluttetPeriode.fnr, ingenPeriode.fnr)
    }

    @Test
    fun skal_sortere_paa_periode_startdato() {
        val tidligst = lagBruker(ArbeidssoekerregisteretForOpensearch(periodeStartDato = LocalDate.now().minusMonths(4)))
        val senest = lagBruker(ArbeidssoekerregisteretForOpensearch(periodeStartDato = LocalDate.now().minusMonths(1)))
        val ingenPeriode = lagBruker(null)
        skrivOgVent(listOf(senest, ingenPeriode, tidligst))

        val stigende = hentBrukere(
            sorteringsfelt = Sorteringsfelt.ARBEIDSSOEKERREGISTERET_PERIODE_STARTDATO,
            rekkefolge = Sorteringsrekkefolge.STIGENDE
        )
        val synkende = hentBrukere(
            sorteringsfelt = Sorteringsfelt.ARBEIDSSOEKERREGISTERET_PERIODE_STARTDATO,
            rekkefolge = Sorteringsrekkefolge.SYNKENDE
        )

        assertThat(stigende).containsExactly(tidligst.fnr, senest.fnr, ingenPeriode.fnr)
        assertThat(synkende).containsExactly(ingenPeriode.fnr, senest.fnr, tidligst.fnr)
    }

    @Test
    fun skal_sortere_paa_periode_avsluttetdato() {
        val start = LocalDate.now().minusYears(1)
        val tidligst = lagBruker(
            ArbeidssoekerregisteretForOpensearch(start, periodeAvsluttetDato = LocalDate.now().minusMonths(4))
        )
        val senest = lagBruker(
            ArbeidssoekerregisteretForOpensearch(start, periodeAvsluttetDato = LocalDate.now().minusMonths(1))
        )
        val aktiv = lagBruker(ArbeidssoekerregisteretForOpensearch(start))
        skrivOgVent(listOf(senest, aktiv, tidligst))

        val stigende = hentBrukere(
            sorteringsfelt = Sorteringsfelt.ARBEIDSSOEKERREGISTERET_PERIODE_AVSLUTTETDATO,
            rekkefolge = Sorteringsrekkefolge.STIGENDE
        )
        val synkende = hentBrukere(
            sorteringsfelt = Sorteringsfelt.ARBEIDSSOEKERREGISTERET_PERIODE_AVSLUTTETDATO,
            rekkefolge = Sorteringsrekkefolge.SYNKENDE
        )

        assertThat(stigende).containsExactly(tidligst.fnr, senest.fnr, aktiv.fnr)
        assertThat(synkende).containsExactly(aktiv.fnr, senest.fnr, tidligst.fnr)
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

    private fun hentBrukere(
        filter: ArbeidssokerregisteretFilter? = null,
        sorteringsfelt: Sorteringsfelt = Sorteringsfelt.IKKE_SATT,
        rekkefolge: Sorteringsrekkefolge = Sorteringsrekkefolge.IKKE_SATT
    ): List<String?> =
        opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            rekkefolge,
            sorteringsfelt,
            getFiltervalgDefaults().copy(arbeidssoekerregisteret = filter),
            null,
            null
        ).brukere.map { it.fnr }
}
