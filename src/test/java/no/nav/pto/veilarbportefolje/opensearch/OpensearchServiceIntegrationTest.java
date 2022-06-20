package no.nav.pto.veilarbportefolje.opensearch;

import lombok.SneakyThrows;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.opensearch.domene.OpensearchResponse;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
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
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchQueryBuilder.byggArbeidslisteQuery;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static no.nav.pto.veilarbportefolje.util.OpensearchTestClient.pollOpensearchUntil;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class OpensearchServiceIntegrationTest extends EndToEndTest {
    private static String TEST_ENHET = randomNavKontor().getValue();
    private static final String TEST_VEILEDER_0 = randomVeilederId().getValue();
    private static final String TEST_VEILEDER_1 = randomVeilederId().getValue();
    private static final String LITE_PRIVILEGERT_VEILEDER = randomVeilederId().getValue();

    private final OpensearchService opensearchService;
    private final OpensearchIndexer opensearchIndexer;
    private final VeilarbVeilederClient veilarbVeilederClientMock;


    @Autowired
    public OpensearchServiceIntegrationTest(OpensearchService opensearchService, OpensearchIndexer opensearchIndexer, VeilarbVeilederClient veilarbVeilederClientMock) {
        this.opensearchService = opensearchService;
        this.opensearchIndexer = opensearchIndexer;
        this.veilarbVeilederClientMock = veilarbVeilederClientMock;
    }

    @BeforeEach
    void byttenhet() {
        TEST_ENHET = randomNavKontor().getValue();
    }

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

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == brukere.size());

        BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(
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
                        .setAktoer_id(randomAktorId().toString())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setAktiviteter(Set.of("foo"))
                        .setVeileder_id(TEST_VEILEDER_0),

                new OppfolgingsBruker()
                        .setFnr(randomFnr().toString())
                        .setAktoer_id(randomAktorId().toString())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setAktiviteter(Set.of("foo"))
                        .setVeileder_id(TEST_VEILEDER_1)
        );

        skrivBrukereTilTestindeks(brukere);

        var filtervalg = new Filtervalg().setFerdigfilterListe(List.of(I_AVTALT_AKTIVITET));
        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == brukere.size());

        var response = opensearchService.hentBrukere(
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


        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == brukere.size());

        var response = opensearchService.hentBrukere(TEST_ENHET, empty(), "asc", "ikke_satt", filtervalg, null, null);

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
                        .setVeileder_id(LITE_PRIVILEGERT_VEILEDER)
        );

        when(veilarbVeilederClientMock.hentVeilederePaaEnhet(any())).thenReturn(List.of(TEST_VEILEDER_0));

        skrivBrukereTilTestindeks(brukere);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == brukere.size());

        var filtervalg = new Filtervalg().setFerdigfilterListe(List.of(UFORDELTE_BRUKERE));
        var response = opensearchService.hentBrukere(TEST_ENHET, empty(), "asc", "ikke_satt", filtervalg, null, null);
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

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == brukere.size());

        FacetResults portefoljestorrelser = opensearchService.hentPortefoljestorrelser(TEST_ENHET);

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
        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        SearchSourceBuilder request = byggArbeidslisteQuery(TEST_ENHET, TEST_VEILEDER_0);
        OpensearchResponse response = opensearchService.search(request, indexName.getValue(), OpensearchResponse.class);
        assertThat(response.hits().getTotal().getValue()).isEqualTo(1);
    }

    @Test
    void skal_hente_riktige_statustall_for_veileder() {
        var testBruker1 = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().toString())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        var testBruker2 = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().toString())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setFormidlingsgruppekode("IARBS")
                .setKvalifiseringsgruppekode("BATT")
                .setAktiviteter(Set.of("egen"))
                .setArbeidsliste_aktiv(true)
                .setNy_for_veileder(true)
                .setTrenger_vurdering(true)
                .setVenterpasvarfranav("2018-05-09T22:00:00Z")
                .setNyesteutlopteaktivitet("2018-05-09T22:00:00Z");

        var inaktivBruker = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().toString())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setFormidlingsgruppekode("ISERV");

        var liste = List.of(testBruker1, testBruker2, inaktivBruker);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                "asc",
                "ikke_satt",
                new Filtervalg().setFerdigfilterListe(List.of()),
                null,
                null
        ).getAntall() == liste.size());

        var statustall = opensearchService.hentStatusTallForVeileder(TEST_VEILEDER_0, TEST_ENHET);
        assertThat(statustall.erSykmeldtMedArbeidsgiver).isEqualTo(0);
        assertThat(statustall.iavtaltAktivitet).isEqualTo(1);
        assertThat(statustall.ikkeIavtaltAktivitet).isEqualTo(2);
        assertThat(statustall.inaktiveBrukere).isEqualTo(1);
        assertThat(statustall.minArbeidsliste).isEqualTo(1);
        assertThat(statustall.nyeBrukereForVeileder).isEqualTo(1);
        assertThat(statustall.trengerVurdering).isEqualTo(1);
        assertThat(statustall.venterPaSvarFraNAV).isEqualTo(1);
        assertThat(statustall.utlopteAktiviteter).isEqualTo(1);
    }

    @Test
    void skal_hente_riktige_statustall_for_enhet() {

        var brukerUtenVeileder = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().get())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET);

        var brukerMedVeileder = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().get())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        var liste = List.of(brukerMedVeileder, brukerUtenVeileder);


        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        when(veilarbVeilederClientMock.hentVeilederePaaEnhet(any())).thenReturn(List.of(TEST_VEILEDER_0));

        var statustall = opensearchService.hentStatusTallForEnhet(TEST_ENHET);
        assertThat(statustall.getUfordelteBrukere()).isEqualTo(1);
    }

    @Test
    void skal_sortere_brukere_pa_arbeidslisteikon() {

        var blaBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setArbeidsliste_aktiv(true)
                .setArbeidsliste_kategori(Arbeidsliste.Kategori.BLA.name());

        var lillaBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setArbeidsliste_aktiv(true)
                .setArbeidsliste_kategori(Arbeidsliste.Kategori.LILLA.name());

        var liste = List.of(blaBruker, lillaBruker);

        skrivBrukereTilTestindeks(blaBruker, lillaBruker);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(
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
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setAktivitet_egen_utlopsdato(tidspunkt3)
                .setAktivitet_mote_utlopsdato(tidspunkt1)
                .setAktiviteter(Set.of("EGEN", "MOTE"));

        var senestFristBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setAktivitet_egen_utlopsdato(tidspunkt2)
                .setAktiviteter(Set.of("EGEN", "MOTE"));

        var nullBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET);

        var liste = List.of(tidligstfristBruker, senestFristBruker, nullBruker);

        skrivBrukereTilTestindeks(tidligstfristBruker, senestFristBruker, nullBruker);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        Filtervalg filtervalg1 = new Filtervalg()
                .setAktiviteterForenklet(List.of("EGEN", "MOTE"))
                .setFerdigfilterListe(List.of());
        Filtervalg filtervalg2 = new Filtervalg()
                .setAktiviteterForenklet(List.of("MOTE", "EGEN"))
                .setFerdigfilterListe(List.of());

        BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                "desc",
                "valgteaktiviteter",
                filtervalg1,
                null,
                null
        );
        BrukereMedAntall brukereMedAntall2 = opensearchService.hentBrukere(
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
        when(veilarbVeilederClientMock.hentVeilederePaaEnhet(any())).thenReturn(List.of(TEST_VEILEDER_0));
        var nyForEnhet = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(LITE_PRIVILEGERT_VEILEDER)
                .setTrenger_vurdering(true);

        var ikkeNyForEnhet = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setTrenger_vurdering(true);


        var liste = List.of(nyForEnhet, ikkeNyForEnhet);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                "asc",
                "ikke_satt",
                new Filtervalg().setFerdigfilterListe(List.of(TRENGER_VURDERING)),
                null,
                null
        ).getAntall() == liste.size());

        List<Brukerstatus> ferdigFiltere = List.of(
                UFORDELTE_BRUKERE,
                TRENGER_VURDERING
        );

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
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

        pollOpensearchUntil(() -> opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                "asc",
                "ikke_satt",
                new Filtervalg(),
                null,
                null
        ).getAntall() == 1);


        var response = opensearchService.hentBrukere(
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
                .setVeileder_id(LITE_PRIVILEGERT_VEILEDER);

        var brukerMedFordeltStatus = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        var liste = List.of(brukerMedUfordeltStatus, brukerMedFordeltStatus);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());


        var response = opensearchService.hentBrukere(
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

        StatusTall statustall = opensearchService.hentStatusTallForEnhet(TEST_ENHET);
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

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var response = opensearchService.hentBrukere(
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

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setKjonn(Kjonn.K);

        var response = opensearchService.hentBrukere(
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

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setRettighetsgruppe(List.of(Rettighetsgruppe.AAP));

        var response = opensearchService.hentBrukere(
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

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setYtelse(YtelseFilter.DAGPENGER);

        var response = opensearchService.hentBrukere(
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

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(Map.of("SOKEAVTALE", JA));

        var response = opensearchService.hentBrukere(
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

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(Map.of("SOKEAVTALE", AktivitetFiltervalg.NEI));

        var response = opensearchService.hentBrukere(
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

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(Map.of("TILTAK", JA));

        var response = opensearchService.hentBrukere(
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
                .setAktoer_id(randomAktorId().toString())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("tiltak"))
                .setTiltak(Set.of("VASV"));

        var brukerMedBehandling = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().toString())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("behandling"));

        var brukerUtenAktiviteter = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().toString())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);

        var liste = List.of(brukerMedTiltak, brukerMedBehandling, brukerUtenAktiviteter);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(Map.of("TILTAK", AktivitetFiltervalg.NEI));

        var response = opensearchService.hentBrukere(
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
                .setUtkast_14a_status("Utkast")
                .setUtkast_14a_ansvarlig_veileder("BVeileder");

        var brukerMedVedtak1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setUtkast_14a_status("Venter på tilbakemelding")
                .setUtkast_14a_ansvarlig_veileder("CVeileder");

        var brukerMedVedtak2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setUtkast_14a_status("Venter på tilbakemelding")
                .setUtkast_14a_ansvarlig_veileder("AVeileder");

        var brukerMedVedtakUtenAnsvarligVeileder = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setUtkast_14a_status("Utkast");

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

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of(UNDER_VURDERING));

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "utkast_14a_ansvarlig_veileder",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(4);
        assertThat(userExistsInResponse(brukerMedVedtak, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedVedtak1, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedVedtak2, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedVedtakUtenAnsvarligVeileder, response)).isTrue();

        assertThat(response.getBrukere().get(0).getUtkast14aAnsvarligVeileder()).isEqualTo("AVeileder");
        assertThat(response.getBrukere().get(1).getUtkast14aAnsvarligVeileder()).isEqualTo("BVeileder");
        assertThat(response.getBrukere().get(2).getUtkast14aAnsvarligVeileder()).isEqualTo("CVeileder");
        assertThat(response.getBrukere().get(3).getUtkast14aAnsvarligVeileder()).isNull();
    }

    @Test
    public void skal_hente_alle_brukere_som_har_tolkbehov() {
        var brukerMedTalkBehov1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setTalespraaktolk("JPN")
                .setTolkBehovSistOppdatert(LocalDate.parse("2022-02-22"));

        var brukerMedTalkBehov2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setTalespraaktolk("SWE")
                .setTegnspraaktolk("SWE")
                .setTolkBehovSistOppdatert(LocalDate.parse("2021-03-23"));

        var brukerUttenTalkBehov = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET);

        var liste = List.of(brukerMedTalkBehov1, brukerMedTalkBehov2, brukerUttenTalkBehov);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setTalespraaktolk(Boolean.TRUE);

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "ikke_satt",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(2);
        assertThat(response.getBrukere().stream().filter(x -> x.getTalespraaktolk().equals("JPN")).filter(x -> x.getTolkBehovSistOppdatert().equals("2022-02-22")).findFirst().isPresent());
        assertThat(response.getBrukere().stream().filter(x -> x.getTalespraaktolk().equals("SWE")).filter(x -> x.getTolkBehovSistOppdatert().equals("2021-03-23")).findFirst().isPresent());


        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setTegnspraaktolk(Boolean.TRUE);

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "ikke_satt",
                filterValg,
                null,
                null
        );
        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(response.getBrukere().stream().filter(x -> x.getTalespraaktolk().equals("SWE")).filter(x -> x.getTolkBehovSistOppdatert().equals("2021-03-23")).findFirst().isPresent());


        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setTolkBehovSpraak("JPN");

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "ikke_satt",
                filterValg,
                null,
                null
        );
        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(response.getBrukere().stream().filter(x -> x.getTalespraaktolk().equals("JPN")).filter(x -> x.getTolkBehovSistOppdatert().equals("2022-02-22")).findFirst().isPresent());
    }

    @Test
    public void skal_hente_alle_brukere_fra_landgruppe() {
        var brukerFraLandGruppe1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setFoedeland("NOR")
                .setFoedelandFulltNavn("Norge")
                .setLandgruppe("1");

        var brukerFraLandGruppe2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setFoedeland("EST")
                .setFoedelandFulltNavn("Estland")
                .setLandgruppe("2")
                .setHovedStatsborgerskap(new Statsborgerskap("Estland", LocalDate.now(), null));
        ;

        var brukerFraLandGruppe3_1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setFoedeland("AZE")
                .setFoedelandFulltNavn("Aserbajdsjan")
                .setLandgruppe("3")
                .setHovedStatsborgerskap(new Statsborgerskap("Norge", LocalDate.now(), null));
        ;

        var brukerFraLandGruppe3_2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setFoedeland("SGP")
                .setFoedelandFulltNavn("Singapore")
                .setLandgruppe("3")
                .setHovedStatsborgerskap(new Statsborgerskap("Singapore", LocalDate.now(), null));

        var brukerUkjentLandGruppe = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setHovedStatsborgerskap(new Statsborgerskap("Norge", LocalDate.now(), null));
        ;

        var liste = List.of(brukerFraLandGruppe1, brukerFraLandGruppe2, brukerFraLandGruppe3_1, brukerFraLandGruppe3_2, brukerUkjentLandGruppe);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setLandgruppe(List.of("LANDGRUPPE_3"));

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "ikke_satt",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(2);
        assertThat(response.getBrukere().stream().filter(x -> x.getFoedeland().equals("Aserbajdsjan")).findFirst().isPresent());
        assertThat(response.getBrukere().stream().filter(x -> x.getFoedeland().equals("Singapore")).findFirst().isPresent());


        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setFoedeland(List.of("NOR"));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "ikke_satt",
                filterValg,
                null,
                null
        );
        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(response.getBrukere().stream().filter(x -> x.getFoedeland().equals("Norge")).findFirst().isPresent());


        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setLandgruppe(List.of("LANDGRUPPE_UKJENT"));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "ikke_satt",
                filterValg,
                null,
                null
        );
        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(response.getBrukere().stream().noneMatch(x -> x.getFoedeland() != null));
    }

    @Test
    public void test_sortering_landgruppe() {
        var brukerFraLandGruppe1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setFoedeland("NOR")
                .setFoedelandFulltNavn("Norge")
                .setLandgruppe("1");

        var brukerFraLandGruppe2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setFoedelandFulltNavn("Estland")
                .setLandgruppe("2")
                .setHovedStatsborgerskap(new Statsborgerskap("Estland", LocalDate.now(), null));
        ;

        var brukerFraLandGruppe3_1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setFoedelandFulltNavn("Aserbajdsjan")
                .setLandgruppe("3")
                .setHovedStatsborgerskap(new Statsborgerskap("Norge", LocalDate.now(), null));
        ;

        var brukerFraLandGruppe3_2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setFoedelandFulltNavn("Singapore")
                .setLandgruppe("3")
                .setHovedStatsborgerskap(new Statsborgerskap("Singapore", LocalDate.now(), null));

        var brukerUkjentLandGruppe = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setHovedStatsborgerskap(new Statsborgerskap("Norge", LocalDate.now(), null));

        var liste = List.of(brukerFraLandGruppe1, brukerFraLandGruppe2, brukerFraLandGruppe3_1, brukerFraLandGruppe3_2, brukerUkjentLandGruppe);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());


        Filtervalg filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of());

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "fodeland",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(5);
        assertThat(response.getBrukere().get(0).getFoedeland().equals("Aserbajdsjan"));
        assertThat(response.getBrukere().get(1).getFoedeland().equals("Estland"));
        assertThat(response.getBrukere().get(2).getFoedeland().equals("Norge"));
        assertThat(response.getBrukere().get(3).getFoedeland().equals("Singapore"));
        assertThat(response.getBrukere().get(4).getFoedeland() == null);


        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "statsborgerskap",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(5);
        assertThat(response.getBrukere().get(0).getHovedStatsborgerskap().getStatsborgerskap().equals("Estland"));
        assertThat(response.getBrukere().get(1).getHovedStatsborgerskap().getStatsborgerskap().equals("Norge"));
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
        opensearchIndexer.skrivTilIndeks(indexName.getValue(), List.of(brukere));
    }

}
