package no.nav.pto.veilarbportefolje.domene.frontendmodell

import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.Profileringsresultat
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.persononinfo.domene.Adressebeskyttelse
import org.junit.Test
import org.junit.jupiter.api.Assertions

class PortefoljebrukerFrontendModellMapperTest {


    @Test
    fun `etiketter skal mappe data rett fra opensearch til frontendmodell`() {
        // given
        val opensearchBruker = PortefoljebrukerOpensearchModell()
        opensearchBruker.setEr_doed(true)
        opensearchBruker.setEr_sykmeldt_med_arbeidsgiver(true)
        opensearchBruker.setNy_for_veileder(true)
        opensearchBruker.setGjeldendeVedtak14a(null)
        val ufordelt = true
        opensearchBruker.setProfilering_resultat(Profileringsresultat.OPPGITT_HINDRINGER)

        // when
        val frontendBruker = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = ufordelt,
            filtervalg = null
        )

        val etiketter = frontendBruker.etiketter

        // then
        Assertions.assertEquals(true, etiketter.erDoed)
        Assertions.assertEquals(true, etiketter.erSykmeldtMedArbeidsgiver)
        Assertions.assertEquals(true, etiketter.nyForVeileder)
        Assertions.assertEquals(true, etiketter.trengerOppfolgingsvedtak)
        Assertions.assertEquals(true, etiketter.nyForEnhet)
        Assertions.assertEquals(Profileringsresultat.OPPGITT_HINDRINGER, etiketter.profileringResultat)

    }

    @Test
    fun `etiketter for diskresjonskodeFortrolig skal settes riktig`() {
        val opensearchBruker = PortefoljebrukerOpensearchModell()

        // fortrolig
        opensearchBruker.setDiskresjonskode(Adressebeskyttelse.FORTROLIG.diskresjonskode)
        val frontendBrukerMedFortrolig = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = null
        )
        val etiketterMedFortrolig = frontendBrukerMedFortrolig.etiketter

        // strengt fortrolig
        opensearchBruker.setDiskresjonskode(Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode)
        val frontendBrukerMedStrengtFortrolig = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = null
        )
        val etiketterMedStrengtFortrolig = frontendBrukerMedStrengtFortrolig.etiketter

        // ugradert
        opensearchBruker.setDiskresjonskode(Adressebeskyttelse.UGRADERT.diskresjonskode)
        val frontendBrukerUgradert = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = null
        )
        val etiketterUtenSikkerhetstiltak = frontendBrukerUgradert.etiketter

        Assertions.assertEquals("7", etiketterMedFortrolig.diskresjonskodeFortrolig)
        Assertions.assertEquals("6", etiketterMedStrengtFortrolig.diskresjonskodeFortrolig)
        Assertions.assertEquals(null, etiketterUtenSikkerhetstiltak.diskresjonskodeFortrolig)
    }

    @Test
    fun `etiketter for harSikkerhetstiltak skal settes riktig`() {
        val opensearchBruker = PortefoljebrukerOpensearchModell()

        opensearchBruker.setSikkerhetstiltak("TOAN")
        val frontendBrukerMedSikkerhetstiltak = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = null
        )
        val etiketterMedSikkerhetstiltak = frontendBrukerMedSikkerhetstiltak.etiketter

        opensearchBruker.setSikkerhetstiltak(null)
        val frontendBrukerUtenSikkerhetstiltak = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = null
        )
        val etiketterUtenSikkerhetstiltak = frontendBrukerUtenSikkerhetstiltak.etiketter

        Assertions.assertEquals(true, etiketterMedSikkerhetstiltak.harSikkerhetstiltak)
        Assertions.assertEquals(false, etiketterUtenSikkerhetstiltak.harSikkerhetstiltak)

    }


    @Test
    fun `etiketter for harBehovForArbeidsevneVurdering skal settes riktig`() {
        val opensearchBruker = PortefoljebrukerOpensearchModell()

        opensearchBruker.setTrenger_vurdering(true)
        opensearchBruker.setKvalifiseringsgruppekode("BKART")
        opensearchBruker.setProfilering_resultat(null)

        val frontendBrukerMedBehov = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = null
        )
        val etiketterMedBehov = frontendBrukerMedBehov.etiketter

        opensearchBruker.setProfilering_resultat(Profileringsresultat.ANTATT_GODE_MULIGHETER)
        val frontendBrukerUtenBehov = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = null
        )
        val etiketterUtenBehov = frontendBrukerUtenBehov.etiketter

        Assertions.assertEquals(true, etiketterMedBehov.harBehovForArbeidsevneVurdering)
        Assertions.assertEquals(false, etiketterUtenBehov.harBehovForArbeidsevneVurdering)
    }


}
