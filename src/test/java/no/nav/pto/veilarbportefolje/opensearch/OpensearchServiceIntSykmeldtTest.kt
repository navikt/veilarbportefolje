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
    fun skal_hente_ut_brukere_som_er_sykmeldt_med_og_uten_arbeidsgiver() {
        val medArbeidsgiver = lagSykmeldtBruker(harArbeidsgiver = true)
        val utenArbeidsgiver = lagSykmeldtBruker(harArbeidsgiver = false)
        val ikkeSykmeldt = lagSykmeldtBruker().copy(sykmeldt = null)
        skrivOgVent(listOf(medArbeidsgiver, utenArbeidsgiver, ikkeSykmeldt))

        val kunMedArbeidsgiver = hentSykmeldte(filter = listOf(Sykmeldt.ER_SYKMELDT_MED_ARBEIDSGIVER))
        val kunUtenArbeidsgiver = hentSykmeldte(filter = listOf(Sykmeldt.ER_SYKMELDT_UTEN_ARBEIDSGIVER))
        val begge = hentSykmeldte(
            filter = listOf(Sykmeldt.ER_SYKMELDT_MED_ARBEIDSGIVER, Sykmeldt.ER_SYKMELDT_UTEN_ARBEIDSGIVER)
        )

        assertThat(kunMedArbeidsgiver).containsExactly(medArbeidsgiver.fnr)
        assertThat(kunUtenArbeidsgiver).containsExactly(utenArbeidsgiver.fnr)
        assertThat(begge).containsExactlyInAnyOrder(medArbeidsgiver.fnr, utenArbeidsgiver.fnr)
    }

    @Test
    fun skal_sortere_brukere_pa_sykmeldt_startdato() {
        val tidligst = lagSykmeldtBruker(startdato = LocalDate.now().minusWeeks(4))
        val senest = lagSykmeldtBruker(startdato = LocalDate.now().minusWeeks(1))
        skrivOgVent(listOf(senest, tidligst))

        val stigende = hentSykmeldte(Sorteringsfelt.SYKMELDT_STARTDATO, Sorteringsrekkefolge.STIGENDE)
        val synkende = hentSykmeldte(Sorteringsfelt.SYKMELDT_STARTDATO, Sorteringsrekkefolge.SYNKENDE)

        assertThat(stigende).containsExactly(tidligst.fnr, senest.fnr)
        assertThat(synkende).containsExactly(senest.fnr, tidligst.fnr)
    }

    @Test
    fun skal_sortere_brukere_pa_sykmeldt_sluttdato() {
        val tidligst = lagSykmeldtBruker(sluttdato = LocalDate.now().plusWeeks(1))
        val senest = lagSykmeldtBruker(sluttdato = LocalDate.now().plusWeeks(4))
        skrivOgVent(listOf(senest, tidligst))

        val stigende = hentSykmeldte(Sorteringsfelt.SYKMELDT_SLUTTDATO, Sorteringsrekkefolge.STIGENDE)
        val synkende = hentSykmeldte(Sorteringsfelt.SYKMELDT_SLUTTDATO, Sorteringsrekkefolge.SYNKENDE)

        assertThat(stigende).containsExactly(tidligst.fnr, senest.fnr)
        assertThat(synkende).containsExactly(senest.fnr, tidligst.fnr)
    }

    @Test
    fun skal_sortere_brukere_pa_sykmeldt_har_arbeidsgiver() {
        val medArbeidsgiver = lagSykmeldtBruker(harArbeidsgiver = true)
        val utenArbeidsgiver = lagSykmeldtBruker(harArbeidsgiver = false)
        skrivOgVent(listOf(medArbeidsgiver, utenArbeidsgiver))

        val stigende = hentSykmeldte(Sorteringsfelt.SYKMELDT_HAR_ARBEIDSGIVER, Sorteringsrekkefolge.STIGENDE)
        val synkende = hentSykmeldte(Sorteringsfelt.SYKMELDT_HAR_ARBEIDSGIVER, Sorteringsrekkefolge.SYNKENDE)

        assertThat(stigende).containsExactly(utenArbeidsgiver.fnr, medArbeidsgiver.fnr)
        assertThat(synkende).containsExactly(medArbeidsgiver.fnr, utenArbeidsgiver.fnr)
    }

    private fun lagSykmeldtBruker(
        startdato: LocalDate = LocalDate.now().minusWeeks(2),
        sluttdato: LocalDate = LocalDate.now().plusWeeks(2),
        harArbeidsgiver: Boolean = true
    ) = PortefoljebrukerOpensearchModell(
        aktoer_id = randomAktorId().get(),
        fnr = randomFnr().toString(),
        oppfolging = true,
        enhet_id = TEST_ENHET,
        veileder_id = TEST_VEILEDER_0,
        sykmeldt = SykmeldtForOpensearch(
            startdato = startdato,
            sluttdato = sluttdato,
            harArbeidsgiver = harArbeidsgiver
        )
    )

    private fun skrivOgVent(brukere: List<PortefoljebrukerOpensearchModell>) {
        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }
    }

    private fun hentSykmeldte(
        sorteringsfelt: Sorteringsfelt = Sorteringsfelt.IKKE_SATT,
        rekkefolge: Sorteringsrekkefolge = Sorteringsrekkefolge.IKKE_SATT,
        filter: List<Sykmeldt> = Sykmeldt.entries
    ): List<String?> =
        opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            rekkefolge,
            sorteringsfelt,
            getFiltervalgDefaults().copy(sykmeldt = filter),
            null,
            null
        ).brukere.map { it.fnr }

    private fun skrivBrukereTilTestindeks(brukere: List<PortefoljebrukerOpensearchModell>) {
        opensearchIndexer.skrivBulkTilIndeks(BRUKERINDEKS_ALIAS, brukere)
    }
}
