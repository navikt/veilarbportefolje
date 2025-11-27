package no.nav.pto.veilarbportefolje.domene.frontendmodell

import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.Profileringsresultat
import no.nav.pto.veilarbportefolje.domene.EnsligeForsorgereOvergangsstonad
import no.nav.pto.veilarbportefolje.domene.Statsborgerskap
import no.nav.pto.veilarbportefolje.domene.YtelseMapping
import no.nav.pto.veilarbportefolje.domene.filtervalg.Brukerstatus
import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg
import no.nav.pto.veilarbportefolje.hendelsesfilter.Kategori
import no.nav.pto.veilarbportefolje.hendelsesfilter.genererRandomHendelse
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.persononinfo.domene.Adressebeskyttelse
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet.TILTAKSPENGER
import no.nav.pto.veilarbportefolje.util.DateUtils
import no.nav.pto.veilarbportefolje.util.DateUtils.fromIsoUtcToLocalDateOrNull
import no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateTimeOrNull
import org.junit.Test
import org.junit.jupiter.api.Assertions
import java.time.LocalDate
import java.time.LocalDateTime

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

    @Test
    fun `geografiskBosted skal mappes riktig`() {
        val opensearchBruker = PortefoljebrukerOpensearchModell()

        opensearchBruker.setKommunenummer("0301")
        opensearchBruker.setBydelsnummer("1234")
        opensearchBruker.setBostedSistOppdatert(LocalDate.of(2025, 1, 1))
        opensearchBruker.setHarUkjentBosted(true)
        opensearchBruker.setUtenlandskAdresse(null)

        val frontendBruker = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = null
        )
        val bosted = frontendBruker.geografiskBosted

        Assertions.assertEquals("1234", bosted.bostedBydel)
        Assertions.assertEquals("Ukjent", bosted.bostedKommuneUkjentEllerUtland)
        Assertions.assertEquals("0301", bosted.bostedKommune)
        Assertions.assertEquals(LocalDate.of(2025, 1, 1), bosted.bostedSistOppdatert)
    }

    @Test
    fun `hendelser skal velge riktig hendelseskategori for mapping basert på filtervalg`() {
        val opensearchBruker = PortefoljebrukerOpensearchModell()

        val utgattVarselHendelse = genererRandomHendelse(Kategori.UTGATT_VARSEL).hendelse
        val udeltSamtalereferatHendelse = genererRandomHendelse(Kategori.UDELT_SAMTALEREFERAT).hendelse

        opensearchBruker
            .setHendelser(
                mapOf(
                    Kategori.UTGATT_VARSEL to utgattVarselHendelse,
                    Kategori.UDELT_SAMTALEREFERAT to udeltSamtalereferatHendelse
                )
            )

        val frontendBrukerUtgåttVarselFilter = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = Filtervalg().setFerdigfilterListe(listOf(Brukerstatus.UTGATTE_VARSEL))
        )
        val frontendBrukerUdeltSamtalereferatFilter =
            PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
                opensearchBruker = opensearchBruker,
                ufordelt = true,
                filtervalg = Filtervalg().setFerdigfilterListe(listOf(Brukerstatus.UDELT_SAMTALEREFERAT))
            )

        val resultUtgåttVarsel = frontendBrukerUtgåttVarselFilter.hendelse
        val resultUdeltSamtalereferat = frontendBrukerUdeltSamtalereferatFilter.hendelse

        Assertions.assertNotNull(resultUtgåttVarsel)
        Assertions.assertEquals(utgattVarselHendelse.beskrivelse, resultUtgåttVarsel!!.beskrivelse)
        Assertions.assertEquals(utgattVarselHendelse.lenke, resultUtgåttVarsel.lenke)
        Assertions.assertEquals(utgattVarselHendelse.dato.dayOfMonth, resultUtgåttVarsel.dato!!.dayOfMonth)

        Assertions.assertNotNull(resultUdeltSamtalereferat)
        Assertions.assertEquals(udeltSamtalereferatHendelse.beskrivelse, resultUdeltSamtalereferat!!.beskrivelse)
        Assertions.assertEquals(udeltSamtalereferatHendelse.lenke, resultUdeltSamtalereferat.lenke)
        Assertions.assertEquals(
            udeltSamtalereferatHendelse.dato.dayOfMonth,
            resultUdeltSamtalereferat.dato!!.dayOfMonth
        )
    }

    @Test
    fun `dialogdata skal mappes riktig`() {
        val opensearchBruker = PortefoljebrukerOpensearchModell()
        val svarFraNavDato = DateUtils.toIsoUTC(LocalDateTime.of(2024, 5, 20, 0, 0))
        val svarFraBrukerDato = DateUtils.toIsoUTC(LocalDateTime.of(2024, 5, 20, 0, 0))
        opensearchBruker.setVenterpasvarfranav(svarFraNavDato)
        opensearchBruker.setVenterpasvarfrabruker(svarFraBrukerDato)
        val frontendBruker = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = null
        )
        val dialogdata = frontendBruker.meldingerVenterPaSvar

        Assertions.assertEquals(fromIsoUtcToLocalDateOrNull(svarFraNavDato), dialogdata.datoMeldingVenterPaNav)
        Assertions.assertEquals(fromIsoUtcToLocalDateOrNull(svarFraBrukerDato), dialogdata.datoMeldingVenterPaBruker)

    }

    @Test
    fun `statborgerskap og gyldig fra dato skal mappes riktig`() {
        val opensearchBruker = PortefoljebrukerOpensearchModell()
        val gyldigFraDato = LocalDate.of(2000, 5, 20)
        opensearchBruker.setHovedStatsborgerskap(Statsborgerskap("NOR", gyldigFraDato, null))

        val frontendBruker = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = null
        )
        val statsborgerskap = frontendBruker.hovedStatsborgerskap

        Assertions.assertEquals("NOR", statsborgerskap!!.statsborgerskap)
        Assertions.assertEquals(gyldigFraDato, statsborgerskap.gyldigFra)
    }

    @Test
    fun `skal mappe alle ytelser til ytelserForBruker når det finnes data`() {
        val opensearchBruker = PortefoljebrukerOpensearchModell()
        opensearchBruker.setYtelse("ORDINARE_DAGPENGER")
        opensearchBruker.setUtlopsdato("2023-06-30T21:59:59Z")
        opensearchBruker.setDagputlopuke(2)
        opensearchBruker.setPermutlopuke(4)
        opensearchBruker.setAapmaxtiduke(10)
        opensearchBruker.setAapunntakukerigjen(5)
        opensearchBruker.setAapordinerutlopsdato(LocalDate.of(2026, 1, 1))
        opensearchBruker.setAap_kelvin_rettighetstype(AapRettighetstype.SYKEPENGEERSTATNING)
        opensearchBruker.setAap_kelvin_tom_vedtaksdato(LocalDate.of(2026, 1, 1))
        opensearchBruker.setTiltakspenger_rettighet(TILTAKSPENGER)
        opensearchBruker.setTiltakspenger_vedtaksdato_tom(LocalDate.of(2026, 1, 1))
        opensearchBruker.setEnslige_forsorgere_overgangsstonad(
            EnsligeForsorgereOvergangsstonad(
                "Utvidelse",
                false,
                LocalDate.now().plusMonths(1),
                LocalDate.now().minusMonths(3)
            )
        );

        val frontendBruker = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = null
        )
        val ytelser = frontendBruker.ytelser
        Assertions.assertEquals(YtelseMapping.ORDINARE_DAGPENGER, ytelser.ytelserArena.ytelse)
        Assertions.assertEquals(toLocalDateTimeOrNull("2023-06-30T21:59:59Z"), ytelser.ytelserArena.utlopsdato)
        Assertions.assertEquals(2, ytelser.ytelserArena.dagputlopUke)
        Assertions.assertEquals(4, ytelser.ytelserArena.permutlopUke)
        Assertions.assertEquals(10, ytelser.ytelserArena.aapmaxtidUke)
        Assertions.assertEquals(5, ytelser.ytelserArena.aapUnntakUkerIgjen)
        Assertions.assertEquals(LocalDate.of(2026, 1, 1), ytelser.ytelserArena.aapordinerutlopsdato)
        Assertions.assertEquals(
            AapRettighetstype.Companion.tilFrontendtekst(AapRettighetstype.SYKEPENGEERSTATNING),
            ytelser.aap!!.rettighetstype
        )
        Assertions.assertEquals(LocalDate.of(2026, 1, 1), ytelser.aap!!.vedtaksdatoTilOgMed)
        Assertions.assertEquals("Tiltakspenger", ytelser.tiltakspenger!!.rettighet)
        Assertions.assertEquals(LocalDate.of(2026, 1, 1), ytelser.tiltakspenger!!.vedtaksdatoTilOgMed)
        Assertions.assertEquals("Utvidelse", ytelser.ensligeForsorgereOvergangsstonad!!.vedtaksPeriodetype)
    }

    @Test
    fun `skal sette null på ytelsesfelt når det ikke finnes data`() {
        val opensearchBruker = PortefoljebrukerOpensearchModell()

        val frontendBruker = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = null
        )
        val ytelser = frontendBruker.ytelser
        Assertions.assertNotNull(ytelser)
        Assertions.assertNotNull(ytelser.ytelserArena)
        Assertions.assertEquals(null, ytelser.ytelserArena.ytelse)
        Assertions.assertEquals(null, ytelser.ytelserArena.utlopsdato)
        Assertions.assertEquals(null, ytelser.ytelserArena.dagputlopUke)
        Assertions.assertEquals(null, ytelser.ytelserArena.permutlopUke)
        Assertions.assertEquals(null, ytelser.ytelserArena.aapmaxtidUke)
        Assertions.assertEquals(null, ytelser.ytelserArena.aapUnntakUkerIgjen)
        Assertions.assertEquals(null, ytelser.ytelserArena.aapordinerutlopsdato)
        Assertions.assertEquals(null, ytelser.aap)
        Assertions.assertEquals(null, ytelser.aap?.vedtaksdatoTilOgMed)
        Assertions.assertEquals(null, ytelser.tiltakspenger)
        Assertions.assertEquals(null, ytelser.tiltakspenger?.vedtaksdatoTilOgMed)
        Assertions.assertEquals(null, ytelser.ensligeForsorgereOvergangsstonad?.vedtaksPeriodetype)
    }

}
