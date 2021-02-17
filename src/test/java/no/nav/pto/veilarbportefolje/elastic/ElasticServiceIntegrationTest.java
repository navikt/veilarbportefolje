package no.nav.pto.veilarbportefolje.elastic;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg.JA;
import static no.nav.pto.veilarbportefolje.domene.Brukerstatus.*;
import static no.nav.pto.veilarbportefolje.util.ElasticTestClient.pollElasticUntil;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ElasticServiceIntegrationTest extends EndToEndTest {

    private static final String TEST_ENHET = "0000";
    private static final String TEST_VEILEDER_0 = "Z000000";
    private static final String TEST_VEILEDER_1 = "Z000001";
    private static final String LITE_PRIVILEGERT_VEILEDER = "Z000001";

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

        val filtervalg = new Filtervalg().setFerdigfilterListe(List.of(I_AVTALT_AKTIVITET));
        pollElasticUntil(() -> elasticTestClient.countDocuments() == brukere.size());

        val response = elasticService.hentBrukere(
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

        val filtervalg = new Filtervalg()
                .setFerdigfilterListe(List.of(UTLOPTE_AKTIVITETER))
                .setVeiledere(List.of(TEST_VEILEDER_0, TEST_VEILEDER_1));


        pollElasticUntil(() -> elasticTestClient.countDocuments() == brukere.size());

        val response = elasticService.hentBrukere(TEST_ENHET, empty(), "asc", "ikke_satt", filtervalg, null, null);

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

        when(veilarbVeilederClientMock.hentVeilederePaaEnhet(anyString())).thenReturn(List.of(TEST_VEILEDER_0));

        skrivBrukereTilTestindeks(brukere);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == brukere.size());

        val filtervalg = new Filtervalg().setFerdigfilterListe(List.of(UFORDELTE_BRUKERE));
        val response = elasticService.hentBrukere(TEST_ENHET, empty(), "asc", "ikke_satt", filtervalg, null, null);
        assertThat(response.getAntall()).isEqualTo(2);
    }

    @Test
    void skal_hente_riktige_antall_brukere_per_veileder() {

        val veilederId1 = "Z000000";
        val veilederId2 = "Z000001";
        val veilederId3 = "Z000003";

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

        val brukerMedArbeidsliste =
                new OppfolgingsBruker()
                        .setFnr(randomFnr().toString())
                        .setOppfolging(true)
                        .setVeileder_id(TEST_VEILEDER_0)
                        .setEnhet_id(TEST_ENHET)
                        .setArbeidsliste_aktiv(true);


        val brukerUtenArbeidsliste =
                new OppfolgingsBruker()
                        .setFnr(randomFnr().toString())
                        .setOppfolging(true)
                        .setVeileder_id(TEST_VEILEDER_0)
                        .setEnhet_id(TEST_ENHET)
                        .setArbeidsliste_aktiv(false);
        val liste = List.of(brukerMedArbeidsliste, brukerUtenArbeidsliste);

        skrivBrukereTilTestindeks(liste);
        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        List<Bruker> brukereMedArbeidsliste = elasticService.hentBrukereMedArbeidsliste(TEST_VEILEDER_0, TEST_ENHET);
        assertThat(brukereMedArbeidsliste.size()).isEqualTo(1);
    }

    @Test
    void skal_hente_riktige_statustall_for_veileder() {

        val testBruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        val testBruker2 = new OppfolgingsBruker()
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

        val inaktivBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setFormidlingsgruppekode("ISERV");

        val liste = List.of(testBruker1, testBruker2, inaktivBruker);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        val statustall = elasticService.hentStatusTallForVeileder(TEST_VEILEDER_0, TEST_ENHET);
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

        val brukerUtenVeileder = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET);

        val brukerMedVeileder = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        val liste = List.of(brukerMedVeileder, brukerUtenVeileder);


        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        when(veilarbVeilederClientMock.hentVeilederePaaEnhet(anyString())).thenReturn(List.of(TEST_VEILEDER_0));

        val statustall = elasticService.hentStatusTallForEnhet(TEST_ENHET);
        assertThat(statustall.getUfordelteBrukere()).isEqualTo(1);
    }

    @Test
    void skal_sortere_brukere_pa_arbeidslisteikon() {

        val blaBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setArbeidsliste_aktiv(true)
                .setArbeidsliste_kategori(Arbeidsliste.Kategori.BLA.name());

        val lillaBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setArbeidsliste_aktiv(true)
                .setArbeidsliste_kategori(Arbeidsliste.Kategori.LILLA.name());

        val liste = List.of(blaBruker, lillaBruker);

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
    void skal_hente_brukere_som_trenger_vurdering_og_er_ny_for_enhet() {

        val nyForEnhet = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_enhet(true)
                .setTrenger_vurdering(true);

        val ikkeNyForEnhet = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_enhet(true)
                .setTrenger_vurdering(false);


        val liste = List.of(nyForEnhet, ikkeNyForEnhet);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        List<Brukerstatus> ferdigFiltere = List.of(
                NYE_BRUKERE,
                TRENGER_VURDERING
        );

        val response = elasticService.hentBrukere(
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
        val brukerVeilederHarTilgangTil = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        val brukerVeilederIkkeHarTilgangTil = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id("NEGA_$testEnhet")
                .setVeileder_id("NEGA_$testVeileder");

        val liste = List.of(brukerVeilederHarTilgangTil, brukerVeilederIkkeHarTilgangTil);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());


        val response = elasticService.hentBrukere(
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

        val brukerMedUfordeltStatus = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(LITE_PRIVILEGERT_VEILEDER)
                .setNy_for_enhet(false);

        val brukerMedFordeltStatus = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_enhet(false);

        val liste = List.of(brukerMedUfordeltStatus, brukerMedFordeltStatus);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());


        val response = elasticService.hentBrukere(
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
        val testBruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setFodselsdag_i_mnd(7)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        val testBruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setFodselsdag_i_mnd(8)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);


        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setFodselsdagIMnd(List.of("7"));

        val liste = List.of(testBruker1, testBruker2);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        val response = elasticService.hentBrukere(
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
        val mann = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setKjonn("M");

        val kvinne = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setKjonn("K");

        val liste = List.of(kvinne, mann);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setKjonn(Kjonn.K);

        val response = elasticService.hentBrukere(
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
        val brukerMedAAP = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.AAP.name());

        val brukerUtenAAP = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.DAGP.name());


        val liste = List.of(brukerMedAAP, brukerUtenAAP);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setRettighetsgruppe(List.of(Rettighetsgruppe.AAP));

        val response = elasticService.hentBrukere(
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

        val brukerMedDagpengerMedPermittering = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.AAP.name())
                .setYtelse(YtelseMapping.DAGPENGER_MED_PERMITTERING.name());


        val brukerMedPermitteringFiskeindustri = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.AAP.name())
                .setYtelse(YtelseMapping.DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI.name());

        val brukerMedAAP = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.DAGP.name())
                .setYtelse(YtelseMapping.AAP_MAXTID.name());

        val brukerMedAnnenVeileder = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(LITE_PRIVILEGERT_VEILEDER)
                .setRettighetsgruppekode(Rettighetsgruppe.AAP.name())
                .setYtelse(YtelseMapping.DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI.name());

        val liste = List.of(brukerMedDagpengerMedPermittering, brukerMedPermitteringFiskeindustri, brukerMedAAP, brukerMedAnnenVeileder);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setYtelse(YtelseFilter.DAGPENGER);

        val response = elasticService.hentBrukere(
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
        val brukerMedSokeAvtale = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("sokeavtale"));

        val brukerMedBehandling = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("behandling"));

        val brukerMedUtenAktiviteter = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);


        val liste = List.of(brukerMedSokeAvtale, brukerMedUtenAktiviteter, brukerMedBehandling);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(Map.of("SOKEAVTALE", JA));

        val response = elasticService.hentBrukere(
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

        val brukerMedSokeAvtale = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("sokeavtale"));

        val brukerMedBehandling = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("behandling"));

        val brukerMedUtenAktiviteter = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().toString())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);

        val liste = List.of(brukerMedSokeAvtale, brukerMedUtenAktiviteter, brukerMedBehandling);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(Map.of("SOKEAVTALE", AktivitetFiltervalg.NEI));

        val response = elasticService.hentBrukere(
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

        val brukerMedTiltak = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("tiltak"));

        val brukerMedBehandling = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("behandling"));

        val brukerUtenAktiviteter = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);

        val liste = List.of(brukerMedTiltak, brukerMedBehandling, brukerUtenAktiviteter);
        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(Map.of("TILTAK", JA));

        val response = elasticService.hentBrukere(
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
        val brukerMedTiltak = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("tiltak"))
                .setTiltak(Set.of("VASV"));

        val brukerMedBehandling = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("behandling"));

        val brukerUtenAktiviteter = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);

        val liste = List.of(brukerMedTiltak, brukerMedBehandling, brukerUtenAktiviteter);

        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(Map.of("TILTAK", AktivitetFiltervalg.NEI));

        val response = elasticService.hentBrukere(
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
    public void skal_hente_alle_brukere_som_har_vedtak(){
        val brukerMedVedtak = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setVedtak_status("Utkast")
                .setVedtak_status_endret("2021-01-01T12:57:41+00:00")
                .setAnsvarlig_veileder_for_vedtak("BVeileder");

        val brukerMedVedtak1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setVedtak_status("Venter på tilbakemelding")
                .setVedtak_status_endret("2021-03-01T15:13:41+00:00")
                .setAnsvarlig_veileder_for_vedtak("CVeileder");

        val brukerMedVedtak2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setVedtak_status("Venter på tilbakemelding")
                .setVedtak_status_endret("2021-06-11T13:13:33+00:00")
                .setAnsvarlig_veileder_for_vedtak("AVeileder");

        val brukerUtenVedtak = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("egen"));

        val liste = List.of(brukerMedVedtak, brukerMedVedtak1, brukerMedVedtak2, brukerUtenVedtak);

        skrivBrukereTilTestindeks(liste);

        pollElasticUntil(() -> elasticTestClient.countDocuments() == liste.size());

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of(UNDER_VURDERING));

        val response = elasticService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "ansvarligveilederforvedtak",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(3);
        assertThat(userExistsInResponse(brukerMedVedtak, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedVedtak1, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedVedtak2, response)).isTrue();

        assertThat(response.getBrukere().get(0).getAnsvarligVeilederForVedtak()).isEqualTo("AVeileder");
        assertThat(response.getBrukere().get(1).getAnsvarligVeilederForVedtak()).isEqualTo("BVeileder");
        assertThat(response.getBrukere().get(2).getAnsvarligVeilederForVedtak()).isEqualTo("CVeileder");
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
