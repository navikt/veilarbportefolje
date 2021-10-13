package no.nav.pto.veilarbportefolje.elastic;

import lombok.SneakyThrows;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg.JA;
import static no.nav.pto.veilarbportefolje.domene.Brukerstatus.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static no.nav.pto.veilarbportefolje.util.ElasticTestClient.pollElasticUntil;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ElasticServiceIntegrationTest extends EndToEndTest {

    private static final String TEST_ENHET = "0000";
    private static final String TEST_VEILEDER_0 = "Z000000";
    private static final String TEST_VEILEDER_1 = "Z000001";
    private static final String LITE_PRIVILEGERT_VEILEDER = "Z000002";

    @Autowired
    private ElasticService elasticService;

    @Autowired
    private ElasticIndexer elasticIndexer;

    @Autowired
    private VeilarbVeilederClient veilarbVeilederClientMock;

    @Test
    void skal_kun_hente_ut_brukere_under_oppfolging() {

        List<OppfolgingsBruker> brukere = List.of(
                new OppfolgingsBruker()
                        .setAktoer_id(randomAktorId().toString())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET),

                new OppfolgingsBruker()
                        .setAktoer_id(randomAktorId().toString())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET),

                // Markert som slettet
                new OppfolgingsBruker()
                        .setAktoer_id(randomAktorId().toString())
                        .setOppfolging(false)
                        .setEnhet_id(TEST_ENHET)
        );

        skrivBrukereTilTestindeks(brukere);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == brukere.size());

        BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
                TEST_ENHET,
                empty(),
                "asc",
                "ikke_satt",
                new Filtervalg(),
                null,
                null
        );

        assertThat(brukereMedAntall.getAntall()).isEqualTo(2);
    }

    @Test
    void skal_sette_brukere_med_veileder_fra_annen_enhet_til_ufordelt() {
        List<OppfolgingsBruker> brukere = List.of(
                new OppfolgingsBruker()
                        .setFnr(randomFnr().toString())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setAktiviteter(Set.of("foo"))
                        .setVeileder_id(TEST_VEILEDER_0),

                new OppfolgingsBruker()
                        .setFnr(randomFnr().toString())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setAktiviteter(Set.of("foo"))
                        .setVeileder_id(TEST_VEILEDER_1)
        );

        skrivBrukereTilTestindeks(brukere);

        var filtervalg = new Filtervalg().setFerdigfilterListe(List.of(I_AVTALT_AKTIVITET));
        pollElasticUntil(() -> elasticTestClient.countDocuments() == brukere.size());

        var response = elasticService.hentBrukere(
                TEST_ENHET,
                empty(),
                "asc",
                "ikke_satt",
                filtervalg,
                null,
                null
        );


        assertThat(response.getAntall()).isEqualTo(2);

        Bruker ufordeltBruker = response.getBrukere().stream()
                .filter(b -> TEST_VEILEDER_1.equals(b.getVeilederId()))
                .collect(toList()).get(0);

        assertThat(ufordeltBruker.isNyForEnhet()).isTrue();
    }

    @Test
    void skal_hente_ut_brukere_ved_soek_paa_flere_veiledere() {
        String now = Instant.now().toString();
        List<OppfolgingsBruker> brukere = List.of(
                new OppfolgingsBruker()
                        .setFnr(randomFnr().toString())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setNyesteutlopteaktivitet(now)
                        .setVeileder_id(TEST_VEILEDER_0),

                new OppfolgingsBruker()
                        .setFnr(randomFnr().toString())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setNyesteutlopteaktivitet(now)
                        .setVeileder_id(TEST_VEILEDER_1),

                new OppfolgingsBruker()
                        .setFnr(randomFnr().toString())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setNyesteutlopteaktivitet(now)
                        .setVeileder_id(null)

        );

        skrivBrukereTilTestindeks(brukere);

        var filtervalg = new Filtervalg()
                .setFerdigfilterListe(List.of(UTLOPTE_AKTIVITETER))
                .setVeiledere(List.of(TEST_VEILEDER_0, TEST_VEILEDER_1));


        pollElasticUntil(() -> elasticTestClient.countDocuments() == brukere.size());

        var response = elasticService.hentBrukere(TEST_ENHET, empty(), "asc", "ikke_satt", filtervalg, null, null);

        assertThat(response.getAntall()).isEqualTo(2);
    }

    @Test
    void skal_hente_riktig_antall_ufordelte_brukere() {

        List<OppfolgingsBruker> brukere = List.of(

                new OppfolgingsBruker()
                        .setAktoer_id(randomAktorId().toString())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setVeileder_id(null),

                new OppfolgingsBruker()
                        .setAktoer_id(randomAktorId().toString())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setVeileder_id(TEST_VEILEDER_0),

                new OppfolgingsBruker()
                        .setAktoer_id(randomAktorId().toString())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setVeileder_id(null)
        );

        when(veilarbVeilederClientMock.hentVeilederePaaEnhet(any())).thenReturn(List.of(TEST_VEILEDER_0));

        skrivBrukereTilTestindeks(brukere);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == brukere.size());

        var filtervalg = new Filtervalg().setFerdigfilterListe(List.of(UFORDELTE_BRUKERE));
        var response = elasticService.hentBrukere(TEST_ENHET, empty(), "asc", "ikke_satt", filtervalg, null, null);
        assertThat(response.getAntall()).isEqualTo(2);
    }

    @Test
    void skal_hente_riktige_antall_brukere_per_veileder() {

        var veilederId1 = "Z000000";
        var veilederId2 = "Z000001";
        var veilederId3 = "Z000003";

        List<OppfolgingsBruker> brukere = Stream.of(
                veilederId1,
                veilederId1,
                veilederId1,
                veilederId1,
                veilederId2,
                veilederId2,
                veilederId2,
                veilederId3,
                veilederId3,
                null
        )
                .map(id ->
                        new OppfolgingsBruker()
                                .setFnr(randomFnr().toString())
                                .setVeileder_id(id)
                                .setOppfolging(true)
                                .setEnhet_id(TEST_ENHET)
                )
                .collect(toList());

        skrivBrukereTilTestindeks(brukere);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == brukere.size());

        FacetResults portefoljestorrelser = elasticService.hentPortefoljestorrelser(TEST_ENHET);

        assertThat(facetResultCountForVeileder(veilederId1, portefoljestorrelser)).isEqualTo(4L);
        assertThat(facetResultCountForVeileder(veilederId2, portefoljestorrelser)).isEqualTo(3L);
        assertThat(facetResultCountForVeileder(veilederId3, portefoljestorrelser)).isEqualTo(2L);
    }

    @Test
    void skal_hente_ut_riktig_antall_brukere_med_arbeidsliste() {

        var brukerMedArbeidsliste =
                new OppfolgingsBruker()
                        .setFnr(randomFnr().toString())
                        .setOppfolging(true)
                        .setVeileder_id(TEST_VEILEDER_0)
                        .setEnhet_id(TEST_ENHET)
                        .setArbeidsliste_aktiv(true);


        var brukerUtenArbeidsliste =
                new OppfolgingsBruker()
                        .setFnr(randomFnr().toString())
                        .setOppfolging(true)
                        .setVeileder_id(TEST_VEILEDER_0)
                        .setEnhet_id(TEST_ENHET)
                        .setArbeidsliste_aktiv(false);
        var liste = List.of(brukerMedArbeidsliste, brukerUtenArbeidsliste);

        skrivBrukereTilTestindeks(liste);
        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        List<Bruker> brukereMedArbeidsliste = elasticService.hentBrukereMedArbeidsliste(TEST_VEILEDER_0, TEST_ENHET);
        assertThat(brukereMedArbeidsliste.size()).isEqualTo(1);
    }

    @Test
    void skal_hente_riktige_statustall_for_veileder() {

        var testBruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        var testBruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setFormidlingsgruppekode("IARBS")
                .setKvalifiseringsgruppekode("BATT")
                .setAktiviteter(Set.of("egen"))
                .setArbeidsliste_aktiv(true)
                .setNy_for_enhet(true)
                .setNy_for_veileder(true)
                .setTrenger_vurdering(true)
                .setVenterpasvarfranav("2018-05-09T22:00:00Z")
                .setNyesteutlopteaktivitet("2018-05-09T22:00:00Z");

        var inaktivBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setFormidlingsgruppekode("ISERV");

        var liste = List.of(testBruker1, testBruker2, inaktivBruker);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        var statustall = elasticService.hentStatusTallForVeileder(TEST_VEILEDER_0, TEST_ENHET);
        assertThat(statustall.erSykmeldtMedArbeidsgiver).isEqualTo(0);
        assertThat(statustall.iavtaltAktivitet).isEqualTo(1);
        assertThat(statustall.ikkeIavtaltAktivitet).isEqualTo(2);
        assertThat(statustall.inaktiveBrukere).isEqualTo(1);
        assertThat(statustall.minArbeidsliste).isEqualTo(1);
        assertThat(statustall.nyeBrukere).isEqualTo(1);
        assertThat(statustall.nyeBrukereForVeileder).isEqualTo(1);
        assertThat(statustall.trengerVurdering).isEqualTo(1);
        assertThat(statustall.venterPaSvarFraNAV).isEqualTo(1);
        assertThat(statustall.utlopteAktiviteter).isEqualTo(1);
    }

    @Test
    void skal_hente_riktige_statustall_for_enhet() {

        var brukerUtenVeileder = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET);

        var brukerMedVeileder = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        var liste = List.of(brukerMedVeileder, brukerUtenVeileder);


        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        when(veilarbVeilederClientMock.hentVeilederePaaEnhet(any())).thenReturn(List.of(TEST_VEILEDER_0));

        var statustall = elasticService.hentStatusTallForEnhet(TEST_ENHET);
        assertThat(statustall.getUfordelteBrukere()).isEqualTo(1);
    }

    @Test
    void skal_sortere_brukere_pa_arbeidslisteikon() {

        var blaBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setArbeidsliste_aktiv(true)
                .setArbeidsliste_kategori(Arbeidsliste.Kategori.BLA.name());

        var lillaBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setArbeidsliste_aktiv(true)
                .setArbeidsliste_kategori(Arbeidsliste.Kategori.LILLA.name());

        var liste = List.of(blaBruker, lillaBruker);

        skrivBrukereTilTestindeks(blaBruker, lillaBruker);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                "desc",
                "arbeidslistekategori",
                new Filtervalg(),
                null,
                null
        );

        List<Bruker> brukere = brukereMedAntall.getBrukere();

        assertThat(brukere.size()).isEqualTo(2);
        assertThat(brukere.get(0).getArbeidsliste().getKategori()).isEqualTo(Arbeidsliste.Kategori.LILLA);
        assertThat(brukere.get(1).getArbeidsliste().getKategori()).isEqualTo(Arbeidsliste.Kategori.BLA);

    }

    @Test
    void skal_sortere_brukere_pa_aktivteter() {
        String tidspunkt1 = toIsoUTC(ZonedDateTime.now().plusDays(1));
        String tidspunkt2 = toIsoUTC(ZonedDateTime.now().plusDays(2));
        String tidspunkt3 = toIsoUTC(ZonedDateTime.now().plusDays(3));

        var tidligstfristBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setAktivitet_egen_utlopsdato(tidspunkt3)
                .setAktivitet_mote_utlopsdato(tidspunkt1)
                .setAktiviteter(Set.of("EGEN", "MOTE"));

        var senestFristBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setAktivitet_egen_utlopsdato(tidspunkt2)
                .setAktiviteter(Set.of("EGEN", "MOTE"));

        var nullBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET);

        var liste = List.of(tidligstfristBruker, senestFristBruker, nullBruker);

        skrivBrukereTilTestindeks(tidligstfristBruker, senestFristBruker, nullBruker);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        Filtervalg filtervalg1 = new Filtervalg()
                .setAktiviteterForenklet(List.of("EGEN", "MOTE"))
                .setFerdigfilterListe(List.of());
        Filtervalg filtervalg2 = new Filtervalg()
                .setAktiviteterForenklet(List.of("MOTE", "EGEN"))
                .setFerdigfilterListe(List.of());

        BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                "desc",
                "valgteaktiviteter",
                filtervalg1,
                null,
                null
        );
        BrukereMedAntall brukereMedAntall2 = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                "desc",
                "valgteaktiviteter",
                filtervalg2,
                null,
                null
        );

        List<Bruker> brukere1 = brukereMedAntall.getBrukere();
        List<Bruker> brukere2 = brukereMedAntall2.getBrukere();

        // brukere1 Filter: List.of("EGEN", "MOTE"))
        // brukere2 Filter: List.of("MOTE", "EGEN"))
        assertThat(brukere1.size()).isEqualTo(2);
        assertThat(brukere1.get(1).getFnr()).isEqualTo(brukere2.get(1).getFnr());
        assertThat(brukere1.get(0).getFnr()).isEqualTo(brukere2.get(0).getFnr());

        // Generell sortering:
        assertThat(brukere1.size()).isEqualTo(2);
        assertThat(brukere1.get(1).getFnr()).isEqualTo(tidligstfristBruker.getFnr());
        assertThat(brukere1.get(0).getFnr()).isEqualTo(senestFristBruker.getFnr());
    }

    @Test
    void skal_hente_brukere_som_trenger_vurdering_og_er_ny_for_enhet() {

        var nyForEnhet = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_enhet(true)
                .setTrenger_vurdering(true);

        var ikkeNyForEnhet = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_enhet(true)
                .setTrenger_vurdering(false);


        var liste = List.of(nyForEnhet, ikkeNyForEnhet);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        List<Brukerstatus> ferdigFiltere = List.of(
                NYE_BRUKERE,
                TRENGER_VURDERING
        );

        var response = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                "asc",
                "ikke_satt",
                new Filtervalg().setFerdigfilterListe(ferdigFiltere),
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(userExistsInResponse(nyForEnhet, response)).isTrue();
        assertThat(userExistsInResponse(ikkeNyForEnhet, response)).isFalse();
    }

    @Test
    void skal_ikke_kunne_hente_brukere_veileder_ikke_har_tilgang_til() {
        var brukerVeilederHarTilgangTil = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        var brukerVeilederIkkeHarTilgangTil = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id("NEGA_$testEnhet")
                .setVeileder_id("NEGA_$testVeileder");

        var liste = List.of(brukerVeilederHarTilgangTil, brukerVeilederIkkeHarTilgangTil);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());


        var response = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                "asc",
                "ikke_satt",
                new Filtervalg(),
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(userExistsInResponse(brukerVeilederHarTilgangTil, response)).isTrue();
        assertThat(userExistsInResponse(brukerVeilederIkkeHarTilgangTil, response)).isFalse();
    }

    @Test
    void skal_anse_bruker_som_ufordelt_om_bruker_har_veileder_som_ikke_har_tilgang_til_enhet() {
        when(veilarbVeilederClientMock.hentVeilederePaaEnhet(any())).thenReturn(List.of(TEST_VEILEDER_0));

        var brukerMedUfordeltStatus = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(LITE_PRIVILEGERT_VEILEDER)
                .setNy_for_enhet(false);

        var brukerMedFordeltStatus = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_enhet(false);

        var liste = List.of(brukerMedUfordeltStatus, brukerMedFordeltStatus);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());


        var response = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.of(LITE_PRIVILEGERT_VEILEDER),
                "asc",
                "ikke_satt",
                new Filtervalg().setFerdigfilterListe(List.of(UFORDELTE_BRUKERE)),
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(veilederExistsInResponse(LITE_PRIVILEGERT_VEILEDER, response)).isTrue();

        StatusTall statustall = elasticService.hentStatusTallForEnhet(TEST_ENHET);
        assertThat(statustall.ufordelteBrukere).isEqualTo(1);
    }

    @Test
    void skal_returnere_brukere_basert_på_fødselsdag_i_måneden() {
        var testBruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setFodselsdag_i_mnd(7)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        var testBruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setFodselsdag_i_mnd(8)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);


        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setFodselsdagIMnd(List.of("7"));

        var liste = List.of(testBruker1, testBruker2);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        var response = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                "asc",
                "ikke_satt",
                filterValg,
                null,
                null

        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(response.getBrukere().stream().anyMatch(it -> it.getFodselsdagIMnd() == 7)).isTrue();
    }

    @Test
    void skal_hente_ut_brukere_basert_på_kjønn() {
        var mann = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setKjonn("M");

        var kvinne = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setKjonn("K");

        var liste = List.of(kvinne, mann);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setKjonn(Kjonn.K);

        var response = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                "asc",
                "ikke_satt",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(response.getBrukere().stream().anyMatch(bruker -> "K".equals(bruker.getKjonn()))).isTrue();
    }

    @Test
    void skal_hente_ut_brukere_som_går_på_arbeidsavklaringspenger() {
        var brukerMedAAP = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.AAP.name());

        var brukerUtenAAP = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.DAGP.name());


        var liste = List.of(brukerMedAAP, brukerUtenAAP);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setRettighetsgruppe(List.of(Rettighetsgruppe.AAP));

        var response = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                "asc",
                "ikke_satt",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(userExistsInResponse(brukerMedAAP, response)).isTrue();
        assertThat(userExistsInResponse(brukerUtenAAP, response)).isFalse();

    }

    @Test
    void skal_hente_ut_brukere_filtrert_på_dagpenger_som_ytelse() {

        var brukerMedDagpengerMedPermittering = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.AAP.name())
                .setYtelse(YtelseMapping.DAGPENGER_MED_PERMITTERING.name());


        var brukerMedPermitteringFiskeindustri = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.AAP.name())
                .setYtelse(YtelseMapping.DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI.name());

        var brukerMedAAP = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.DAGP.name())
                .setYtelse(YtelseMapping.AAP_MAXTID.name());

        var brukerMedAnnenVeileder = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(LITE_PRIVILEGERT_VEILEDER)
                .setRettighetsgruppekode(Rettighetsgruppe.AAP.name())
                .setYtelse(YtelseMapping.DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI.name());

        var liste = List.of(brukerMedDagpengerMedPermittering, brukerMedPermitteringFiskeindustri, brukerMedAAP, brukerMedAnnenVeileder);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setYtelse(YtelseFilter.DAGPENGER);

        var response = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                "asc",
                "ikke_satt",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(2);
        assertThat(userExistsInResponse(brukerMedDagpengerMedPermittering, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedPermitteringFiskeindustri, response)).isTrue();

    }

    @Test
    void skal_hente_ut_brukere_som_har_avtale_om_å_søke_jobber() {
        var brukerMedSokeAvtale = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("sokeavtale"));

        var brukerMedBehandling = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("behandling"));

        var brukerMedUtenAktiviteter = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);


        var liste = List.of(brukerMedSokeAvtale, brukerMedUtenAktiviteter, brukerMedBehandling);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(Map.of("SOKEAVTALE", JA));

        var response = elasticService.hentBrukere(
                TEST_ENHET,
                empty(),
                "asc",
                "ikke_satt",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(userExistsInResponse(brukerMedSokeAvtale, response)).isTrue();
    }

    @Test
    void skal_hente_ut_alle_brukere_unntatt_de_som_har_avtale_om_å_søke_jobber() {

        var brukerMedSokeAvtale = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("sokeavtale"));

        var brukerMedBehandling = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("behandling"));

        var brukerMedUtenAktiviteter = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().toString())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);

        var liste = List.of(brukerMedSokeAvtale, brukerMedUtenAktiviteter, brukerMedBehandling);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(Map.of("SOKEAVTALE", AktivitetFiltervalg.NEI));

        var response = elasticService.hentBrukere(
                TEST_ENHET,
                empty(),
                "asc",
                "ikke_satt",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(2);
        assertThat(userExistsInResponse(brukerMedBehandling, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedUtenAktiviteter, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedSokeAvtale, response)).isFalse();
    }

    @Test
    void skal_hente_ut_alle_brukere_med_tiltak() {

        var brukerMedTiltak = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("tiltak"));

        var brukerMedBehandling = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("behandling"));

        var brukerUtenAktiviteter = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);

        var liste = List.of(brukerMedTiltak, brukerMedBehandling, brukerUtenAktiviteter);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(Map.of("TILTAK", JA));

        var response = elasticService.hentBrukere(
                TEST_ENHET,
                empty(),
                "asc",
                "ikke_satt",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(userExistsInResponse(brukerMedTiltak, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedBehandling, response)).isFalse();
        assertThat(userExistsInResponse(brukerUtenAktiviteter, response)).isFalse();
    }

    @Test
    void skal_hente_ut_alle_brukere_som_ikke_har_tiltak() {
        var brukerMedTiltak = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("tiltak"))
                .setTiltak(Set.of("VASV"));

        var brukerMedBehandling = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("behandling"));

        var brukerUtenAktiviteter = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);

        var liste = List.of(brukerMedTiltak, brukerMedBehandling, brukerUtenAktiviteter);

        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(Map.of("TILTAK", AktivitetFiltervalg.NEI));

        var response = elasticService.hentBrukere(
                TEST_ENHET,
                empty(),
                "asc",
                "ikke_satt",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(2);
        assertThat(userExistsInResponse(brukerMedBehandling, response)).isTrue();
        assertThat(userExistsInResponse(brukerUtenAktiviteter, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedTiltak, response)).isFalse();
    }

    @Test
    public void skal_hente_alle_brukere_som_har_vedtak() {
        var brukerMedVedtak = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setVedtak_status("Utkast")
                .setAnsvarlig_veileder_for_vedtak("BVeileder");

        var brukerMedVedtak1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setVedtak_status("Venter på tilbakemelding")
                .setAnsvarlig_veileder_for_vedtak("CVeileder");

        var brukerMedVedtak2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setVedtak_status("Venter på tilbakemelding")
                .setAnsvarlig_veileder_for_vedtak("AVeileder");

        var brukerMedVedtakUtenAnsvarligVeileder = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setVedtak_status("Utkast");

        var brukerUtenVedtak = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("egen"));

        var liste = List.of(brukerMedVedtak, brukerMedVedtak1, brukerMedVedtak2, brukerMedVedtakUtenAnsvarligVeileder, brukerUtenVedtak);

        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of(UNDER_VURDERING));

        var response = elasticService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "ansvarlig_veileder_for_vedtak",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(4);
        assertThat(userExistsInResponse(brukerMedVedtak, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedVedtak1, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedVedtak2, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedVedtakUtenAnsvarligVeileder, response)).isTrue();

        assertThat(response.getBrukere().get(0).getAnsvarligVeilederForVedtak()).isEqualTo("AVeileder");
        assertThat(response.getBrukere().get(1).getAnsvarligVeilederForVedtak()).isEqualTo("BVeileder");
        assertThat(response.getBrukere().get(2).getAnsvarligVeilederForVedtak()).isEqualTo("CVeileder");
        assertThat(response.getBrukere().get(3).getAnsvarligVeilederForVedtak()).isNull();
    }

    private boolean veilederExistsInResponse(String veilederId, BrukereMedAntall brukere) {
        return brukere.getBrukere().stream().anyMatch(bruker -> veilederId.equals(bruker.getVeilederId()));
    }

    private boolean userExistsInResponse(OppfolgingsBruker bruker, BrukereMedAntall brukere) {
        return brukere.getBrukere().stream().anyMatch(b -> bruker.getFnr().equals(b.getFnr()));
    }

    private Long facetResultCountForVeileder(String testVeileder1, FacetResults portefoljestorrelser) {
        return portefoljestorrelser.getFacetResults().stream().filter(it -> testVeileder1.equals(it.getValue())).map(Facet::getCount).collect(toList()).get(0);
    }

    private void skrivBrukereTilTestindeks(List<OppfolgingsBruker> brukere) {
        OppfolgingsBruker[] array = new OppfolgingsBruker[brukere.size()];
        skrivBrukereTilTestindeks(brukere.toArray(array));
    }

    @SneakyThrows
    private void skrivBrukereTilTestindeks(OppfolgingsBruker... brukere) {
        elasticIndexer.skrivTilIndeks(indexName.getValue(), List.of(brukere));
    }

}
