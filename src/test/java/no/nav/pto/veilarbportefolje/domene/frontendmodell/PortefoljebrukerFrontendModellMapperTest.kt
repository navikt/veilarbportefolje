package no.nav.pto.veilarbportefolje.domene.frontendmodell

import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.Profileringsresultat
import no.nav.pto.veilarbportefolje.dagpenger.domene.DagpengerRettighetstype
import no.nav.pto.veilarbportefolje.domene.*
import no.nav.pto.veilarbportefolje.domene.filtervalg.AktivitetFiltervalg
import no.nav.pto.veilarbportefolje.domene.filtervalg.Brukerstatus
import no.nav.pto.veilarbportefolje.domene.opensearchmodell.DagpengerForOpensearch
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriVerdi
import no.nav.pto.veilarbportefolje.hendelsesfilter.Kategori
import no.nav.pto.veilarbportefolje.hendelsesfilter.genererRandomHendelse
import no.nav.pto.veilarbportefolje.opensearch.domene.Endring
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.GjeldendeVedtak14a
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData
import no.nav.pto.veilarbportefolje.persononinfo.domene.Adressebeskyttelse
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet.TILTAKSPENGER
import no.nav.pto.veilarbportefolje.util.DateUtils.*
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe
import org.junit.Test
import org.junit.jupiter.api.Assertions
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

class PortefoljebrukerFrontendModellMapperTest {


    @Test
    fun `etiketter skal mappe data rett fra opensearch til frontendmodell`() {
        // given
        val opensearchBruker = PortefoljebrukerOpensearchModell(
            er_doed = true,
            er_sykmeldt_med_arbeidsgiver = true,
            ny_for_veileder = true,
            gjeldendeVedtak14a = null,
            profilering_resultat = Profileringsresultat.OPPGITT_HINDRINGER,
        )

        // when
        val frontendBruker = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
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
        // fortrolig
        val frontendBrukerMedFortrolig = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = PortefoljebrukerOpensearchModell(diskresjonskode = Adressebeskyttelse.FORTROLIG.diskresjonskode),
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )
        val etiketterMedFortrolig = frontendBrukerMedFortrolig.etiketter

        // strengt fortrolig
        val frontendBrukerMedStrengtFortrolig = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = PortefoljebrukerOpensearchModell(diskresjonskode = Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode),
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )
        val etiketterMedStrengtFortrolig = frontendBrukerMedStrengtFortrolig.etiketter

        // ugradert
        val frontendBrukerUgradert = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = PortefoljebrukerOpensearchModell(diskresjonskode = Adressebeskyttelse.UGRADERT.diskresjonskode),
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )
        val etiketterUtenSikkerhetstiltak = frontendBrukerUgradert.etiketter

        Assertions.assertEquals("7", etiketterMedFortrolig.diskresjonskodeFortrolig)
        Assertions.assertEquals("6", etiketterMedStrengtFortrolig.diskresjonskodeFortrolig)
        Assertions.assertEquals(null, etiketterUtenSikkerhetstiltak.diskresjonskodeFortrolig)
    }

    @Test
    fun `etiketter for harSikkerhetstiltak skal settes riktig`() {
        val frontendBrukerMedSikkerhetstiltak = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = PortefoljebrukerOpensearchModell(sikkerhetstiltak = "TOAN"),
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )
        val etiketterMedSikkerhetstiltak = frontendBrukerMedSikkerhetstiltak.etiketter

        val frontendBrukerUtenSikkerhetstiltak = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = PortefoljebrukerOpensearchModell(sikkerhetstiltak = null),
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )
        val etiketterUtenSikkerhetstiltak = frontendBrukerUtenSikkerhetstiltak.etiketter

        Assertions.assertEquals(true, etiketterMedSikkerhetstiltak.harSikkerhetstiltak)
        Assertions.assertEquals(false, etiketterUtenSikkerhetstiltak.harSikkerhetstiltak)

    }


    @Test
    fun `etiketter for harBehovForArbeidsevneVurdering skal settes riktig`() {
        val opensearchBruker = PortefoljebrukerOpensearchModell(
            trenger_vurdering = true,
            kvalifiseringsgruppekode = "BKART",
            profilering_resultat = null,
        )

        val frontendBrukerMedBehov = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )
        val etiketterMedBehov = frontendBrukerMedBehov.etiketter

        val frontendBrukerUtenBehov = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker.copy(profilering_resultat = Profileringsresultat.ANTATT_GODE_MULIGHETER),
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )
        val etiketterUtenBehov = frontendBrukerUtenBehov.etiketter

        Assertions.assertEquals(true, etiketterMedBehov.harBehovForArbeidsevneVurdering)
        Assertions.assertEquals(false, etiketterUtenBehov.harBehovForArbeidsevneVurdering)
    }

    @Test
    fun `personaliadata og en til en variabler skal mappes riktig`() {
        val opensearchBruker = PortefoljebrukerOpensearchModell(
            fnr = "12345678901",
            fornavn = "Ola",
            etternavn = "Nordmann",
            foedelandFulltNavn = "NOR",
            barn_under_18_aar = listOf(BarnUnder18AarData(8, null)),
            egen_ansatt = false,
            skjermet_til = LocalDateTime.of(2020, 1, 1, 10, 0),
            utdanning_og_situasjon_sist_endret = LocalDate.of(2024, 1, 1),
            neste_svarfrist_stilling_fra_nav = LocalDate.of(2024, 5, 1),
        )

        val frontendBruker = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )

        Assertions.assertEquals("12345678901", frontendBruker.fnr)
        Assertions.assertEquals("Ola", frontendBruker.fornavn)
        Assertions.assertEquals("Nordmann", frontendBruker.etternavn)
        Assertions.assertEquals("NOR", frontendBruker.foedeland)
        Assertions.assertEquals(listOf(BarnUnder18AarData(8, null)), frontendBruker.barnUnder18AarData)
        Assertions.assertEquals(false, frontendBruker.egenAnsatt)
        Assertions.assertEquals(LocalDate.of(2020, 1, 1), frontendBruker.skjermetTil)
        Assertions.assertEquals(LocalDate.of(2024, 1, 1), frontendBruker.utdanningOgSituasjonSistEndret)
        Assertions.assertEquals(LocalDate.of(2024, 5, 1), frontendBruker.nesteSvarfristCvStillingFraNav)

    }

    @Test
    fun `geografiskBosted skal mappes riktig`() {
        val opensearchBruker = PortefoljebrukerOpensearchModell(
            kommunenummer = "0301",
            bydelsnummer = "1234",
            bostedSistOppdatert = LocalDate.of(2025, 1, 1),
            harUkjentBosted = true,
            utenlandskAdresse = null,
        )

        val frontendBruker = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )
        val bosted = frontendBruker.geografiskBosted

        Assertions.assertEquals("1234", bosted.bostedBydel)
        Assertions.assertEquals("Ukjent", bosted.bostedKommuneUkjentEllerUtland)
        Assertions.assertEquals("0301", bosted.bostedKommune)
        Assertions.assertEquals(LocalDate.of(2025, 1, 1), bosted.bostedSistOppdatert)
    }

    @Test
    fun `statborgerskap og gyldig fra dato skal mappes riktig`() {
        val frontendBruker = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = PortefoljebrukerOpensearchModell(
                hovedStatsborgerskap = Statsborgerskap("NOR", LocalDate.of(2000, 5, 20), null)
            ),
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )
        val statsborgerskap = frontendBruker.hovedStatsborgerskap

        Assertions.assertEquals("NOR", statsborgerskap!!.statsborgerskap)
        Assertions.assertEquals(LocalDate.of(2000, 5, 20), statsborgerskap.gyldigFra)
    }

    @Test
    fun `tolkebehov skal mappes riktig`() {
        val sistOppdatert = LocalDate.of(2025, 5, 20)
        val opensearchBruker = PortefoljebrukerOpensearchModell()

        val frontendBrukerUtenData = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )

        val tolkebehovUtenData = frontendBrukerUtenData.tolkebehov
        Assertions.assertEquals("", tolkebehovUtenData.talespraaktolk)
        Assertions.assertEquals("", tolkebehovUtenData.tegnspraaktolk)
        Assertions.assertEquals(null, tolkebehovUtenData.sistOppdatert)

        val frontendBrukerMedData = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker.copy(
                talespraaktolk = "FR",
                tegnspraaktolk = "EN",
                tolkBehovSistOppdatert = sistOppdatert,
            ),
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )
        val tolkebehov = frontendBrukerMedData.tolkebehov

        Assertions.assertEquals("FR", tolkebehov.talespraaktolk)
        Assertions.assertEquals("EN", tolkebehov.tegnspraaktolk)
        Assertions.assertEquals(sistOppdatert, tolkebehov.sistOppdatert)
    }

    @Test
    fun `oppfølgingsdata skal mappes riktig`() {
        val opensearchBruker = PortefoljebrukerOpensearchModell(
            oppfolging_startdato = "2023-06-30T21:59:59Z",
            tildelt_tidspunkt = LocalDateTime.of(2023, 6, 15, 10, 0),
            veileder_id = "Z123456",
        )

        val frontendBruker = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )

        Assertions.assertEquals(
            toLocalDateTimeOrNull("2023-06-30T21:59:59Z")?.toLocalDate(),
            frontendBruker.oppfolgingStartdato
        )
        Assertions.assertEquals(LocalDate.of(2023, 6, 15), frontendBruker.tildeltTidspunkt)
        Assertions.assertEquals("Z123456", frontendBruker.veilederId)
    }

    @Test
    fun `hendelser skal velge riktig hendelseskategori for mapping basert på filtervalg`() {
        val utgattVarselHendelse = genererRandomHendelse(Kategori.UTGATT_VARSEL).hendelse
        val udeltSamtalereferatHendelse = genererRandomHendelse(Kategori.UDELT_SAMTALEREFERAT).hendelse
        val opensearchBruker = PortefoljebrukerOpensearchModell(
            hendelser =
                mapOf(
                    Kategori.UTGATT_VARSEL to utgattVarselHendelse,
                    Kategori.UDELT_SAMTALEREFERAT to udeltSamtalereferatHendelse
                )
        )

        val frontendBrukerUtgåttVarselFilter = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults().copy(
                ferdigfilterListe = listOf(Brukerstatus.UTGATTE_VARSEL)
            )
        )
        val frontendBrukerUdeltSamtalereferatFilter =
            PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
                opensearchBruker = opensearchBruker,
                ufordelt = true,
                filtervalg = getFiltervalgDefaults().copy(
                    ferdigfilterListe = listOf(Brukerstatus.UDELT_SAMTALEREFERAT)
                )
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
        val svarFraNavDato = toIsoUTC(LocalDateTime.of(2024, 5, 20, 0, 0))
        val svarFraBrukerDato = toIsoUTC(LocalDateTime.of(2024, 5, 20, 0, 0))
        val opensearchBruker = PortefoljebrukerOpensearchModell(
            venterpasvarfranav = svarFraNavDato,
            venterpasvarfrabruker = svarFraBrukerDato,
        )

        val frontendBruker = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )

        val dialogdata = frontendBruker.meldingerVenterPaSvar
        Assertions.assertEquals(fromIsoUtcToLocalDateOrNull(svarFraNavDato), dialogdata.datoMeldingVenterPaNav)
        Assertions.assertEquals(fromIsoUtcToLocalDateOrNull(svarFraBrukerDato), dialogdata.datoMeldingVenterPaBruker)

    }


    @Test
    fun `skal mappe alle ytelser til ytelserForBruker når det finnes data`() {
        val opensearchBruker = PortefoljebrukerOpensearchModell(
            ytelse = "ORDINARE_DAGPENGER",
            utlopsdato = "2023-06-30T21:59:59Z",
            dagputlopuke = 2,
            permutlopuke = 4,
            aapmaxtiduke = 10,
            aapunntakukerigjen = 5,
            aapordinerutlopsdato = LocalDate.of(2026, 1, 1),
            aap_kelvin_rettighetstype = AapRettighetstype.VURDERES_FOR_UFØRETRYGD,
            aap_kelvin_tom_vedtaksdato = LocalDate.of(2026, 1, 1),
            tiltakspenger_rettighet = TILTAKSPENGER,
            tiltakspenger_vedtaksdato_tom = LocalDate.of(2026, 1, 1),
            enslige_forsorgere_overgangsstonad =
                EnsligeForsorgereOvergangsstonad(
                    "Utvidelse",
                    false,
                    LocalDate.now().plusMonths(1),
                    LocalDate.now().minusMonths(3)
                ),
            dagpenger = DagpengerForOpensearch(
                true,
                DagpengerRettighetstype.DAGPENGER_PERMITTERING_ORDINAER,
                null,
                156,
                LocalDate.of(2026, 1, 1),
            )
        )

        val frontendBruker = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
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
            AapRettighetstype.Companion.tilFrontendtekst(AapRettighetstype.VURDERES_FOR_UFØRETRYGD),
            ytelser.aap!!.rettighetstype
        )
        Assertions.assertEquals(LocalDate.of(2026, 1, 1), ytelser.aap.vedtaksdatoTilOgMed)
        Assertions.assertEquals("Tiltakspenger", ytelser.tiltakspenger!!.rettighet)
        Assertions.assertEquals(LocalDate.of(2026, 1, 1), ytelser.tiltakspenger.vedtaksdatoTilOgMed)
        Assertions.assertEquals("Utvidelse", ytelser.ensligeForsorgereOvergangsstonad!!.vedtaksPeriodetype)
        Assertions.assertEquals("Dagpenger under permittering", ytelser.dagpenger!!.rettighetstype)
        Assertions.assertEquals(null, ytelser.dagpenger.datoStans)
        Assertions.assertEquals("156 dager (per 01.01.2026)", ytelser.dagpenger.resterendeDager)
    }

    @Test
    fun `skal sette null på ytelsesfelt når det ikke finnes data`() {
        val opensearchBruker = PortefoljebrukerOpensearchModell()

        val frontendBruker = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
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
        Assertions.assertEquals(null, ytelser.dagpenger)
    }

    @Test
    fun `skal mappe vedtak 14a til riktig object når det finnes data `() {
        val dagensDato = LocalDate.now()
        val utcDateTimeStatus = dagensDato
            .minusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .withZoneSameInstant(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_INSTANT)
        val zonedDateTimeFattetDato = ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())

        val opensearchBruker = PortefoljebrukerOpensearchModell(
            utkast_14a_status = "Utkast",
            utkast_14a_status_endret = utcDateTimeStatus,
            utkast_14a_ansvarlig_veileder = "Veileder Navn",
            gjeldendeVedtak14a =
                GjeldendeVedtak14a(
                    Innsatsgruppe.STANDARD_INNSATS,
                    Hovedmal.SKAFFE_ARBEID,
                    zonedDateTimeFattetDato
                )
        )

        val frontendBruker = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )

        val vedtak14a = frontendBruker.vedtak14a
        Assertions.assertNotNull(vedtak14a.gjeldendeVedtak14a)
        Assertions.assertNotNull(vedtak14a.utkast14a)
        Assertions.assertEquals(Innsatsgruppe.STANDARD_INNSATS, vedtak14a.gjeldendeVedtak14a!!.innsatsgruppe)
        Assertions.assertEquals(Hovedmal.SKAFFE_ARBEID, vedtak14a.gjeldendeVedtak14a!!.hovedmal)
        Assertions.assertEquals(zonedDateTimeFattetDato.toLocalDate(), vedtak14a.gjeldendeVedtak14a!!.fattetDato)
        Assertions.assertEquals("Utkast", vedtak14a.utkast14a!!.status)
        Assertions.assertEquals("1 dag siden", vedtak14a.utkast14a!!.dagerSidenStatusEndretSeg)
        Assertions.assertEquals("Veileder Navn", vedtak14a.utkast14a!!.ansvarligVeileder)
    }

    @Test
    fun `skal ikke mappe vedtak 14a når det ikke finnes data `() {
        val opensearchBruker = PortefoljebrukerOpensearchModell()
        val frontendBruker = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )
        val vedtak14a = frontendBruker.vedtak14a

        Assertions.assertNotNull(vedtak14a)
        Assertions.assertNull(vedtak14a.gjeldendeVedtak14a)
        Assertions.assertNull(vedtak14a.utkast14a)
    }

    @Test
    fun `skal mappe siste endring av bruker når det finnes data i samme kategori som valgt filter`() {
        val kategori = " NY_BEHANDLING"
        val isoUtc = "2023-06-30T21:59:59Z"
        val sisteEndringOpensearch: Map<String, Endring> = mapOf(
            kategori to Endring(
                aktivtetId = "11111",
                tidspunkt = isoUtc,
                erSett = "N"
            )
        )
        val opensearchBruker = PortefoljebrukerOpensearchModell(siste_endringer = sisteEndringOpensearch)

        val frontendBrukerMedSammeFilterkategori =
            PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
                opensearchBruker = opensearchBruker,
                ufordelt = true,
                filtervalg = getFiltervalgDefaults().copy(
                    sisteEndringKategori = kategori
                )
            )

        val sisteEndringMedSammeFilterkategori = frontendBrukerMedSammeFilterkategori.sisteEndringAvBruker
        Assertions.assertNotNull(sisteEndringMedSammeFilterkategori)
        Assertions.assertEquals("11111", sisteEndringMedSammeFilterkategori!!.aktivitetId)
        Assertions.assertEquals(fromIsoUtcToLocalDateOrNull(isoUtc), sisteEndringMedSammeFilterkategori.tidspunkt)
        Assertions.assertEquals(kategori, sisteEndringMedSammeFilterkategori.kategori)
    }

    @Test
    fun `skal ikke mappe siste endring av bruker når det ikke finnes data i samme kategori som valgt filter`() {
        val kategori = " NY_BEHANDLING"
        val isoUtc = "2023-06-30T21:59:59Z"
        val sisteEndringOpensearch: Map<String, Endring> = mapOf(
            kategori to Endring(
                aktivtetId = "11111",
                tidspunkt = isoUtc,
                erSett = "N"
            )
        )
        val opensearchBruker = PortefoljebrukerOpensearchModell(siste_endringer = sisteEndringOpensearch)

        val frontendBrukerMedAnnenFilterkategori =
            PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
                opensearchBruker = opensearchBruker,
                ufordelt = true,
                filtervalg = getFiltervalgDefaults().copy(
                    sisteEndringKategori = "NY_JOBB"
                )
            )

        val sisteEndringMedAnnenFilterkategori = frontendBrukerMedAnnenFilterkategori.sisteEndringAvBruker
        Assertions.assertNull(sisteEndringMedAnnenFilterkategori)
    }

    @Test
    fun `skal ikke mappe siste endring av bruker når det ikke finnes data i opensearch`() {
        val opensearchBruker = PortefoljebrukerOpensearchModell()
        val kategori = " NY_BEHANDLING"

        val frontendBrukerMedFilterkategori = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults().copy(
                sisteEndringKategori = kategori
            )
        )

        val sisteEndring = frontendBrukerMedFilterkategori.sisteEndringAvBruker
        Assertions.assertNull(sisteEndring)
    }

    @Test
    fun `skal ikke mappe siste endring av bruker når det ikke er valgt filter for siste endring`() {
        val kategori = " NY_BEHANDLING"
        val isoUtc = "2023-06-30T21:59:59Z"
        val sisteEndringOpensearch: Map<String, Endring> = mapOf(
            kategori to Endring(
                aktivtetId = "11111",
                tidspunkt = isoUtc,
                erSett = "N"
            )
        )
        val opensearchBruker = PortefoljebrukerOpensearchModell(siste_endringer = sisteEndringOpensearch)

        val frontendBrukerMedFilterkategori = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults(),
        )

        val sisteEndring = frontendBrukerMedFilterkategori.sisteEndringAvBruker
        Assertions.assertNull(sisteEndring)
    }

    @Test
    fun `aktiviteterAvtaltMedNav - skal mappe aktiviteter med dato og hente ut den neste`() {
        val tidspunkt1 = toIsoUTC(ZonedDateTime.now().plusDays(1))
        val tidspunkt2 = toIsoUTC(ZonedDateTime.now().plusDays(2))
        val tidspunkt3 = toIsoUTC(ZonedDateTime.now().plusDays(3))
        val tidspunkt4 = getFarInTheFutureDate()
        val opensearchBruker = PortefoljebrukerOpensearchModell(
            aktivitet_tiltak_utlopsdato = tidspunkt1,
            aktivitet_mote_utlopsdato = tidspunkt2,
            aktivitet_stilling_utlopsdato = tidspunkt3,
            aktivitet_behandling_utlopsdato = tidspunkt4,
        )

        val frontendBruker = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults(),
        )

        val nesteUtlopsdato = frontendBruker.aktiviteterAvtaltMedNav?.nesteUtlopsdatoForAlleAktiviteter
        Assertions.assertNotNull(nesteUtlopsdato)
        Assertions.assertEquals(fromIsoUtcToLocalDateOrNull(tidspunkt1), nesteUtlopsdato)
    }

    @Test
    fun `aktiviteterAvtaltMedNav - skal mappe neste utløpsdato basert på forenklet filtervalg og den nyeste datoen`() {
        val tidspunkt1 = toIsoUTC(ZonedDateTime.now().plusDays(1))
        val tidspunkt2 = toIsoUTC(ZonedDateTime.now().plusDays(2))
        val tidspunkt3 = toIsoUTC(ZonedDateTime.now().plusDays(3))
        val tidspunkt4 = toIsoUTC(ZonedDateTime.now().plusDays(4))
        val opensearchBruker = PortefoljebrukerOpensearchModell(
            aktivitet_tiltak_utlopsdato = tidspunkt1,
            aktivitet_mote_utlopsdato = tidspunkt2,
            aktivitet_stilling_utlopsdato = tidspunkt3,
            aktivitet_behandling_utlopsdato = tidspunkt4
        )

        val frontendBrukerUtenFilter = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults(),
        )

        val aktiviteterUtenFilter =
            frontendBrukerUtenFilter.aktiviteterAvtaltMedNav?.nesteUtlopsdatoForFiltrerteAktiviteter
        Assertions.assertNull(aktiviteterUtenFilter)

        val frontendBrukerMedForenkletfilter = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults().copy(
                aktiviteterForenklet = listOf("BEHANDLING", "MOTE")
            )
        )

        val aktiviteterMedForenkletfilter =
            frontendBrukerMedForenkletfilter.aktiviteterAvtaltMedNav?.nesteUtlopsdatoForFiltrerteAktiviteter
        Assertions.assertNotNull(aktiviteterMedForenkletfilter)
        Assertions.assertEquals(fromIsoUtcToLocalDateOrNull(tidspunkt2), aktiviteterMedForenkletfilter)

        val frontendBrukerMedTiltaksfilter = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults().copy(
                tiltakstyper = listOf("BEHANDLING", "MOTE")
            )
        )

        val aktiviteterMedTiltaksfilter =
            frontendBrukerMedTiltaksfilter.aktiviteterAvtaltMedNav?.nesteUtlopsdatoForFiltrerteAktiviteter
        Assertions.assertNotNull(aktiviteterMedTiltaksfilter)
        Assertions.assertEquals(fromIsoUtcToLocalDateOrNull(tidspunkt1), aktiviteterMedTiltaksfilter)
    }

    @Test
    fun `aktiviteterAvtaltMedNav - skal mappe neste utløpsdato basert på avansert filtervalg og den nyeste datoen`() {
        val tidspunkt1 = toIsoUTC(ZonedDateTime.now().plusDays(1))
        val tidspunkt2 = toIsoUTC(ZonedDateTime.now().plusDays(2))
        val tidspunkt3 = toIsoUTC(ZonedDateTime.now().plusDays(3))
        val tidspunkt4 = toIsoUTC(ZonedDateTime.now().plusDays(4))
        val opensearchBruker = PortefoljebrukerOpensearchModell(
            aktivitet_tiltak_utlopsdato = tidspunkt1,
            aktivitet_mote_utlopsdato = tidspunkt2,
            aktivitet_stilling_utlopsdato = tidspunkt3,
            aktivitet_behandling_utlopsdato = tidspunkt4,
        )

        val frontendBrukerMedAvansertfilter = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults().copy(
                aktiviteter =
                    mapOf(
                        "BEHANDLING" to AktivitetFiltervalg.JA,
                        "MOTE" to AktivitetFiltervalg.JA,
                        "STILLING" to AktivitetFiltervalg.NEI,
                        "TILTAK" to AktivitetFiltervalg.NEI
                    )
            )
        )

        val aktiviteterMedAvansertfilter =
            frontendBrukerMedAvansertfilter.aktiviteterAvtaltMedNav?.nesteUtlopsdatoForFiltrerteAktiviteter
        Assertions.assertNotNull(aktiviteterMedAvansertfilter)
        Assertions.assertEquals(fromIsoUtcToLocalDateOrNull(tidspunkt2), aktiviteterMedAvansertfilter)

    }

    @Test
    fun `aktiviteterAvtaltMedNav - skal mappe neste utløpsdato basert på forenklet OG avansert filtervalg og den nyeste datoen`() {
        val tidspunkt1 = toIsoUTC(ZonedDateTime.now().plusDays(1))
        val tidspunkt2 = toIsoUTC(ZonedDateTime.now().plusDays(2))
        val tidspunkt3 = toIsoUTC(ZonedDateTime.now().plusDays(3))
        val tidspunkt4 = toIsoUTC(ZonedDateTime.now().plusDays(4))
        val opensearchBruker = PortefoljebrukerOpensearchModell(
            aktivitet_tiltak_utlopsdato = tidspunkt1,
            aktivitet_mote_utlopsdato = tidspunkt2,
            aktivitet_stilling_utlopsdato = tidspunkt3,
            aktivitet_behandling_utlopsdato = tidspunkt4,
        )

        val frontendBrukerMedBeggeFilter = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults().copy(
                aktiviteter = mapOf(
                    "BEHANDLING" to AktivitetFiltervalg.JA,
                    "STILLING" to AktivitetFiltervalg.JA,
                ),
                aktiviteterForenklet = listOf("TILTAK", "MOTE")
            )
        )

        val aktiviteterMedBeggefilter =
            frontendBrukerMedBeggeFilter.aktiviteterAvtaltMedNav?.nesteUtlopsdatoForFiltrerteAktiviteter
        Assertions.assertNotNull(aktiviteterMedBeggefilter)
        Assertions.assertEquals(fromIsoUtcToLocalDateOrNull(tidspunkt1), aktiviteterMedBeggefilter)
    }


    @Test
    fun `aktiviteterAvtaltMedNav - skal mappe resterende felter`() {
        val tidspunkt1 = toIsoUTC(ZonedDateTime.now().plusDays(1))
        val tidspunkt2 = toIsoUTC(ZonedDateTime.now().plusDays(2))
        val tidspunkt3 = toIsoUTC(ZonedDateTime.now().plusDays(3))
        val tidspunkt4 = toIsoUTC(ZonedDateTime.now().plusDays(4))
        val opensearchBruker = PortefoljebrukerOpensearchModell(
            nyesteutlopteaktivitet = tidspunkt1,
            aktivitet_start = tidspunkt2,
            neste_aktivitet_start = tidspunkt3,
            forrige_aktivitet_start = tidspunkt4,
        )

        val frontendBruker = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )

        val aktiviteter = frontendBruker.aktiviteterAvtaltMedNav
        Assertions.assertNotNull(aktiviteter)
        Assertions.assertEquals(fromIsoUtcToLocalDateOrNull(tidspunkt1), aktiviteter?.nyesteUtlopteAktivitet)
        Assertions.assertEquals(fromIsoUtcToLocalDateOrNull(tidspunkt2), aktiviteter?.aktivitetStart)
        Assertions.assertEquals(fromIsoUtcToLocalDateOrNull(tidspunkt3), aktiviteter?.nesteAktivitetStart)
        Assertions.assertEquals(fromIsoUtcToLocalDateOrNull(tidspunkt4), aktiviteter?.forrigeAktivitetStart)
    }

    @Test
    fun `moteMedNav - skal mappes riktig`() {
        val avtaltMedNavIDag = toIsoUTC(ZonedDateTime.now())
        val ikkeAvtaltMedNavIDag = toIsoUTC(ZonedDateTime.now().plusDays(1))
        val møteIDagStart = toIsoUTC(ZonedDateTime.now().withHour(9).withMinute(30))
        val møteIDagSlutt = toIsoUTC(ZonedDateTime.now().withHour(10).withMinute(30))
        val opensearchBruker = PortefoljebrukerOpensearchModell(
            // har møte i dag som er avtalt med nav
            aktivitet_mote_startdato = avtaltMedNavIDag,
            alle_aktiviteter_mote_startdato = møteIDagStart,
            alle_aktiviteter_mote_utlopsdato = møteIDagSlutt,
        )

        val frontendBrukerAvtaltMøteIDag = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )

        val moteIDagAvtalt = frontendBrukerAvtaltMøteIDag.moteMedNavIDag
        Assertions.assertNotNull(moteIDagAvtalt)
        Assertions.assertEquals(true, moteIDagAvtalt!!.avtaltMedNav)
        Assertions.assertEquals("09:30", moteIDagAvtalt.klokkeslett)
        Assertions.assertEquals(60, moteIDagAvtalt.varighetMinutter)

        val frontendBrukerIkkeAvtaltIDag = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker.copy(
                // har møte i dag som ikke er avtalt med nav
                aktivitet_mote_startdato = ikkeAvtaltMedNavIDag
            ),
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )
        val moteIDagIkkeAvtalt = frontendBrukerIkkeAvtaltIDag.moteMedNavIDag
        Assertions.assertNotNull(moteIDagIkkeAvtalt)
        Assertions.assertEquals(false, moteIDagIkkeAvtalt!!.avtaltMedNav)
        Assertions.assertEquals("09:30", moteIDagIkkeAvtalt.klokkeslett)
        Assertions.assertEquals(60, moteIDagIkkeAvtalt.varighetMinutter)

        val frontendBrukerIkkeMøteIDag = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker.copy(
                // har ikke møte i dag
                aktivitet_mote_startdato = ikkeAvtaltMedNavIDag,
                alle_aktiviteter_mote_startdato = getFarInTheFutureDate(),
                alle_aktiviteter_mote_utlopsdato = getFarInTheFutureDate(),
            ),
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )

        Assertions.assertNull(frontendBrukerIkkeMøteIDag.moteMedNavIDag)
    }

    @Test
    fun `huskelapp og fargekategori skal mappes riktig`() {
        val opensearchBruker = PortefoljebrukerOpensearchModell()

        val frontendBrukerUtenData = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker,
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )

        Assertions.assertNull(frontendBrukerUtenData.huskelapp)
        Assertions.assertNull(frontendBrukerUtenData.fargekategori)
        Assertions.assertNull(frontendBrukerUtenData.fargekategoriEnhetId)

        val huskelapp = HuskelappForBruker(
            LocalDate.now().plusDays(20),
            "dddd avtal møte med bruker",
            LocalDate.now().minusDays(10),
            "Z123456",
            UUID.randomUUID().toString(),
            "0130"
        )

        val frontendBruker = PortefoljebrukerFrontendModellMapper.toPortefoljebrukerFrontendModell(
            opensearchBruker = opensearchBruker.copy(
                huskelapp = huskelapp,
                fargekategori = FargekategoriVerdi.FARGEKATEGORI_A.name,
                fargekategori_enhetId = "1234",
            ),
            ufordelt = true,
            filtervalg = getFiltervalgDefaults()
        )

        Assertions.assertEquals(huskelapp, frontendBruker.huskelapp)
        Assertions.assertEquals(FargekategoriVerdi.FARGEKATEGORI_A.name, frontendBruker.fargekategori)
        Assertions.assertEquals("1234", frontendBruker.fargekategoriEnhetId)

    }
}
