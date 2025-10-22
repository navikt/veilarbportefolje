package no.nav.pto.veilarbportefolje.opensearch;

import lombok.SneakyThrows;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriVerdi;
import no.nav.pto.veilarbportefolje.hendelsesfilter.Hendelse;
import no.nav.pto.veilarbportefolje.hendelsesfilter.Kategori;
import no.nav.pto.veilarbportefolje.opensearch.domene.OpensearchResponse;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.opensearch.domene.StatustallResponse;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.avvik14aVedtak.Avvik14aVedtak;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.GjeldendeVedtak14a;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.siste14aVedtak.Siste14aVedtakForBruker;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakstype;
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet;
import no.nav.pto.veilarbportefolje.util.BrukerComparator;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.*;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg.JA;
import static no.nav.pto.veilarbportefolje.hendelsesfilter.HendelsesfilterTestUtilKt.genererRandomHendelse;
import static no.nav.pto.veilarbportefolje.opensearch.BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ;
import static no.nav.pto.veilarbportefolje.opensearch.BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchQueryBuilder.byggArbeidslisteQuery;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static no.nav.pto.veilarbportefolje.util.OpensearchTestClient.pollOpensearchUntil;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class OpensearchServiceIntegrationTest extends EndToEndTest {
    private static String TEST_ENHET = randomNavKontor().getValue();
    private static final String TEST_VEILEDER_0 = randomVeilederId().getValue();
    private static final String TEST_VEILEDER_1 = randomVeilederId().getValue();
    private static final String TEST_VEILEDER_2 = randomVeilederId().getValue();
    private static final String TEST_VEILEDER_3 = randomVeilederId().getValue();
    private static final String LITE_PRIVILEGERT_VEILEDER = randomVeilederId().getValue();

    private final OpensearchService opensearchService;
    private final OpensearchIndexer opensearchIndexer;
    private final VeilarbVeilederClient veilarbVeilederClientMock;
    private final AuthContextHolder authContextHolder;

    @Autowired
    public OpensearchServiceIntegrationTest(
            OpensearchService opensearchService,
            OpensearchIndexer opensearchIndexer,
            VeilarbVeilederClient veilarbVeilederClientMock,
            AuthContextHolder authContextHolder
    ) {
        this.opensearchService = opensearchService;
        this.opensearchIndexer = opensearchIndexer;
        this.veilarbVeilederClientMock = veilarbVeilederClientMock;
        this.authContextHolder = authContextHolder;
    }

    @BeforeEach
    void byttenhet() {
        TEST_ENHET = randomNavKontor().getValue();
    }




    @Test
    public void skal_kun_hente_ut_brukere_under_oppfolging() {
        List<OppfolgingsBruker> brukere = List.of(
                new OppfolgingsBruker()
                        .setAktoer_id(randomAktorId().toString())
                        .setFnr(randomFnr().get())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET),

                new OppfolgingsBruker()
                        .setAktoer_id(randomAktorId().toString())
                        .setFnr(randomFnr().get())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET),

                // Markert som slettet
                new OppfolgingsBruker()
                        .setAktoer_id(randomAktorId().toString())
                        .setFnr(randomFnr().get())
                        .setOppfolging(false)
                        .setEnhet_id(TEST_ENHET)
        );

        skrivBrukereTilTestindeks(brukere);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == brukere.size());

        BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                new Filtervalg(),
                null,
                null
        );

        assertThat(brukereMedAntall.getAntall()).isEqualTo(2);
    }

    @Test
    public void skal_sette_brukere_med_veileder_fra_annen_enhet_til_ufordelt() {
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

        var filtervalg = new Filtervalg().setFerdigfilterListe(List.of(Brukerstatus.I_AVTALT_AKTIVITET));
        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == brukere.size());

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                filtervalg,
                null,
                null
        );


        assertThat(response.getAntall()).isEqualTo(2);

        Bruker ufordeltBruker = response.getBrukere().stream()
                .filter(b -> TEST_VEILEDER_1.equals(b.getVeilederId()))
                .toList().getFirst();

        assertThat(ufordeltBruker.isNyForEnhet()).isTrue();
    }

    @Test
    public void skal_hente_ut_brukere_ved_soek_paa_flere_veiledere() {
        String now = Instant.now().toString();
        List<OppfolgingsBruker> brukere = List.of(
                new OppfolgingsBruker()
                        .setAktoer_id(randomAktorId().get())
                        .setFnr(randomFnr().toString())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setNyesteutlopteaktivitet(now)
                        .setVeileder_id(TEST_VEILEDER_0),

                new OppfolgingsBruker()
                        .setAktoer_id(randomAktorId().get())
                        .setFnr(randomFnr().toString())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setNyesteutlopteaktivitet(now)
                        .setVeileder_id(TEST_VEILEDER_1),

                new OppfolgingsBruker()
                        .setAktoer_id(randomAktorId().get())
                        .setFnr(randomFnr().toString())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setNyesteutlopteaktivitet(now)
                        .setVeileder_id(null)

        );

        skrivBrukereTilTestindeks(brukere);

        var filtervalg = new Filtervalg()
                .setFerdigfilterListe(List.of(Brukerstatus.UTLOPTE_AKTIVITETER))
                .setVeiledere(List.of(TEST_VEILEDER_0, TEST_VEILEDER_1));


        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == brukere.size());

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                filtervalg,
                null,
                null);

        assertThat(response.getAntall()).isEqualTo(2);
    }

    @Test
    public void skal_hente_riktig_antall_ufordelte_brukere() {

        List<OppfolgingsBruker> brukere = List.of(
                new OppfolgingsBruker()
                        .setAktoer_id(randomAktorId().toString())
                        .setFnr(randomFnr().get())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setVeileder_id(null),

                new OppfolgingsBruker()
                        .setAktoer_id(randomAktorId().toString())
                        .setFnr(randomFnr().get())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setVeileder_id(TEST_VEILEDER_0),

                new OppfolgingsBruker()
                        .setAktoer_id(randomAktorId().toString())
                        .setFnr(randomFnr().get())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setVeileder_id(LITE_PRIVILEGERT_VEILEDER)
        );

        when(veilarbVeilederClientMock.hentVeilederePaaEnhet(any())).thenReturn(List.of(TEST_VEILEDER_0));

        skrivBrukereTilTestindeks(brukere);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == brukere.size());

        var filtervalg = new Filtervalg().setFerdigfilterListe(List.of(Brukerstatus.UFORDELTE_BRUKERE));
        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                filtervalg,
                null,
                null);
        assertThat(response.getAntall()).isEqualTo(2);
    }

    @Test
    public void skal_hente_riktige_antall_brukere_per_veileder() {

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
                                .setAktoer_id(randomAktorId().get())
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
                        .setAktoer_id(randomAktorId().get())
                        .setFnr(randomFnr().toString())
                        .setOppfolging(true)
                        .setVeileder_id(TEST_VEILEDER_0)
                        .setEnhet_id(TEST_ENHET)
                        .setArbeidsliste_aktiv(true);


        var brukerUtenArbeidsliste =
                new OppfolgingsBruker()
                        .setAktoer_id(randomAktorId().get())
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
    public void skal_hente_riktige_statustall_for_veileder_når_feature_toggle_er_av() {
        List<String> veilederePaEnhet = List.of(TEST_VEILEDER_0, TEST_VEILEDER_1, TEST_VEILEDER_2, TEST_VEILEDER_3);

        doReturn(veilederePaEnhet).when(veilarbVeilederClientMock).hentVeilederePaaEnhet(EnhetId.of(TEST_ENHET));
        doReturn(false).when(defaultUnleash).isEnabled(FeatureToggle.BRUK_FILTER_FOR_BRUKERINNSYN_TILGANGER);

        OppfolgingsBruker kode_6_bruker = genererRandomBruker(TEST_ENHET, null, "6", false);
        OppfolgingsBruker kode_6_bruker_med_tilordnet_veileder = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_0, "6", false);
        OppfolgingsBruker kode_6_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_1, "6", false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker kode_7_bruker = genererRandomBruker(TEST_ENHET, null, "7", false);
        OppfolgingsBruker kode_7_bruker_med_tilordnet_veileder = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_0, "7", false);
        OppfolgingsBruker kode_7_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_1, "7", false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker egen_ansatt_bruker = genererRandomBruker(TEST_ENHET, null, null, true);
        OppfolgingsBruker egen_ansatt_bruker_med_tilordnet_veileder = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_0, null, true);
        OppfolgingsBruker egen_ansatt_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_1, null, true).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker egen_ansatt_og_kode_7_bruker = genererRandomBruker(TEST_ENHET, null, "7", true);
        OppfolgingsBruker egen_ansatt_og_kode_7_bruker_med_tilordnet_veileder = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_0, "7", true);
        OppfolgingsBruker egen_ansatt_og_kode_7_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_1, "7", true).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker tilfeldig_fordelt_bruker = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_0, null, false);
        OppfolgingsBruker tilfeldig_ufordelt_bruker = genererRandomBruker(TEST_ENHET, null, null, false);
        OppfolgingsBruker tilfeldig_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_0, null, false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        List<OppfolgingsBruker> brukere = List.of(
                kode_6_bruker,
                kode_6_bruker_med_tilordnet_veileder,
                kode_6_bruker_som_venter_pa_svar_fra_nav,
                kode_7_bruker,
                kode_7_bruker_med_tilordnet_veileder,
                kode_7_bruker_som_venter_pa_svar_fra_nav,
                egen_ansatt_bruker,
                egen_ansatt_bruker_med_tilordnet_veileder,
                egen_ansatt_bruker_som_venter_pa_svar_fra_nav,
                egen_ansatt_og_kode_7_bruker,
                egen_ansatt_og_kode_7_bruker_med_tilordnet_veileder,
                egen_ansatt_og_kode_7_bruker_som_venter_pa_svar_fra_nav,
                tilfeldig_fordelt_bruker,
                tilfeldig_ufordelt_bruker,
                tilfeldig_bruker_som_venter_pa_svar_fra_nav
        );

        skrivBrukereTilTestindeks(brukere);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == brukere.size());

        Statustall responsNyttEndepunkt = authContextHolder.withContext(
                new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDER_0)),
                () -> opensearchService.hentStatustallForVeilederPortefolje(TEST_VEILEDER_0, TEST_ENHET)
        );

        assertThat(responsNyttEndepunkt.getTotalt()).isEqualTo(6);
        assertThat(responsNyttEndepunkt.getVenterPaSvarFraNAV()).isEqualTo(1);
    }

    @Test
    public void skal_hente_riktige_statustall_for_enhet_når_feature_toggle_er_av() {
        List<String> veilederePaEnhet = List.of(TEST_VEILEDER_0, TEST_VEILEDER_1, TEST_VEILEDER_2, TEST_VEILEDER_3);

        doReturn(veilederePaEnhet).when(veilarbVeilederClientMock).hentVeilederePaaEnhet(EnhetId.of(TEST_ENHET));
        doReturn(false).when(defaultUnleash).isEnabled(FeatureToggle.BRUK_FILTER_FOR_BRUKERINNSYN_TILGANGER);

        OppfolgingsBruker kode_6_bruker = genererRandomBruker(TEST_ENHET, null, "6", false);
        OppfolgingsBruker kode_6_bruker_med_tilordnet_veileder = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_0, "6", false);
        OppfolgingsBruker kode_6_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_1, "6", false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker kode_7_bruker = genererRandomBruker(TEST_ENHET, null, "7", false);
        OppfolgingsBruker kode_7_bruker_med_tilordnet_veileder = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_0, "7", false);
        OppfolgingsBruker kode_7_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_1, "7", false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker egen_ansatt_bruker = genererRandomBruker(TEST_ENHET, null, null, true);
        OppfolgingsBruker egen_ansatt_bruker_med_tilordnet_veileder = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_0, null, true);
        OppfolgingsBruker egen_ansatt_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_1, null, true).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker egen_ansatt_og_kode_7_bruker = genererRandomBruker(TEST_ENHET, null, "7", true);
        OppfolgingsBruker egen_ansatt_og_kode_7_bruker_med_tilordnet_veileder = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_0, "7", true);
        OppfolgingsBruker egen_ansatt_og_kode_7_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_1, "7", true).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker tilfeldig_fordelt_bruker = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_0, null, false);
        OppfolgingsBruker tilfeldig_ufordelt_bruker = genererRandomBruker(TEST_ENHET, null, null, false);
        OppfolgingsBruker tilfeldig_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_0, null, false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        List<OppfolgingsBruker> brukere = List.of(
                kode_6_bruker,
                kode_6_bruker_med_tilordnet_veileder,
                kode_6_bruker_som_venter_pa_svar_fra_nav,
                kode_7_bruker,
                kode_7_bruker_med_tilordnet_veileder,
                kode_7_bruker_som_venter_pa_svar_fra_nav,
                egen_ansatt_bruker,
                egen_ansatt_bruker_med_tilordnet_veileder,
                egen_ansatt_bruker_som_venter_pa_svar_fra_nav,
                egen_ansatt_og_kode_7_bruker,
                egen_ansatt_og_kode_7_bruker_med_tilordnet_veileder,
                egen_ansatt_og_kode_7_bruker_som_venter_pa_svar_fra_nav,
                tilfeldig_fordelt_bruker,
                tilfeldig_ufordelt_bruker,
                tilfeldig_bruker_som_venter_pa_svar_fra_nav
        );

        skrivBrukereTilTestindeks(brukere);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == brukere.size());

        Statustall responsMedBrukerinnsyn = authContextHolder.withContext(
                new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDER_0)),
                () -> opensearchService.hentStatusTallForEnhetPortefolje(TEST_ENHET, BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ)
        );
        Statustall responsUtenBrukerinnsyn = authContextHolder.withContext(
                new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDER_0)),
                () -> opensearchService.hentStatusTallForEnhetPortefolje(TEST_ENHET, BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ)
        );

        assertThat(responsMedBrukerinnsyn.getTotalt() + responsUtenBrukerinnsyn.getTotalt()).isEqualTo(15);
        assertThat(responsMedBrukerinnsyn.getVenterPaSvarFraNAV() + responsUtenBrukerinnsyn.getVenterPaSvarFraNAV()).isEqualTo(5);
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

        var statustallForBrukereSomVeilederHarInnsynsrettPå = opensearchService.hentStatusTallForEnhetPortefolje(TEST_ENHET, BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ);
        var statustallForBrukereSomVeilederIkkeHarInnsynsrettPå = opensearchService.hentStatusTallForEnhetPortefolje(TEST_ENHET, BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ);

        assertThat(statustallForBrukereSomVeilederHarInnsynsrettPå.getUfordelteBrukere()).isEqualTo(1);
        assertThat(statustallForBrukereSomVeilederIkkeHarInnsynsrettPå.getUfordelteBrukere()).isZero();
    }

    @Test
    void skal_mappe_statustall_for_samtlige_aggregation_keys() {
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

        Statustall veilederStatustall = opensearchService.hentStatustallForVeilederPortefolje(TEST_VEILEDER_0, TEST_ENHET);
        Statustall enhetStatustallForBrukereSomVeilederHarInnsynsrettPå = opensearchService.hentStatusTallForEnhetPortefolje(TEST_ENHET, BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ);
        Statustall enhetStatustallForBrukereSomVeilederIkkeHarInnsynsrettPå = opensearchService.hentStatusTallForEnhetPortefolje(TEST_ENHET, BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ);

        Arrays.stream(StatustallResponse.StatustallAggregationKey.values()).forEach(key -> {
            assertDoesNotThrow(() -> veilederStatustall.getClass().getDeclaredField(key.key));
            assertDoesNotThrow(() -> enhetStatustallForBrukereSomVeilederHarInnsynsrettPå.getClass().getDeclaredField(key.key));
            assertDoesNotThrow(() -> enhetStatustallForBrukereSomVeilederIkkeHarInnsynsrettPå.getClass().getDeclaredField(key.key));
        });
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
                Sorteringsrekkefolge.SYNKENDE,
                Sorteringsfelt.VALGTE_AKTIVITETER,
                filtervalg1,
                null,
                null
        );
        BrukereMedAntall brukereMedAntall2 = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                Sorteringsrekkefolge.SYNKENDE,
                Sorteringsfelt.VALGTE_AKTIVITETER,
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
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(LITE_PRIVILEGERT_VEILEDER)
                .setTrenger_vurdering(true);

        var ikkeNyForEnhet = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setTrenger_vurdering(true);


        var liste = List.of(nyForEnhet, ikkeNyForEnhet);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        List<Brukerstatus> ferdigFiltere = List.of(
                Brukerstatus.UFORDELTE_BRUKERE
        );

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
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
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        var brukerVeilederIkkeHarTilgangTil = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id("NEGA_$testEnhet")
                .setVeileder_id("NEGA_$testVeileder");

        var liste = List.of(brukerVeilederHarTilgangTil, brukerVeilederIkkeHarTilgangTil);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
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
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(LITE_PRIVILEGERT_VEILEDER);

        var brukerMedFordeltStatus = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
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
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                new Filtervalg().setFerdigfilterListe(List.of(Brukerstatus.UFORDELTE_BRUKERE)),
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(veilederExistsInResponse(LITE_PRIVILEGERT_VEILEDER, response)).isTrue();

        Statustall statustall = opensearchService.hentStatusTallForEnhetPortefolje(TEST_ENHET, BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ);
        assertThat(statustall.getUfordelteBrukere()).isEqualTo(1);
    }

    @Test
    void skal_returnere_brukere_basert_på_fødselsdag_i_måneden() {
        var testBrukerFodselsdagSyvende = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setFodselsdag_i_mnd(7)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        var testBrukerFodselsdagNiende = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setFodselsdag_i_mnd(9)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);


        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setFodselsdagIMnd(List.of("7"));

        var liste = List.of(testBrukerFodselsdagSyvende, testBrukerFodselsdagNiende);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null

        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertEquals(testBrukerFodselsdagSyvende.getFnr(), response.getBrukere().getFirst().getFnr());
    }

    @Test
    void skal_hente_ut_brukere_basert_på_kjønn() {
        var mann = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setKjonn("M");

        var kvinne = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
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
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertEquals(kvinne.getFnr(), response.getBrukere().getFirst().getFnr());
    }

    @Test
    void skal_hente_ut_brukere_som_går_på_arbeidsavklaringspenger_som_rettighetsgruppefilter() {
        var brukerMedAAP = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.AAP.name());

        var brukerUtenAAP = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
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
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(userExistsInResponse(brukerMedAAP, response)).isTrue();
        assertThat(userExistsInResponse(brukerUtenAAP, response)).isFalse();

    }

    @Test
    void skal_hente_ut_brukere_som_går_på_arbeidsavklaringspenger_behandlet_i_arena() {
        var brukerMedAAPUnntak = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setYtelse("AAP_UNNTAK");

        var brukerMedAAPOrdinær = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setYtelse("AAP_MAXTID");

        var brukerUtenAAP = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setYtelse("ORDINARE_DAGPENGER");


        var liste = List.of(brukerMedAAPOrdinær, brukerUtenAAP, brukerMedAAPUnntak);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setYtelseAapArena(List.of(YtelseAapArena.HAR_AAP_ORDINAR, YtelseAapArena.HAR_AAP_UNNTAK));

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(2);
        assertThat(userExistsInResponse(brukerMedAAPUnntak, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedAAPOrdinær, response)).isTrue();
        assertThat(userExistsInResponse(brukerUtenAAP, response)).isFalse();

    }

    @Test
    void antall_brukere_med_arbeidsavklaringspenger_skal_være_like_for_ytelsesfilter_og_aapArenafilter() {
        var brukerMedAAPUnntak = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setYtelse("AAP_UNNTAK");

        var brukerMedAAPOrdinær = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setYtelse("AAP_MAXTID");

        var brukerMedAAPUnntak2 = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setYtelse("AAP_UNNTAK");

        var brukerUtenAap = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setYtelse("TILTAKSPENGER");


        var liste = List.of(brukerMedAAPOrdinær, brukerMedAAPUnntak, brukerMedAAPUnntak2, brukerUtenAap);
        skrivBrukereTilTestindeks(liste);
        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValgAapArenaFilter = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setYtelseAapArena(List.of(YtelseAapArena.HAR_AAP_ORDINAR, YtelseAapArena.HAR_AAP_UNNTAK));

        var filterValgYtelseFilter = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setYtelse(YtelseFilterArena.AAP);

        var responseAapArenaFilter = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                filterValgAapArenaFilter,
                null,
                null
        );

        var responseYtelseFilter = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                filterValgYtelseFilter,
                null,
                null
        );
        assertThat(responseAapArenaFilter.getAntall()).isEqualTo(3);
        assertThat(responseYtelseFilter.getAntall()).isEqualTo(3);
        assertThat(userExistsInResponse(brukerMedAAPUnntak, responseAapArenaFilter)).isTrue();
        assertThat(userExistsInResponse(brukerMedAAPUnntak, responseYtelseFilter)).isTrue();
        assertThat(userExistsInResponse(brukerMedAAPOrdinær, responseAapArenaFilter)).isTrue();
        assertThat(userExistsInResponse(brukerMedAAPOrdinær, responseYtelseFilter)).isTrue();
        assertThat(userExistsInResponse(brukerUtenAap, responseAapArenaFilter)).isFalse();
        assertThat(userExistsInResponse(brukerUtenAap, responseYtelseFilter)).isFalse();
    }

    @Test
    void skal_hente_ut_brukere_som_går_på_arbeidsavklaringspenger_behandlet_i_kelvin() {
        var brukerMedAAP = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setAap_kelvin(true);

        var brukerUtenAAP = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setAap_kelvin(false);


        var liste = List.of(brukerMedAAP, brukerUtenAAP);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setYtelseAapKelvin(List.of(YtelseAapKelvin.HAR_AAP));

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(userExistsInResponse(brukerMedAAP, response)).isTrue();
        assertThat(userExistsInResponse(brukerUtenAAP, response)).isFalse();

    }

    @Test
    void skal_hente_ut_brukere_som_går_på_tiltakspenger_behandlet_i_tpsak() {
        var brukerMedTiltakspenger = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setTiltakspenger(true);

        var brukerUtenTiltakspenger = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setTiltakspenger(false);


        var liste = List.of(brukerMedTiltakspenger, brukerUtenTiltakspenger);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setYtelseTiltakspenger(List.of(YtelseTiltakspenger.HAR_TILTAKSPENGER));

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(userExistsInResponse(brukerMedTiltakspenger, response)).isTrue();
        assertThat(userExistsInResponse(brukerUtenTiltakspenger, response)).isFalse();

    }

    @Test
    void skal_hente_ut_brukere_som_går_på_tiltakspenger_behandlet_i_arena() {
        var brukerMedTiltakspenger = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setYtelse("TILTAKSPENGER");

        var brukerUtenTiltakspenger = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setYtelse("ORDINARE_DAGPENGER");


        var liste = List.of(brukerMedTiltakspenger, brukerUtenTiltakspenger);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setYtelseTiltakspengerArena(List.of(YtelseTiltakspengerArena.HAR_TILTAKSPENGER));

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(userExistsInResponse(brukerMedTiltakspenger, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedTiltakspenger, response)).isTrue();
        assertThat(userExistsInResponse(brukerUtenTiltakspenger, response)).isFalse();

    }

    @Test
    void antall_brukere_med_tiltakspenger_skal_være_like_for_ytelsesfilter_og_tiltakspengerArenafilter() {
        var brukerMedTiltakspenger = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setYtelse("TILTAKSPENGER");

        var brukerUtenTiltakspenger = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setYtelse("DAGPENGER");


        var liste = List.of(brukerMedTiltakspenger, brukerUtenTiltakspenger);
        skrivBrukereTilTestindeks(liste);
        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValgTpArenaFilter = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setYtelseTiltakspengerArena(List.of(YtelseTiltakspengerArena.HAR_TILTAKSPENGER));

        var filterValgYtelseFilter = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setYtelse(YtelseFilterArena.TILTAKSPENGER);

        var responseTpArenaFilter = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                filterValgTpArenaFilter,
                null,
                null
        );

        var responseYtelseFilter = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                filterValgYtelseFilter,
                null,
                null
        );
        assertThat(responseTpArenaFilter.getAntall()).isEqualTo(1);
        assertThat(responseYtelseFilter.getAntall()).isEqualTo(1);
        assertThat(userExistsInResponse(brukerMedTiltakspenger, responseTpArenaFilter)).isTrue();
        assertThat(userExistsInResponse(brukerMedTiltakspenger, responseYtelseFilter)).isTrue();
        assertThat(userExistsInResponse(brukerUtenTiltakspenger, responseTpArenaFilter)).isFalse();
        assertThat(userExistsInResponse(brukerUtenTiltakspenger, responseYtelseFilter)).isFalse();
    }


    @Test
    void skal_hente_ut_brukere_filtrert_på_dagpenger_som_ytelse() {

        var brukerMedDagpengerMedPermittering = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.AAP.name())
                .setYtelse(YtelseMapping.DAGPENGER_MED_PERMITTERING.name());


        var brukerMedPermitteringFiskeindustri = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.AAP.name())
                .setYtelse(YtelseMapping.DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI.name());

        var brukerMedAAP = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.DAGP.name())
                .setYtelse(YtelseMapping.AAP_MAXTID.name());

        var brukerMedAnnenVeileder = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
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
                .setYtelse(YtelseFilterArena.DAGPENGER);

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
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
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("sokeavtale"));

        var brukerMedBehandling = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("behandling"));

        var brukerMedUtenAktiviteter = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
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
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
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
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
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
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("tiltak"));

        var brukerMedBehandling = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(Set.of("behandling"));

        var brukerUtenAktiviteter = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
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
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
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
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
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
                .setFerdigfilterListe(List.of(Brukerstatus.UNDER_VURDERING));

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.UTKAST_14A_ANSVARLIG_VEILEDER,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(4);
        assertThat(userExistsInResponse(brukerMedVedtak, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedVedtak1, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedVedtak2, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedVedtakUtenAnsvarligVeileder, response)).isTrue();

        assertThat(response.getBrukere().get(0).getUtkast14a().ansvarligVeileder()).isEqualTo("AVeileder");
        assertThat(response.getBrukere().get(1).getUtkast14a().ansvarligVeileder()).isEqualTo("BVeileder");
        assertThat(response.getBrukere().get(2).getUtkast14a().ansvarligVeileder()).isEqualTo("CVeileder");
        assertThat(response.getBrukere().get(3).getUtkast14a().ansvarligVeileder()).isNull();
    }

    @Test
    public void skal_hente_alle_brukere_som_har_tolkbehov() {
        var tolkesprakJapansk = "JPN";
        var tolkesprakSvensk = "SWE";
        var tolkesprakNorsk = "NB";

        var trengerTalespraktolkBehovSistOppdatert = "2022-02-22";
        var trengerTalespraktolk = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_0)
                .setTalespraaktolk(tolkesprakJapansk)
                .setTolkBehovSistOppdatert(LocalDate.parse(trengerTalespraktolkBehovSistOppdatert));

        var trengerTaleOgTegnspraktolkBehovSistOppdatert = "2021-03-23";
        var trengerTaleOgTegnspraktolk = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_0)
                .setTalespraaktolk(tolkesprakSvensk)
                .setTegnspraaktolk(tolkesprakSvensk)
                .setTolkBehovSistOppdatert(LocalDate.parse(trengerTaleOgTegnspraktolkBehovSistOppdatert));

        var trengerTegnspraktolkBehovSistOppdatert = "2023-03-24";
        var trengerTegnspraktolk = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_0)
                .setTegnspraaktolk(tolkesprakNorsk)
                .setTolkBehovSistOppdatert(LocalDate.parse(trengerTegnspraktolkBehovSistOppdatert));

        var brukerUtenTolkebehov1 = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_0)
                .setTalespraaktolk(null)
                .setTegnspraaktolk(null);

        var brukerUtenTolkebehov2 = genererRandomBruker(TEST_ENHET, TEST_VEILEDER_0)
                .setTalespraaktolk("")
                .setTegnspraaktolk("");

        var liste = List.of(trengerTalespraktolk, trengerTaleOgTegnspraktolk, trengerTegnspraktolk, brukerUtenTolkebehov1, brukerUtenTolkebehov2);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());


        /* Skal hente alle med talespråktolk */
        var filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setTolkebehov(List.of("TALESPRAAKTOLK"));

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(2);
        assertTrue(response.getBrukere().stream().filter(x -> x.getTolkebehov().talespraaktolk().equals(tolkesprakJapansk)).anyMatch(x -> x.getTolkebehov().sistOppdatert().toString().equals(trengerTalespraktolkBehovSistOppdatert)));
        assertTrue(response.getBrukere().stream().filter(x -> x.getTolkebehov().talespraaktolk().equals(tolkesprakSvensk)).anyMatch(x -> x.getTolkebehov().sistOppdatert().toString().equals(trengerTaleOgTegnspraktolkBehovSistOppdatert)));


        /* Skal hente alle med tegnspråktolk */
        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setTolkebehov(List.of("TEGNSPRAAKTOLK"));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );
        assertThat(response.getAntall()).isEqualTo(2);
        assertTrue(response.getBrukere().stream().filter(x -> x.getTolkebehov().tegnspraaktolk().equals(tolkesprakSvensk)).anyMatch(x -> x.getTolkebehov().sistOppdatert().toString().equals(trengerTaleOgTegnspraktolkBehovSistOppdatert)));
        assertTrue(response.getBrukere().stream().filter(x -> x.getTolkebehov().tegnspraaktolk().equals(tolkesprakNorsk)).anyMatch(x -> x.getTolkebehov().sistOppdatert().toString().equals(trengerTegnspraktolkBehovSistOppdatert)));


        /* Skal hente alle med tegn- eller talespråktolk */
        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setTolkebehov(List.of("TEGNSPRAAKTOLK", "TALESPRAAKTOLK"));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(3);
        assertTrue(response.getBrukere().stream().anyMatch(x -> x.getTolkebehov().sistOppdatert().toString().equals(trengerTalespraktolkBehovSistOppdatert)));
        assertTrue(response.getBrukere().stream().anyMatch(x -> x.getTolkebehov().sistOppdatert().toString().equals(trengerTaleOgTegnspraktolkBehovSistOppdatert)));
        assertTrue(response.getBrukere().stream().anyMatch(x -> x.getTolkebehov().sistOppdatert().toString().equals(trengerTegnspraktolkBehovSistOppdatert)));


        /* Skal hente alle med japansk som tegn- eller talespråk */
        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setTolkebehov(List.of("TEGNSPRAAKTOLK", "TALESPRAAKTOLK"))
                .setTolkBehovSpraak(List.of(tolkesprakJapansk));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );
        assertThat(response.getAntall()).isEqualTo(1);
        assertTrue(response.getBrukere().stream().filter(x -> x.getTolkebehov().talespraaktolk().equals(tolkesprakJapansk)).anyMatch(x -> x.getTolkebehov().sistOppdatert().toString().equals(trengerTalespraktolkBehovSistOppdatert)));


        /* Skal hente alle med japansk som tegn- eller talespråk, også når ingen tolkebehov er valgt */
        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setTolkebehov(List.of())
                .setTolkBehovSpraak(List.of(tolkesprakJapansk));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );
        assertThat(response.getAntall()).isEqualTo(1);
        assertTrue(response.getBrukere().stream().filter(x -> x.getTolkebehov().talespraaktolk().equals(tolkesprakJapansk)).anyMatch(x -> x.getTolkebehov().sistOppdatert().toString().equals(trengerTalespraktolkBehovSistOppdatert)));
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

        var liste = List.of(brukerFraLandGruppe1, brukerFraLandGruppe2, brukerFraLandGruppe3_1, brukerFraLandGruppe3_2, brukerUkjentLandGruppe);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setLandgruppe(List.of("LANDGRUPPE_3"));

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(2);
        assertTrue(response.getBrukere().stream().anyMatch(x -> x.getFoedeland().equals("Aserbajdsjan")));
        assertTrue((response.getBrukere().stream().anyMatch(x -> x.getFoedeland().equals("Singapore"))));


        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setFoedeland(List.of("NOR"));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );
        assertThat(response.getAntall()).isEqualTo(1);
        assertTrue(response.getBrukere().stream().anyMatch(x -> x.getFoedeland().equals("Norge")));


        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setLandgruppe(List.of("LANDGRUPPE_UKJENT"));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );
        assertThat(response.getAntall()).isEqualTo(1);
        assertTrue(response.getBrukere().stream().noneMatch(x -> x.getFoedeland() != null));

        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setLandgruppe(List.of("LANDGRUPPE_UKJENT", "LANDGRUPPE_3"));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );
        assertThat(response.getAntall()).isEqualTo(3);
        assertTrue(response.getBrukere().stream().filter(x -> x.getFoedeland() != null).anyMatch(x -> x.getFoedeland().equals("Singapore")));
        assertTrue(response.getBrukere().stream().filter(x -> x.getFoedeland() != null).anyMatch(x -> x.getFoedeland().equals("Aserbajdsjan")));
        assertTrue(response.getBrukere().stream().anyMatch(x -> x.getFoedeland() == null));
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

        var brukerFraLandGruppe3_3 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setFoedelandFulltNavn("Botswana")
                .setLandgruppe("3")
                .setHovedStatsborgerskap(new Statsborgerskap("Botswana", LocalDate.now(), null));

        var brukerUkjentLandGruppe = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setHovedStatsborgerskap(new Statsborgerskap("Norge", LocalDate.now(), null));

        var liste = List.of(brukerFraLandGruppe1, brukerFraLandGruppe2, brukerFraLandGruppe3_1, brukerFraLandGruppe3_2, brukerFraLandGruppe3_3, brukerUkjentLandGruppe);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());


        Filtervalg filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of());

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.FODELAND,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(6);
        assertEquals("Aserbajdsjan", response.getBrukere().get(0).getFoedeland());
        assertEquals("Botswana", response.getBrukere().get(1).getFoedeland());
        assertEquals("Estland", response.getBrukere().get(2).getFoedeland());
        assertEquals("Norge", response.getBrukere().get(3).getFoedeland());
        assertEquals("Singapore", response.getBrukere().get(4).getFoedeland());
        assertNull(response.getBrukere().get(5).getFoedeland());

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.SYNKENDE,
                Sorteringsfelt.FODELAND,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(6);
        assertEquals("Singapore", response.getBrukere().get(0).getFoedeland());
        assertEquals("Norge", response.getBrukere().get(1).getFoedeland());
        assertEquals("Estland", response.getBrukere().get(2).getFoedeland());
        assertEquals("Botswana", response.getBrukere().get(3).getFoedeland());
        assertEquals("Aserbajdsjan", response.getBrukere().get(4).getFoedeland());
        assertNull(response.getBrukere().get(5).getFoedeland());

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.STATSBORGERSKAP,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(6);
        assertEquals("Botswana", response.getBrukere().get(0).getHovedStatsborgerskap().getStatsborgerskap());
        assertEquals("Estland", response.getBrukere().get(1).getHovedStatsborgerskap().getStatsborgerskap());
        assertEquals("Norge", response.getBrukere().get(2).getHovedStatsborgerskap().getStatsborgerskap());
    }

    @Test
    public void skal_hente_alle_brukere_med_bosted() {
        var bruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setKommunenummer("10")
                .setBydelsnummer("1222");

        var bruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setKommunenummer("12")
                .setBydelsnummer("1233");

        var bruker3 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setKommunenummer("12")
                .setBydelsnummer("1234");

        var bruker4 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setKommunenummer("10")
                .setBydelsnummer("1010");

        var brukerUkjentBosted = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET);

        var liste = List.of(bruker1, bruker2, bruker3, bruker4, brukerUkjentBosted);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setGeografiskBosted(List.of("10"));

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(2);
        assertTrue(response.getBrukere().stream().allMatch(x -> x.getBostedKommune().equals("10")));


        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setGeografiskBosted(List.of("1233"));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );
        assertThat(response.getAntall()).isEqualTo(1);
        assertTrue(response.getBrukere().stream().allMatch(x -> x.getBostedBydel().equals("1233")));


        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setGeografiskBosted(List.of("10", "1233"));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );
        assertThat(response.getAntall()).isEqualTo(3);
        assertThat(response.getBrukere().stream().filter(x -> x.getBostedKommune().equalsIgnoreCase("10")).count()).isEqualTo(2);
        assertTrue(response.getBrukere().stream().anyMatch(x -> x.getBostedBydel().equalsIgnoreCase("1233")));
    }

    @Test
    public void test_sortering_bostedkommune() {
        var bruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setKommunenummer("10");

        var bruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setKommunenummer("12")
                .setBydelsnummer("1233");

        var bruker3 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setKommunenummer("12")
                .setBydelsnummer("1234");

        var bruker4 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setKommunenummer("10")
                .setBydelsnummer("1010");

        var brukerUkjentBosted = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET);

        var liste = List.of(bruker1, bruker2, bruker3, bruker4, brukerUkjentBosted);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());


        Filtervalg filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of());

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.BOSTED_KOMMUNE,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(5);
        assertEquals("10", response.getBrukere().get(0).getBostedKommune());
        assertEquals("10", response.getBrukere().get(1).getBostedKommune());
        assertEquals("12", response.getBrukere().get(2).getBostedKommune());
        assertEquals("12", response.getBrukere().get(3).getBostedKommune());
        assertNull(response.getBrukere().get(4).getBostedKommune());

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.SYNKENDE,
                Sorteringsfelt.BOSTED_BYDEL,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(5);
        assertEquals("1234", response.getBrukere().get(0).getBostedBydel());
        assertEquals("1233", response.getBrukere().get(1).getBostedBydel());
        assertEquals("1010", response.getBrukere().get(2).getBostedBydel());
        assertNull(response.getBrukere().get(3).getBostedBydel());
        assertNull(response.getBrukere().get(4).getBostedBydel());

    }

    @Test
    public void skal_filtrere_brukere_med_riktige_avvikstyper_når_filter_for_avvik_er_valgt_hvor_noen_brukere_har_avvik_og_noen_ikke_har_avvik() {
        var bruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setAvvik14aVedtak(Avvik14aVedtak.INGEN_AVVIK);

        var bruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setAvvik14aVedtak(Avvik14aVedtak.INNSATSGRUPPE_MANGLER_I_NY_KILDE);

        var bruker3 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setAvvik14aVedtak(Avvik14aVedtak.INNSATSGRUPPE_ULIK);

        var bruker4 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setAvvik14aVedtak(Avvik14aVedtak.HOVEDMAAL_ULIK);

        var bruker5 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setAvvik14aVedtak(Avvik14aVedtak.INNSATSGRUPPE_OG_HOVEDMAAL_ULIK);

        var liste = List.of(bruker1, bruker2, bruker3, bruker4, bruker5);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setAvvik14aVedtak(List.of(Avvik14aVedtak.INNSATSGRUPPE_ULIK, Avvik14aVedtak.INNSATSGRUPPE_OG_HOVEDMAAL_ULIK));

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );

        assertThat(response.getBrukere())
                .hasSize(2)
                .extracting(Bruker::getAvvik14aVedtak)
                .containsExactlyInAnyOrder(Avvik14aVedtak.INNSATSGRUPPE_ULIK, Avvik14aVedtak.INNSATSGRUPPE_OG_HOVEDMAAL_ULIK);
    }

    @Test
    public void skal_filtrere_brukere_med_riktige_avvikstyper_når_filter_for_avvik_er_valgt_hvor_alle_brukere_ikke_har_avvik() {
        var bruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setAvvik14aVedtak(Avvik14aVedtak.INGEN_AVVIK);

        var bruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setAvvik14aVedtak(Avvik14aVedtak.INGEN_AVVIK);

        var bruker3 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setAvvik14aVedtak(Avvik14aVedtak.INGEN_AVVIK);

        var bruker4 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setAvvik14aVedtak(Avvik14aVedtak.INGEN_AVVIK);

        var bruker5 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setAvvik14aVedtak(Avvik14aVedtak.INGEN_AVVIK);

        var liste = List.of(bruker1, bruker2, bruker3, bruker4, bruker5);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setAvvik14aVedtak(List.of(Avvik14aVedtak.INGEN_AVVIK));

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );

        assertThat(response.getBrukere())
                .hasSize(5)
                .extracting(Bruker::getAvvik14aVedtak)
                .containsOnly(Avvik14aVedtak.INGEN_AVVIK);
    }

    @Test
    void skal_ikke_filtrere_på_avvikstype_når_filter_for_avvik_ikke_er_valgt() {
        var bruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setAvvik14aVedtak(Avvik14aVedtak.INGEN_AVVIK);

        var bruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setAvvik14aVedtak(Avvik14aVedtak.INNSATSGRUPPE_MANGLER_I_NY_KILDE);

        var bruker3 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setAvvik14aVedtak(Avvik14aVedtak.INNSATSGRUPPE_ULIK);

        var bruker4 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setAvvik14aVedtak(Avvik14aVedtak.HOVEDMAAL_ULIK);

        var bruker5 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setAvvik14aVedtak(Avvik14aVedtak.INNSATSGRUPPE_OG_HOVEDMAAL_ULIK);

        var liste = List.of(bruker1, bruker2, bruker3, bruker4, bruker5);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg();

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );

        assertThat(response.getBrukere()).hasSize(5);
    }

    @Test
    void skal_ikke_automatisk_sortere_nye_brukere_paa_top() {
        when(defaultUnleash.isEnabled(anyString())).thenReturn(true);
        var nyBrukerForVeileder = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(true)
                .setEtternavn("A");
        var brukerForVeileder1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEtternavn("B");
        var brukerForVeileder2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEtternavn("C");

        var liste = List.of(nyBrukerForVeileder, brukerForVeileder1, brukerForVeileder2);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg();

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                Sorteringsrekkefolge.SYNKENDE,
                Sorteringsfelt.ETTERNAVN,
                filterValg,
                null,
                null
        );

        assertThat(response.getBrukere()).hasSize(3);
        assertThat(response.getBrukere().get(0).getEtternavn()).isEqualTo("C");
        assertThat(response.getBrukere().get(2).getEtternavn()).isEqualTo("A");
    }

    @Test
    void skal_ikke_automatisk_sortere_ufordelte_brukere_paa_top() {
        when(defaultUnleash.isEnabled(anyString())).thenReturn(true);
        var ufordeltBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setVeileder_id(null)
                .setEtternavn("A");
        var bruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEtternavn("B");
        var bruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEtternavn("C");

        var liste = List.of(ufordeltBruker, bruker1, bruker2);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg();

        var response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.SYNKENDE,
                Sorteringsfelt.ETTERNAVN,
                filterValg,
                null,
                null
        );

        assertThat(response.getBrukere()).hasSize(3);
        assertThat(response.getBrukere().get(0).getEtternavn()).isEqualTo("C");
        assertThat(response.getBrukere().get(2).getEtternavn()).isEqualTo("A");
    }

    @Test
    public void test_sortering_enslige_forsorgere() {
        var bruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setEnslige_forsorgere_overgangsstonad(new EnsligeForsorgereOvergangsstonad("Hovedperiode",
                        true, LocalDate.now().plusMonths(4), LocalDate.now().minusMonths(2)));

        var bruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setEnslige_forsorgere_overgangsstonad(new EnsligeForsorgereOvergangsstonad("Forlengelse",
                        false, LocalDate.now().plusMonths(3), LocalDate.now().plusMonths(7)));

        var bruker3 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setEnslige_forsorgere_overgangsstonad(new EnsligeForsorgereOvergangsstonad("Utvidelse",
                        false, LocalDate.now().plusMonths(1), LocalDate.now().minusMonths(3)));

        var bruker4 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setEnslige_forsorgere_overgangsstonad(new EnsligeForsorgereOvergangsstonad("Periode før fødsel",
                        true, LocalDate.now().plusMonths(7), LocalDate.now().minusMonths(1)));

        var bruker5 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET);

        var liste = List.of(bruker1, bruker2, bruker3, bruker4, bruker5);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());


        Filtervalg filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of());

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.ENSLIGE_FORSORGERE_UTLOP_YTELSE,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(5);
        assertEquals(response.getBrukere().get(0).getFnr(), bruker5.getFnr());
        assertEquals(response.getBrukere().get(1).getFnr(), bruker3.getFnr());
        assertEquals(response.getBrukere().get(2).getFnr(), bruker2.getFnr());
        assertEquals(response.getBrukere().get(3).getFnr(), bruker1.getFnr());
        assertEquals(response.getBrukere().get(4).getFnr(), bruker4.getFnr());

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.ENSLIGE_FORSORGERE_OM_BARNET,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(5);
        assertEquals(response.getBrukere().get(0).getFnr(), bruker5.getFnr());
        assertEquals(response.getBrukere().get(1).getFnr(), bruker3.getFnr());
        assertEquals(response.getBrukere().get(2).getFnr(), bruker1.getFnr());
        assertEquals(response.getBrukere().get(3).getFnr(), bruker4.getFnr());
        assertEquals(response.getBrukere().get(4).getFnr(), bruker2.getFnr());

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.ENSLIGE_FORSORGERE_AKTIVITETSPLIKT,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(5);
        assertTrue(response.getBrukere().get(0).getFnr().equals(bruker2.getFnr()) || response.getBrukere().get(0).getFnr().equals(bruker3.getFnr()));
        assertTrue(response.getBrukere().get(1).getFnr().equals(bruker2.getFnr()) || response.getBrukere().get(1).getFnr().equals(bruker3.getFnr()));
        assertTrue(response.getBrukere().get(2).getFnr().equals(bruker1.getFnr()) || response.getBrukere().get(2).getFnr().equals(bruker4.getFnr()));
        assertTrue(response.getBrukere().get(3).getFnr().equals(bruker1.getFnr()) || response.getBrukere().get(3).getFnr().equals(bruker4.getFnr()));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.ENSLIGE_FORSORGERE_VEDTAKSPERIODETYPE,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(5);
        assertEquals(response.getBrukere().get(0).getFnr(), bruker2.getFnr());
        assertEquals(response.getBrukere().get(1).getFnr(), bruker1.getFnr());
        assertEquals(response.getBrukere().get(2).getFnr(), bruker4.getFnr());
        assertEquals(response.getBrukere().get(3).getFnr(), bruker3.getFnr());
    }


    @Test
    public void test_filtrering_og_statustall_tiltakshendelser() {
        OppfolgingsBruker bruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setTiltakshendelse(null);


        Fnr bruker2Fnr = Fnr.of("02020222222");
        UUID bruker2UUID = UUID.randomUUID();
        LocalDateTime bruker2Opprettet = LocalDateTime.now();
        String bruker2Tekst = "Forslag: Endre alt";
        String bruker2Lenke = "http.cat/200";
        Tiltakstype bruker2Tiltakstype = Tiltakstype.ARBFORB;

        OppfolgingsBruker bruker2 = new OppfolgingsBruker()
                .setFnr(bruker2Fnr.toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setTiltakshendelse(new Tiltakshendelse(bruker2UUID, bruker2Opprettet, bruker2Tekst, bruker2Lenke, bruker2Tiltakstype, bruker2Fnr));


        Fnr bruker3Fnr = Fnr.of("03030333333");
        UUID bruker3UUID = UUID.randomUUID();
        LocalDateTime bruker3Opprettet = LocalDateTime.now();
        String bruker3Tekst = "Forslag: Endre alt";
        String bruker3Lenke = "http.cat/200";
        Tiltakstype bruker3Tiltakstype = Tiltakstype.ARBFORB;

        OppfolgingsBruker bruker3 = new OppfolgingsBruker()
                .setFnr(bruker3Fnr.toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setTiltakshendelse(new Tiltakshendelse(bruker3UUID, bruker3Opprettet, bruker3Tekst, bruker3Lenke, bruker3Tiltakstype, bruker3Fnr));


        List<OppfolgingsBruker> brukere = List.of(bruker1, bruker2, bruker3);

        skrivBrukereTilTestindeks(brukere);
        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == brukere.size());

        Filtervalg filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of(Brukerstatus.TILTAKSHENDELSER));

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );
        List<Bruker> sorterteBrukere = response.getBrukere().stream().sorted(new BrukerComparator()).toList();

        assertThat(response.getAntall()).isEqualTo(2);
        assertThat(sorterteBrukere.get(0).getFnr()).isEqualTo(bruker2Fnr.toString());
        assertThat(sorterteBrukere.get(1).getFnr()).isEqualTo(bruker3Fnr.toString());

        var statustall = opensearchService.hentStatustallForVeilederPortefolje(TEST_VEILEDER_0, TEST_ENHET);
        assertThat(statustall.getTiltakshendelser()).isEqualTo(2);
    }

    @Test
    public void test_sortering_tiltakshendelser() {
        Fnr bruker1Fnr = Fnr.of("01010111111");
        Fnr bruker2Fnr = Fnr.of("02020222222");
        Fnr bruker3Fnr = Fnr.of("03030333333");
        LocalDateTime bruker1Opprettet = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime bruker2Opprettet = LocalDateTime.of(2023, 6, 1, 0, 0);
        LocalDateTime bruker3Opprettet = LocalDateTime.of(2022, 6, 1, 0, 0);
        String bruker1tekst = "Dette er noko tekst som startar på D.";
        String bruker2Tekst = "Akkurat slik startar du ein setning med bokstaven A.";
        String bruker3Tekst = "Byrjinga av denne teksten er bokstaven B.";
        String lenke = "http.cat/200";
        Tiltakstype tiltakstype = Tiltakstype.ARBFORB;

        OppfolgingsBruker bruker1 = new OppfolgingsBruker()
                .setFnr(bruker1Fnr.toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setTiltakshendelse(new Tiltakshendelse(UUID.randomUUID(), bruker1Opprettet, bruker1tekst, lenke, tiltakstype, bruker1Fnr));

        OppfolgingsBruker bruker2 = new OppfolgingsBruker()
                .setFnr(bruker2Fnr.toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setTiltakshendelse(new Tiltakshendelse(UUID.randomUUID(), bruker2Opprettet, bruker2Tekst, lenke, tiltakstype, bruker2Fnr));

        OppfolgingsBruker bruker3 = new OppfolgingsBruker()
                .setFnr(bruker3Fnr.toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setTiltakshendelse(new Tiltakshendelse(UUID.randomUUID(), bruker3Opprettet, bruker3Tekst, lenke, tiltakstype, bruker3Fnr));


        List<OppfolgingsBruker> brukere = List.of(bruker1, bruker2, bruker3);

        skrivBrukereTilTestindeks(brukere);
        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == brukere.size());

        Filtervalg filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of(Brukerstatus.TILTAKSHENDELSER));

        /* Om ein filtrerer på tiltakshendelse og ikkje har valgt sortering: sorter på opprettet-tidspunkt stigande. */
        BrukereMedAntall responseDefaultSortering = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );
        List<Bruker> brukereDefaultRekkefolge = responseDefaultSortering.getBrukere();

        assertThat(responseDefaultSortering.getAntall()).isEqualTo(3);
        assertThat(brukereDefaultRekkefolge.get(0).getFnr()).isEqualTo(bruker3Fnr.toString());
        assertThat(brukereDefaultRekkefolge.get(1).getFnr()).isEqualTo(bruker2Fnr.toString());
        assertThat(brukereDefaultRekkefolge.get(2).getFnr()).isEqualTo(bruker1Fnr.toString());

        BrukereMedAntall responseSortertNyesteDato = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.SYNKENDE,
                Sorteringsfelt.TILTAKSHENDELSE_DATO_OPPRETTET,
                filterValg,
                null,
                null
        );
        List<Bruker> brukereOpprettetSortertPaNyeste = responseSortertNyesteDato.getBrukere();

        assertThat(responseSortertNyesteDato.getAntall()).isEqualTo(3);
        assertThat(brukereOpprettetSortertPaNyeste.get(0).getFnr()).isEqualTo(bruker1Fnr.toString());
        assertThat(brukereOpprettetSortertPaNyeste.get(1).getFnr()).isEqualTo(bruker2Fnr.toString());
        assertThat(brukereOpprettetSortertPaNyeste.get(2).getFnr()).isEqualTo(bruker3Fnr.toString());

        BrukereMedAntall responseSortertAlfabetisk = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.TILTAKSHENDELSE_TEKST,
                filterValg,
                null,
                null
        );
        List<Bruker> brukereTekstSortertAlfabetisk = responseSortertAlfabetisk.getBrukere();

        assertThat(responseSortertAlfabetisk.getAntall()).isEqualTo(3);
        assertThat(brukereTekstSortertAlfabetisk.get(0).getFnr()).isEqualTo(bruker2Fnr.toString());
        assertThat(brukereTekstSortertAlfabetisk.get(1).getFnr()).isEqualTo(bruker3Fnr.toString());
        assertThat(brukereTekstSortertAlfabetisk.get(2).getFnr()).isEqualTo(bruker1Fnr.toString());
    }

    @Test
    public void test_filtrering_og_statustall_utgatte_varsel() {
        OppfolgingsBruker oppfolgingsBruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setUtgatt_varsel(null);


        Fnr oppfolgingsBruker2Fnr = Fnr.of("02020222222");
        Hendelse.HendelseInnhold utgattVarselBruker2 = genererRandomHendelse(Kategori.UTGATT_VARSEL).getHendelse();

        OppfolgingsBruker oppfolgingsBruker2 = new OppfolgingsBruker()
                .setFnr(oppfolgingsBruker2Fnr.toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setUtgatt_varsel(utgattVarselBruker2);


        Fnr oppfolgingsBruker3Fnr = Fnr.of("03030333333");
        Hendelse.HendelseInnhold utgattVarselBruker3 = genererRandomHendelse(Kategori.UTGATT_VARSEL).getHendelse();

        OppfolgingsBruker bruker3 = new OppfolgingsBruker()
                .setFnr(oppfolgingsBruker3Fnr.toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setUtgatt_varsel(utgattVarselBruker3);


        List<OppfolgingsBruker> brukere = List.of(oppfolgingsBruker1, oppfolgingsBruker2, bruker3);

        skrivBrukereTilTestindeks(brukere);
        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == brukere.size());

        Filtervalg filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of(Brukerstatus.UTGATTE_VARSEL));

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );
        List<Bruker> sorterteBrukere = response.getBrukere().stream().sorted(new BrukerComparator()).toList();

        assertThat(response.getAntall()).isEqualTo(2);
        assertThat(sorterteBrukere.get(0).getFnr()).isEqualTo(oppfolgingsBruker2Fnr.toString());
        assertThat(sorterteBrukere.get(1).getFnr()).isEqualTo(oppfolgingsBruker3Fnr.toString());

        var statustallForVeiledar = opensearchService.hentStatustallForVeilederPortefolje(TEST_VEILEDER_0, TEST_ENHET);
        assertThat(statustallForVeiledar.getUtgatteVarsel()).isEqualTo(2);

        var statustallForEnhet = opensearchService.hentStatusTallForEnhetPortefolje(TEST_ENHET, BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ);
        assertThat(statustallForEnhet.getUtgatteVarsel()).isEqualTo(2);
    }

    @Test
    void skal_sortere_pa_hendelsesdato_som_standard_ved_filtrering_pa_utgatt_varsel() {
        // Given
        AktorId aktoridBruker1 = AktorId.of("1111111111111");
        AktorId aktoridBruker2 = AktorId.of("2222222222222");
        AktorId aktoridBruker3 = AktorId.of("3333333333333");
        ZonedDateTime hendelsedatoBruker1 = ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime hendelsedatoBruker2 = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime hendelsedatoBruker3 = ZonedDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault());

        Hendelse.HendelseInnhold utgattVarselBruker1 = genererRandomHendelse(Kategori.UTGATT_VARSEL, hendelsedatoBruker1).getHendelse();
        Hendelse.HendelseInnhold utgattVarselBruker2 = genererRandomHendelse(Kategori.UTGATT_VARSEL, hendelsedatoBruker2).getHendelse();
        Hendelse.HendelseInnhold utgattVarselBruker3 = genererRandomHendelse(Kategori.UTGATT_VARSEL, hendelsedatoBruker3).getHendelse();

        OppfolgingsBruker bruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(aktoridBruker1.toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setUtgatt_varsel(utgattVarselBruker1);

        OppfolgingsBruker bruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(aktoridBruker2.toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setUtgatt_varsel(utgattVarselBruker2);

        OppfolgingsBruker bruker3 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(aktoridBruker3.toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setUtgatt_varsel(utgattVarselBruker3);

        List<OppfolgingsBruker> brukere = List.of(bruker1, bruker2, bruker3);

        skrivBrukereTilTestindeks(brukere);
        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == brukere.size());

        // When
        Filtervalg filtervalg = new Filtervalg()
                .setFerdigfilterListe(List.of(Brukerstatus.UTGATTE_VARSEL));

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                filtervalg,
                null,
                null
        );

        // Then
        assertThat(response.getAntall()).isEqualTo(3);
        assertEquals(response.getBrukere().get(0).getAktoerid(), bruker3.getAktoer_id());
        assertEquals(response.getBrukere().get(1).getAktoerid(), bruker1.getAktoer_id());
        assertEquals(response.getBrukere().get(2).getAktoerid(), bruker2.getAktoer_id());
    }

    @Test
    void skal_kunne_sortere_pa_hendelsesdato_pa_utgatt_varsel() {
        // Given
        AktorId aktoridBruker1 = AktorId.of("1111111111111");
        AktorId aktoridBruker2 = AktorId.of("2222222222222");
        AktorId aktoridBruker3 = AktorId.of("3333333333333");
        ZonedDateTime hendelsedatoBruker1 = ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime hendelsedatoBruker2 = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime hendelsedatoBruker3 = ZonedDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault());

        Hendelse.HendelseInnhold utgattVarselBruker1 = genererRandomHendelse(Kategori.UTGATT_VARSEL, hendelsedatoBruker1).getHendelse();
        Hendelse.HendelseInnhold utgattVarselBruker2 = genererRandomHendelse(Kategori.UTGATT_VARSEL, hendelsedatoBruker2).getHendelse();
        Hendelse.HendelseInnhold utgattVarselBruker3 = genererRandomHendelse(Kategori.UTGATT_VARSEL, hendelsedatoBruker3).getHendelse();

        OppfolgingsBruker bruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(aktoridBruker1.toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setUtgatt_varsel(utgattVarselBruker1);

        OppfolgingsBruker bruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(aktoridBruker2.toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setUtgatt_varsel(utgattVarselBruker2);

        OppfolgingsBruker bruker3 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(aktoridBruker3.toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setUtgatt_varsel(utgattVarselBruker3);

        List<OppfolgingsBruker> brukere = List.of(bruker1, bruker2, bruker3);

        skrivBrukereTilTestindeks(brukere);
        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == brukere.size());

        // When
        Filtervalg filtervalg = new Filtervalg()
                .setFerdigfilterListe(List.of(Brukerstatus.UTGATTE_VARSEL));

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.UTGATT_VARSEL_DATO,
                filtervalg,
                null,
                null
        );


        // Then
        assertThat(response.getAntall()).isEqualTo(3);
        assertEquals(bruker3.getAktoer_id(), response.getBrukere().get(0).getAktoerid());
        assertEquals(bruker1.getAktoer_id(), response.getBrukere().get(1).getAktoerid());
        assertEquals(bruker2.getAktoer_id(), response.getBrukere().get(2).getAktoerid());
    }


    @Test
    public void test_sortering_AAP() {
        var bruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setYtelse("AAP_UNNTAK")
                .setUtlopsdato("2023-06-30T21:59:59Z");

        var bruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setYtelse("AAP_MAXTID")
                .setAapmaxtiduke(43)
                .setAapordinerutlopsdato(DateUtils.toLocalDateOrNull("2023-04-20"));

        var bruker3 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setYtelse("AAP_UNNTAK")
                .setEnhet_id(TEST_ENHET);

        var bruker4 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setYtelse("AAP_MAXTID")
                .setEnhet_id(TEST_ENHET);

        var bruker5 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setYtelse("AAP_UNNTAK")
                .setUtlopsdato("2023-08-30T21:59:59Z");

        var bruker6 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setYtelse("AAP_MAXTID")
                .setAapmaxtiduke(12)
                .setAapordinerutlopsdato(DateUtils.toLocalDateOrNull("2023-04-12"));

        var liste = List.of(bruker1, bruker2, bruker3, bruker4, bruker5, bruker6);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());


        Filtervalg filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setYtelseAapArena(List.of(YtelseAapArena.HAR_AAP_ORDINAR));

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.AAP_VURDERINGSFRIST,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(3);
        assertThat(response.getBrukere().get(0).getFnr()).isEqualTo(bruker4.getFnr());
        assertThat(response.getBrukere().get(1).getFnr()).isEqualTo(bruker6.getFnr());
        assertThat(response.getBrukere().get(2).getFnr()).isEqualTo(bruker2.getFnr());

        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setYtelseAapArena(List.of(YtelseAapArena.HAR_AAP_UNNTAK));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.AAP_VURDERINGSFRIST,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(3);
        assertEquals(response.getBrukere().get(0).getFnr(), bruker3.getFnr());
        assertEquals(response.getBrukere().get(1).getFnr(), bruker1.getFnr());
        assertEquals(response.getBrukere().get(2).getFnr(), bruker5.getFnr());
    }

    @Test
    public void test_sortering_huskelapp() {
        var huskelapp1 = new HuskelappForBruker(LocalDate.now().plusDays(20), "dddd Ringe fastlege", LocalDate.now().minusDays(10), TEST_VEILEDER_0, UUID.randomUUID().toString(), TEST_ENHET);
        var huskelapp2 = new HuskelappForBruker(LocalDate.now().plusDays(30), "bbbb Ha et møte", LocalDate.now().minusDays(12), TEST_VEILEDER_0, UUID.randomUUID().toString(), TEST_ENHET);
        var huskelapp3 = new HuskelappForBruker(LocalDate.now().plusMonths(2), "aaaa Snakke om idrett", LocalDate.now().minusDays(8), TEST_VEILEDER_0, UUID.randomUUID().toString(), TEST_ENHET);
        var huskelapp4 = new HuskelappForBruker(LocalDate.now().plusDays(3), "cccc Huddle med Julie", LocalDate.now().minusDays(14), TEST_VEILEDER_0, UUID.randomUUID().toString(), TEST_ENHET);

        var bruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setHuskelapp(huskelapp1);

        var bruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setHuskelapp(huskelapp2);

        var bruker3 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setHuskelapp(huskelapp3);

        var bruker4 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setHuskelapp(huskelapp4);

        var bruker5 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);

        var bruker6 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);

        var liste = List.of(bruker1, bruker2, bruker3, bruker4, bruker5, bruker6);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());


        Filtervalg filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of());

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.HUSKELAPP_KOMMENTAR,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(6);
        assertEquals(response.getBrukere().get(0).getFnr(), bruker3.getFnr());
        assertEquals(response.getBrukere().get(1).getFnr(), bruker2.getFnr());
        assertEquals(response.getBrukere().get(2).getFnr(), bruker4.getFnr());
        assertEquals(response.getBrukere().get(3).getFnr(), bruker1.getFnr());


        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.SYNKENDE,
                Sorteringsfelt.HUSKELAPP_FRIST,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(6);
        assertEquals(response.getBrukere().get(0).getFnr(), bruker3.getFnr());
        assertEquals(response.getBrukere().get(1).getFnr(), bruker2.getFnr());
        assertEquals(response.getBrukere().get(2).getFnr(), bruker1.getFnr());
        assertEquals(response.getBrukere().get(3).getFnr(), bruker4.getFnr());

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.HUSKELAPP_FRIST,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(6);
        assertEquals(response.getBrukere().get(0).getFnr(), bruker4.getFnr());
        assertEquals(response.getBrukere().get(1).getFnr(), bruker1.getFnr());
        assertEquals(response.getBrukere().get(2).getFnr(), bruker2.getFnr());
        assertEquals(response.getBrukere().get(3).getFnr(), bruker3.getFnr());

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.HUSKELAPP_SIST_ENDRET,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(6);
        assertEquals(response.getBrukere().get(0).getFnr(), bruker4.getFnr());
        assertEquals(response.getBrukere().get(1).getFnr(), bruker2.getFnr());
        assertEquals(response.getBrukere().get(2).getFnr(), bruker1.getFnr());
        assertEquals(response.getBrukere().get(3).getFnr(), bruker3.getFnr());
    }

    @Test
    public void test_filtering_og_sortering_fargekategori() {

        var bruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setFargekategori(FargekategoriVerdi.FARGEKATEGORI_D.name())
                .setFargekategori_enhetId(TEST_ENHET);

        var bruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setFargekategori(FargekategoriVerdi.FARGEKATEGORI_A.name())
                .setFargekategori_enhetId(TEST_ENHET);

        var bruker3 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setFargekategori(FargekategoriVerdi.FARGEKATEGORI_A.name())
                .setFargekategori_enhetId(TEST_ENHET);

        var bruker4 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setFargekategori(FargekategoriVerdi.FARGEKATEGORI_B.name())
                .setFargekategori_enhetId(TEST_ENHET);

        var bruker5 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);

        var bruker6 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);

        var liste = List.of(bruker1, bruker2, bruker3, bruker4, bruker5, bruker6);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        Filtervalg filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setFargekategorier(List.of(FargekategoriVerdi.FARGEKATEGORI_B.name(), FargekategoriVerdi.FARGEKATEGORI_A.name()));

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(3);
        assertTrue(response.getBrukere().stream().map(Bruker::getFnr).toList().containsAll(List.of(bruker2.getFnr(), bruker3.getFnr(), bruker4.getFnr())));

        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setFargekategorier(List.of("INGEN_KATEGORI"));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(2);
        assertTrue(response.getBrukere().stream().map(Bruker::getFnr).toList().containsAll(List.of(bruker5.getFnr(), bruker6.getFnr())));


        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of());

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.FARGEKATEGORI,
                filterValg,
                null,
                null
        );

        List<String> equealSortOrder = List.of(bruker2.getFnr(), bruker3.getFnr());

        assertThat(response.getAntall()).isEqualTo(6);
        assertTrue(equealSortOrder.contains(response.getBrukere().get(0).getFnr()));
        assertTrue(equealSortOrder.contains(response.getBrukere().get(1).getFnr()));
        assertEquals(response.getBrukere().get(2).getFnr(), bruker4.getFnr());
        assertEquals(response.getBrukere().get(3).getFnr(), bruker1.getFnr());
    }

    @Test
    public void skal_hente_brukere_med_gjeldendeVedtak14a() {
        Fnr brukerMedSiste14aVedtakFnr = randomFnr();
        Fnr brukerUtenSiste14aVedtakFnr = randomFnr();
        AktorId brukerMedSiste14aVedtakAktorId = randomAktorId();
        AktorId brukerUtenSiste14aVedtakAktorId = randomAktorId();
        Innsatsgruppe innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS;
        Hovedmal hovedmal = Hovedmal.BEHOLDE_ARBEID;
        ZonedDateTime fattetDato = ZonedDateTime.now();
        boolean fraArena = false;
        Siste14aVedtakForBruker siste14aVedtakForBruker = new Siste14aVedtakForBruker(
                brukerMedSiste14aVedtakAktorId,
                innsatsgruppe,
                hovedmal,
                fattetDato,
                fraArena
        );
        skrivBrukereTilTestindeks(
                List.of(
                        new OppfolgingsBruker()
                                .setFnr(brukerMedSiste14aVedtakFnr.get())
                                .setAktoer_id(brukerMedSiste14aVedtakAktorId.get())
                                .setEnhet_id(TEST_ENHET)
                                .setOppfolging(true)
                                .setGjeldendeVedtak14a(new GjeldendeVedtak14a(
                                        siste14aVedtakForBruker.getInnsatsgruppe(),
                                        siste14aVedtakForBruker.getHovedmal(),
                                        siste14aVedtakForBruker.getFattetDato()
                                )),
                        new OppfolgingsBruker()
                                .setFnr(brukerUtenSiste14aVedtakFnr.get())
                                .setAktoer_id(brukerUtenSiste14aVedtakAktorId.get())
                                .setEnhet_id(TEST_ENHET)
                                .setOppfolging(true)
                )
        );
        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == 2);

        BrukereMedAntall respons = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                new Filtervalg().setFerdigfilterListe(emptyList()).setGjeldendeVedtak14a(List.of("HAR_14A_VEDTAK")),
                null,
                null
        );
        assertThat(respons.getAntall()).isEqualTo(1);
        Bruker brukerFraOpenSearch = respons.getBrukere().getFirst();
        assertThat(brukerFraOpenSearch.getFnr()).isEqualTo(brukerMedSiste14aVedtakFnr.get());
        assertThat(brukerFraOpenSearch.getAktoerid()).isEqualTo(brukerMedSiste14aVedtakAktorId.get());
        GjeldendeVedtak14a brukerFraOpenSearchGjeldendeVedtak14a = brukerFraOpenSearch.getGjeldendeVedtak14a();
        assertThat(brukerFraOpenSearchGjeldendeVedtak14a).isNotNull();
        assertThat(brukerFraOpenSearchGjeldendeVedtak14a.innsatsgruppe()).isEqualTo(innsatsgruppe);
        assertThat(brukerFraOpenSearchGjeldendeVedtak14a.hovedmal()).isEqualTo(hovedmal);
        assertThat(brukerFraOpenSearchGjeldendeVedtak14a.fattetDato()).isEqualTo(fattetDato.toOffsetDateTime().toZonedDateTime());
    }

    @Test
    public void skal_hente_brukere_uten_gjeldendeVedtak14a() {
        Fnr brukerMedSiste14aVedtakFnr = randomFnr();
        Fnr brukerUtenSiste14aVedtakFnr = randomFnr();
        AktorId brukerMedSiste14aVedtakAktorId = randomAktorId();
        AktorId brukerUtenSiste14aVedtakAktorId = randomAktorId();
        Innsatsgruppe innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS;
        Hovedmal hovedmal = Hovedmal.BEHOLDE_ARBEID;
        ZonedDateTime fattetDato = ZonedDateTime.now();
        boolean fraArena = false;
        Siste14aVedtakForBruker siste14aVedtakForBruker = new Siste14aVedtakForBruker(
                brukerMedSiste14aVedtakAktorId,
                innsatsgruppe,
                hovedmal,
                fattetDato,
                fraArena
        );
        skrivBrukereTilTestindeks(
                List.of(
                        new OppfolgingsBruker()
                                .setFnr(brukerMedSiste14aVedtakFnr.get())
                                .setAktoer_id(brukerMedSiste14aVedtakAktorId.get())
                                .setEnhet_id(TEST_ENHET)
                                .setOppfolging(true)
                                .setGjeldendeVedtak14a(new GjeldendeVedtak14a(
                                        siste14aVedtakForBruker.getInnsatsgruppe(),
                                        siste14aVedtakForBruker.getHovedmal(),
                                        siste14aVedtakForBruker.getFattetDato()
                                )),
                        new OppfolgingsBruker()
                                .setFnr(brukerUtenSiste14aVedtakFnr.get())
                                .setAktoer_id(brukerUtenSiste14aVedtakAktorId.get())
                                .setEnhet_id(TEST_ENHET)
                                .setOppfolging(true)
                )
        );
        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == 2);

        BrukereMedAntall respons = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                new Filtervalg().setFerdigfilterListe(emptyList()).setGjeldendeVedtak14a(List.of("HAR_IKKE_14A_VEDTAK")),
                null,
                null
        );
        assertThat(respons.getAntall()).isEqualTo(1);
        Bruker brukerFraOpenSearch = respons.getBrukere().getFirst();
        assertThat(brukerFraOpenSearch.getFnr()).isEqualTo(brukerUtenSiste14aVedtakFnr.get());
        assertThat(brukerFraOpenSearch.getAktoerid()).isEqualTo(brukerUtenSiste14aVedtakAktorId.get());
        GjeldendeVedtak14a brukerFraOpenSearchGjeldendeVedtak14a = brukerFraOpenSearch.getGjeldendeVedtak14a();
        assertThat(brukerFraOpenSearchGjeldendeVedtak14a).isNull();
    }

    @Test
    public void skal_hente_brukere_med_og_uten_gjeldendeVedtak14a() {
        Fnr brukerMedSiste14aVedtakFnr = randomFnr();
        Fnr brukerUtenSiste14aVedtakFnr = randomFnr();
        AktorId brukerMedSiste14aVedtakAktorId = randomAktorId();
        AktorId brukerUtenSiste14aVedtakAktorId = randomAktorId();
        Innsatsgruppe innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS;
        Hovedmal hovedmal = Hovedmal.BEHOLDE_ARBEID;
        ZonedDateTime fattetDato = ZonedDateTime.now();
        boolean fraArena = false;
        Siste14aVedtakForBruker siste14aVedtakForBruker = new Siste14aVedtakForBruker(
                brukerMedSiste14aVedtakAktorId,
                innsatsgruppe,
                hovedmal,
                fattetDato,
                fraArena
        );
        skrivBrukereTilTestindeks(
                List.of(
                        new OppfolgingsBruker()
                                .setFnr(brukerMedSiste14aVedtakFnr.get())
                                .setAktoer_id(brukerMedSiste14aVedtakAktorId.get())
                                .setEnhet_id(TEST_ENHET)
                                .setOppfolging(true)
                                .setGjeldendeVedtak14a(new GjeldendeVedtak14a(
                                        siste14aVedtakForBruker.getInnsatsgruppe(),
                                        siste14aVedtakForBruker.getHovedmal(),
                                        siste14aVedtakForBruker.getFattetDato()
                                )),
                        new OppfolgingsBruker()
                                .setFnr(brukerUtenSiste14aVedtakFnr.get())
                                .setAktoer_id(brukerUtenSiste14aVedtakAktorId.get())
                                .setEnhet_id(TEST_ENHET)
                                .setOppfolging(true)
                )
        );
        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == 2);

        BrukereMedAntall respons = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                new Filtervalg().setFerdigfilterListe(emptyList()).setGjeldendeVedtak14a(List.of("HAR_14A_VEDTAK", "HAR_IKKE_14A_VEDTAK")),
                null,
                null
        );
        assertThat(respons.getAntall()).isEqualTo(2);
    }

    @Test
    public void sorter_pa_vedtaksdato_som_standard_ved_filtrering_pa_alle_gjeldendeVedtak14a_filter() {
        AktorId aktoridBrukerMedGjeldendeVedtak14a1 = AktorId.of("4444444444444");
        AktorId aktoridBrukerMedGjeldendeVedtak14a2 = AktorId.of("3333333333333");
        AktorId aktoridBrukerMedGjeldendeVedtak14a3 = AktorId.of("2222222222222");
        AktorId aktoridBrukerUtenVedtak = AktorId.of("1111111111111");

        ZonedDateTime vedtaksdatoBruker1 = ZonedDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime vedtaksdatoBruker2 = ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime vedtaksdatoBruker3 = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault());

        OppfolgingsBruker bruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().get())
                .setAktoer_id(aktoridBrukerMedGjeldendeVedtak14a1.get())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setGjeldendeVedtak14a(new GjeldendeVedtak14a(
                        Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                        Hovedmal.OKE_DELTAKELSE,
                        vedtaksdatoBruker1
                ));

        OppfolgingsBruker bruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().get())
                .setAktoer_id(aktoridBrukerMedGjeldendeVedtak14a2.get())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setGjeldendeVedtak14a(new GjeldendeVedtak14a(
                        Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
                        Hovedmal.SKAFFE_ARBEID,
                        vedtaksdatoBruker2
                ));

        OppfolgingsBruker bruker3 = new OppfolgingsBruker()
                .setFnr(randomFnr().get())
                .setAktoer_id(aktoridBrukerMedGjeldendeVedtak14a3.get())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setGjeldendeVedtak14a(new GjeldendeVedtak14a(
                        Innsatsgruppe.STANDARD_INNSATS,
                        Hovedmal.BEHOLDE_ARBEID,
                        vedtaksdatoBruker3
                ));

        OppfolgingsBruker brukerUtenGjeldendeVedtak = new OppfolgingsBruker()
                .setFnr(randomFnr().get())
                .setAktoer_id(aktoridBrukerUtenVedtak.get())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true);

        var liste = List.of(bruker1, bruker2, bruker3, brukerUtenGjeldendeVedtak);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        Filtervalg filtrertHarGjeldendeVedtak = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setGjeldendeVedtak14a(List.of("HAR_14A_VEDTAK", "HAR_IKKE_14A_VEDTAK"));

        BrukereMedAntall responsFiltrertGjeldendeVedtak = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                filtrertHarGjeldendeVedtak,
                null,
                null
        );
        assertThat(responsFiltrertGjeldendeVedtak.getAntall()).isEqualTo(4);
        assertEquals(responsFiltrertGjeldendeVedtak.getBrukere().get(0).getAktoerid(), bruker1.getAktoer_id());
        assertEquals(responsFiltrertGjeldendeVedtak.getBrukere().get(1).getAktoerid(), bruker2.getAktoer_id());
        assertEquals(responsFiltrertGjeldendeVedtak.getBrukere().get(2).getAktoerid(), bruker3.getAktoer_id());
        assertEquals(responsFiltrertGjeldendeVedtak.getBrukere().get(3).getAktoerid(), brukerUtenGjeldendeVedtak.getAktoer_id());

        /* Nuller sorteringa ved å sortere på etternamn */
        sorterBrukerePaStandardsorteringenAktorid(opensearchService);

        Filtervalg filtrertInnsatsgruppe = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setInnsatsgruppeGjeldendeVedtak14a(List.of(Innsatsgruppe.STANDARD_INNSATS, Innsatsgruppe.VARIG_TILPASSET_INNSATS, Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS));

        BrukereMedAntall responsFiltrertInnsatsgruppe = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                filtrertInnsatsgruppe,
                null,
                null
        );
        assertThat(responsFiltrertInnsatsgruppe.getAntall()).isEqualTo(3);
        assertEquals(responsFiltrertGjeldendeVedtak.getBrukere().get(0).getAktoerid(), bruker1.getAktoer_id());
        assertEquals(responsFiltrertGjeldendeVedtak.getBrukere().get(1).getAktoerid(), bruker2.getAktoer_id());
        assertEquals(responsFiltrertGjeldendeVedtak.getBrukere().get(2).getAktoerid(), bruker3.getAktoer_id());

        /* Nuller sorteringa ved å sortere på etternamn */
        BrukereMedAntall responsNullstilling = sorterBrukerePaStandardsorteringenAktorid(opensearchService);
        assertEquals(responsNullstilling.getBrukere().get(0).getAktoerid(), brukerUtenGjeldendeVedtak.getAktoer_id());
        assertEquals(responsNullstilling.getBrukere().get(1).getAktoerid(), bruker3.getAktoer_id());
        assertEquals(responsNullstilling.getBrukere().get(2).getAktoerid(), bruker2.getAktoer_id());
        assertEquals(responsNullstilling.getBrukere().get(3).getAktoerid(), bruker1.getAktoer_id());

        Filtervalg filtrertHovedmal = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setHovedmalGjeldendeVedtak14a(List.of(Hovedmal.SKAFFE_ARBEID, Hovedmal.BEHOLDE_ARBEID, Hovedmal.OKE_DELTAKELSE));

        BrukereMedAntall responsFiltrertHovedmal = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                filtrertHovedmal,
                null,
                null
        );
        assertThat(responsFiltrertHovedmal.getAntall()).isEqualTo(3);
        assertEquals(responsFiltrertGjeldendeVedtak.getBrukere().get(0).getAktoerid(), bruker1.getAktoer_id());
        assertEquals(responsFiltrertGjeldendeVedtak.getBrukere().get(1).getAktoerid(), bruker2.getAktoer_id());
        assertEquals(responsFiltrertGjeldendeVedtak.getBrukere().get(2).getAktoerid(), bruker3.getAktoer_id());
    }

    @Test
    public void skal_kunne_sortere_brukere_med_og_uten_gjeldendeVedtak14a_pa_14a_kolonner() {
        Fnr brukerMedSiste14aVedtakFnr1 = Fnr.of("11111111111");
        Fnr brukerMedSiste14aVedtakFnr2 = Fnr.of("22222222222");
        Fnr brukerMedSiste14aVedtakFnr3 = Fnr.of("33333333333");
        Fnr brukerUtenSiste14aVedtakFnr = Fnr.of("44444444444");
        Innsatsgruppe innsatsgruppeBruker1 = Innsatsgruppe.VARIG_TILPASSET_INNSATS;
        Innsatsgruppe innsatsgruppeBruker2 = Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS;
        Innsatsgruppe innsatsgruppeBruker3 = Innsatsgruppe.STANDARD_INNSATS;
        Hovedmal hovedmalBruker1 = Hovedmal.OKE_DELTAKELSE;
        Hovedmal hovedmalBruker2 = Hovedmal.SKAFFE_ARBEID;
        Hovedmal hovedmalBruker3 = Hovedmal.BEHOLDE_ARBEID;
        ZonedDateTime vedtaksdatoBruker1 = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime vedtaksdatoBruker2 = ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime vedtaksdatoBruker3 = ZonedDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault());

        OppfolgingsBruker bruker1 = new OppfolgingsBruker()
                .setFnr(brukerMedSiste14aVedtakFnr1.get())
                .setAktoer_id(randomAktorId().get())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setGjeldendeVedtak14a(new GjeldendeVedtak14a(
                        innsatsgruppeBruker1,
                        hovedmalBruker1,
                        vedtaksdatoBruker1
                ));

        OppfolgingsBruker bruker2 = new OppfolgingsBruker()
                .setFnr(brukerMedSiste14aVedtakFnr2.get())
                .setAktoer_id(randomAktorId().get())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setGjeldendeVedtak14a(new GjeldendeVedtak14a(
                        innsatsgruppeBruker2,
                        hovedmalBruker2,
                        vedtaksdatoBruker2
                ));

        OppfolgingsBruker bruker3 = new OppfolgingsBruker()
                .setFnr(brukerMedSiste14aVedtakFnr3.get())
                .setAktoer_id(randomAktorId().get())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setGjeldendeVedtak14a(new GjeldendeVedtak14a(
                        innsatsgruppeBruker3,
                        hovedmalBruker3,
                        vedtaksdatoBruker3
                ));

        OppfolgingsBruker brukerUtenGjeldendeVedtak = new OppfolgingsBruker()
                .setFnr(brukerUtenSiste14aVedtakFnr.get())
                .setAktoer_id(randomAktorId().get())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true);

        var liste = List.of(bruker1, bruker2, bruker3, brukerUtenGjeldendeVedtak);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        Filtervalg filtervalg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setGjeldendeVedtak14a(List.of("HAR_14A_VEDTAK", "HAR_IKKE_14A_VEDTAK"));

        /* Innsatsgruppe, stigande. Forventa rekkefølgje: 2, 3, 1, Uten */
        BrukereMedAntall responsInnsatsgruppeStigende = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.GJELDENDE_VEDTAK_14A_INNSATSGRUPPE,
                filtervalg,
                null,
                null
        );
        assertThat(responsInnsatsgruppeStigende.getAntall()).isEqualTo(4);
        assertEquals(responsInnsatsgruppeStigende.getBrukere().get(0).getFnr(), bruker2.getFnr());
        assertEquals(responsInnsatsgruppeStigende.getBrukere().get(1).getFnr(), bruker3.getFnr());
        assertEquals(responsInnsatsgruppeStigende.getBrukere().get(2).getFnr(), bruker1.getFnr());
        assertEquals(responsInnsatsgruppeStigende.getBrukere().get(3).getFnr(), brukerUtenGjeldendeVedtak.getFnr());

        /* Innsatsgruppe, synkande. Forventa rekkefølgje: 1, 3, 2, Uten */
        BrukereMedAntall responsInnsatsgruppeSynkende = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.SYNKENDE,
                Sorteringsfelt.GJELDENDE_VEDTAK_14A_INNSATSGRUPPE,
                filtervalg,
                null,
                null
        );
        assertThat(responsInnsatsgruppeSynkende.getAntall()).isEqualTo(4);
        assertEquals(responsInnsatsgruppeSynkende.getBrukere().get(0).getFnr(), bruker1.getFnr());
        assertEquals(responsInnsatsgruppeSynkende.getBrukere().get(1).getFnr(), bruker3.getFnr());
        assertEquals(responsInnsatsgruppeSynkende.getBrukere().get(2).getFnr(), bruker2.getFnr());
        assertEquals(responsInnsatsgruppeSynkende.getBrukere().get(3).getFnr(), brukerUtenGjeldendeVedtak.getFnr());

        /* Hovedmål, stigande. Forventa: 3, 1, 2, Uten */
        BrukereMedAntall responsHovedmalStigende = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.GJELDENDE_VEDTAK_14A_HOVEDMAL,
                filtervalg,
                null,
                null
        );
        assertThat(responsHovedmalStigende.getAntall()).isEqualTo(4);
        assertEquals(responsHovedmalStigende.getBrukere().get(0).getFnr(), bruker3.getFnr());
        assertEquals(responsHovedmalStigende.getBrukere().get(1).getFnr(), bruker1.getFnr());
        assertEquals(responsHovedmalStigende.getBrukere().get(2).getFnr(), bruker2.getFnr());
        assertEquals(responsHovedmalStigende.getBrukere().get(3).getFnr(), brukerUtenGjeldendeVedtak.getFnr());

        /* Vedtaksdato, stigande. Forventa rekkefølgje: 3, 2, 1, Uten */
        BrukereMedAntall responsVedtaksdatoStigende = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.GJELDENDE_VEDTAK_14A_VEDTAKSDATO,
                filtervalg,
                null,
                null
        );
        assertThat(responsVedtaksdatoStigende.getAntall()).isEqualTo(4);
        assertEquals(responsVedtaksdatoStigende.getBrukere().get(0).getFnr(), bruker3.getFnr());
        assertEquals(responsVedtaksdatoStigende.getBrukere().get(1).getFnr(), bruker2.getFnr());
        assertEquals(responsVedtaksdatoStigende.getBrukere().get(2).getFnr(), bruker1.getFnr());
        assertEquals(responsVedtaksdatoStigende.getBrukere().get(3).getFnr(), brukerUtenGjeldendeVedtak.getFnr());
    }

    @Test
    public void skal_hente_brukere_med_innsatsgruppeGjeldendeVedtak14a() {
        Fnr brukerMedSiste14aVedtakFnr1 = Fnr.of("11111111111");
        Fnr brukerMedSiste14aVedtakFnr2 = Fnr.of("22222222222");
        Fnr brukerMedSiste14aVedtakFnr3 = Fnr.of("33333333333");
        Fnr brukerUtenSiste14aVedtakFnr = Fnr.of("44444444444");

        OppfolgingsBruker bruker1 = new OppfolgingsBruker()
                .setFnr(brukerMedSiste14aVedtakFnr1.get())
                .setAktoer_id(randomAktorId().get())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setGjeldendeVedtak14a(new GjeldendeVedtak14a(
                        Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                        Hovedmal.OKE_DELTAKELSE,
                        ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
                ));

        OppfolgingsBruker bruker2 = new OppfolgingsBruker()
                .setFnr(brukerMedSiste14aVedtakFnr2.get())
                .setAktoer_id(randomAktorId().get())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setGjeldendeVedtak14a(new GjeldendeVedtak14a(
                        Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
                        Hovedmal.SKAFFE_ARBEID,
                        ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
                ));

        OppfolgingsBruker bruker3 = new OppfolgingsBruker()
                .setFnr(brukerMedSiste14aVedtakFnr3.get())
                .setAktoer_id(randomAktorId().get())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setGjeldendeVedtak14a(new GjeldendeVedtak14a(
                        Innsatsgruppe.STANDARD_INNSATS,
                        Hovedmal.BEHOLDE_ARBEID,
                        ZonedDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
                ));

        OppfolgingsBruker brukerUtenGjeldendeVedtak = new OppfolgingsBruker()
                .setFnr(brukerUtenSiste14aVedtakFnr.get())
                .setAktoer_id(randomAktorId().get())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true);

        var liste = List.of(bruker1, bruker2, bruker3, brukerUtenGjeldendeVedtak);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        Filtervalg filtervalg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setInnsatsgruppeGjeldendeVedtak14a(List.of(Innsatsgruppe.VARIG_TILPASSET_INNSATS, Innsatsgruppe.STANDARD_INNSATS));

        BrukereMedAntall respons = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.GJELDENDE_VEDTAK_14A_INNSATSGRUPPE,
                filtervalg,
                null,
                null
        );

        assertThat(respons.getAntall()).isEqualTo(2);
        Bruker brukerFraOpenSearch = respons.getBrukere().get(0);
        assertThat(brukerFraOpenSearch.getFnr()).isEqualTo(brukerMedSiste14aVedtakFnr3.get());
        Bruker brukerFraOpenSearch1 = respons.getBrukere().get(1);
        assertThat(brukerFraOpenSearch1.getFnr()).isEqualTo(brukerMedSiste14aVedtakFnr1.get());
    }

    @Test
    public void skal_hente_brukere_med_hovedmalGjeldendeVedtak14a() {
        Fnr brukerMedSiste14aVedtakFnr1 = Fnr.of("11111111111");
        Fnr brukerMedSiste14aVedtakFnr2 = Fnr.of("22222222222");
        Fnr brukerMedSiste14aVedtakFnr3 = Fnr.of("33333333333");
        Fnr brukerUtenSiste14aVedtakFnr = Fnr.of("44444444444");

        OppfolgingsBruker bruker1 = new OppfolgingsBruker()
                .setFnr(brukerMedSiste14aVedtakFnr1.get())
                .setAktoer_id(randomAktorId().get())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setGjeldendeVedtak14a(new GjeldendeVedtak14a(
                        Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
                        Hovedmal.OKE_DELTAKELSE,
                        ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
                ));

        OppfolgingsBruker bruker2 = new OppfolgingsBruker()
                .setFnr(brukerMedSiste14aVedtakFnr2.get())
                .setAktoer_id(randomAktorId().get())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setGjeldendeVedtak14a(new GjeldendeVedtak14a(
                        Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
                        Hovedmal.SKAFFE_ARBEID,
                        ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
                ));

        OppfolgingsBruker bruker3 = new OppfolgingsBruker()
                .setFnr(brukerMedSiste14aVedtakFnr3.get())
                .setAktoer_id(randomAktorId().get())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setGjeldendeVedtak14a(new GjeldendeVedtak14a(
                        Innsatsgruppe.STANDARD_INNSATS,
                        Hovedmal.BEHOLDE_ARBEID,
                        ZonedDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
                ));

        OppfolgingsBruker brukerUtenGjeldendeVedtak = new OppfolgingsBruker()
                .setFnr(brukerUtenSiste14aVedtakFnr.get())
                .setAktoer_id(randomAktorId().get())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true);

        var liste = List.of(bruker1, bruker2, bruker3, brukerUtenGjeldendeVedtak);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        Filtervalg filtervalg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setHovedmalGjeldendeVedtak14a(List.of(Hovedmal.SKAFFE_ARBEID, Hovedmal.OKE_DELTAKELSE));

        BrukereMedAntall respons = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.GJELDENDE_VEDTAK_14A_HOVEDMAL,
                filtervalg,
                null,
                null
        );

        assertThat(respons.getAntall()).isEqualTo(2);
        Bruker brukerFraOpenSearch = respons.getBrukere().get(0);
        assertThat(brukerFraOpenSearch.getFnr()).isEqualTo(brukerMedSiste14aVedtakFnr1.get());
        Bruker brukerFraOpenSearch1 = respons.getBrukere().get(1);
        assertThat(brukerFraOpenSearch1.getFnr()).isEqualTo(brukerMedSiste14aVedtakFnr2.get());
    }

    @Test
    public void skal_sortere_pa_aktorid_som_standard_om_ikke_sorteringsfelt_er_valgt() {
        Fnr fnrBruker1 = Fnr.of("11111111111");
        Fnr fnrBruker2 = Fnr.of("22222222222");
        Fnr fnrBruker3 = Fnr.of("33333333333");
        AktorId aktoridBruker1 = AktorId.of("3333333333333");
        AktorId aktoridBruker2 = AktorId.of("1111111111111");
        AktorId aktoridBruker3 = AktorId.of("2222222222222");
        ZonedDateTime datoBruker1 = ZonedDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime datoBruker2 = ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime datoBruker3 = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault());

        OppfolgingsBruker bruker1 = new OppfolgingsBruker()
                .setFnr(fnrBruker1.get())
                .setAktoer_id(aktoridBruker1.get())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setTiltakshendelse(new Tiltakshendelse(UUID.randomUUID(), datoBruker1.toLocalDateTime(), "", "", Tiltakstype.ARBFORB, fnrBruker1))
                .setGjeldendeVedtak14a(new GjeldendeVedtak14a(Innsatsgruppe.STANDARD_INNSATS, Hovedmal.SKAFFE_ARBEID, datoBruker1));

        OppfolgingsBruker bruker2 = new OppfolgingsBruker()
                .setFnr(fnrBruker2.get())
                .setAktoer_id(aktoridBruker2.get())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setTiltakshendelse(new Tiltakshendelse(UUID.randomUUID(), datoBruker2.toLocalDateTime(), "", "", Tiltakstype.ARBFORB, fnrBruker2))
                .setGjeldendeVedtak14a(new GjeldendeVedtak14a(Innsatsgruppe.STANDARD_INNSATS, Hovedmal.SKAFFE_ARBEID, datoBruker2));

        OppfolgingsBruker bruker3 = new OppfolgingsBruker()
                .setFnr(fnrBruker3.get())
                .setAktoer_id(aktoridBruker3.get())
                .setEnhet_id(TEST_ENHET)
                .setOppfolging(true)
                .setTiltakshendelse(new Tiltakshendelse(UUID.randomUUID(), datoBruker3.toLocalDateTime(), "", "", Tiltakstype.ARBFORB, fnrBruker3))
                .setGjeldendeVedtak14a(new GjeldendeVedtak14a(Innsatsgruppe.STANDARD_INNSATS, Hovedmal.SKAFFE_ARBEID, datoBruker1));

        var liste = List.of(bruker1, bruker2, bruker3);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        BrukereMedAntall respons = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                new Filtervalg().setFerdigfilterListe(emptyList()),
                null,
                null
        );
        assertThat(respons.getAntall()).isEqualTo(3);
        assertEquals(respons.getBrukere().get(0).getAktoerid(), bruker2.getAktoer_id());
        assertEquals(respons.getBrukere().get(1).getAktoerid(), bruker3.getAktoer_id());
        assertEquals(respons.getBrukere().get(2).getAktoerid(), bruker1.getAktoer_id());
    }

    @Test
    public void skal_kunne_sortere_pa_alle_gyldige_sorteringsverdier() {
        Sorteringsfelt[] alleSorteringsfelt = Sorteringsfelt.values();
        ArrayList<Sorteringsfelt> sorteringsfeltSomFeilerISortering = new ArrayList<>();

        for (Sorteringsfelt sorteringsfelt : alleSorteringsfelt) {
            try {
                opensearchService.hentBrukere(
                        TEST_ENHET,
                        empty(),
                        Sorteringsrekkefolge.STIGENDE,
                        sorteringsfelt,
                        new Filtervalg().setFerdigfilterListe(emptyList()),
                        null,
                        null
                );
            } catch (Exception e) {
                sorteringsfeltSomFeilerISortering.add(sorteringsfelt);
            }
        }

        // Viser at vi får feil slik kodebasen er no. Målet er at sorteringsfeltSomFeilerISortering skal vere tom.
        assertThat(sorteringsfeltSomFeilerISortering).isNotEmpty();
    }

    @Test
    void skal_sortere_brukere_pa_aap_tom_vedtaksdato() {
        LocalDate tidspunkt1 = LocalDate.now();
        LocalDate tidspunkt2 = LocalDate.now().plusDays(2);
        LocalDate tidspunkt3 = LocalDate.now().plusDays(3);

        var tidligstTomBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setAap_kelvin(true)
                .setAap_kelvin_tom_vedtaksdato(tidspunkt1);

        var midtImellomBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setAap_kelvin(true)
                .setAap_kelvin_tom_vedtaksdato(tidspunkt2);

        var senestTomBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setAap_kelvin(true)
                .setAap_kelvin_tom_vedtaksdato(tidspunkt3);

        var nullBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setAap_kelvin(false);


        var liste = List.of(midtImellomBruker, senestTomBruker, tidligstTomBruker, nullBruker);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        Filtervalg filtervalg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setYtelseAapKelvin(List.of(YtelseAapKelvin.HAR_AAP, YtelseAapKelvin.HAR_IKKE_AAP));

        BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.AAP_KELVIN_TOM_VEDTAKSDATO,
                filtervalg,
                null,
                null
        );
        BrukereMedAntall brukereMedAntall2 = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                Sorteringsrekkefolge.SYNKENDE,
                Sorteringsfelt.AAP_KELVIN_TOM_VEDTAKSDATO,
                filtervalg,
                null,
                null
        );

        List<Bruker> brukereStigende = brukereMedAntall.getBrukere();
        List<Bruker> brukereSynkende = brukereMedAntall2.getBrukere();

        assertThat(brukereStigende.size()).isEqualTo(4);
        assertThat(brukereStigende.get(0).getFnr()).isEqualTo(tidligstTomBruker.getFnr());
        assertThat(brukereStigende.get(3).getFnr()).isEqualTo(nullBruker.getFnr());

        assertThat(brukereSynkende.get(0).getFnr()).isEqualTo(nullBruker.getFnr());
        assertThat(brukereSynkende.get(1).getFnr()).isEqualTo(senestTomBruker.getFnr());
        assertThat(brukereSynkende.get(3).getFnr()).isEqualTo(tidligstTomBruker.getFnr());
    }


    @Test
    void skal_sortere_brukere_pa_tiltakspenger_tom_vedtaksdato() {
        LocalDate tidspunkt1 = LocalDate.now();
        LocalDate tidspunkt2 = LocalDate.now().plusDays(2);
        LocalDate tidspunkt3 = LocalDate.now().plusDays(3);

        var tidligstTomBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setTiltakspenger(true)
                .setTiltakspenger_vedtaksdato_tom(tidspunkt1);

        var midtImellomBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setTiltakspenger(true)
                .setTiltakspenger_vedtaksdato_tom(tidspunkt2);

        var senestTomBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setTiltakspenger(true)
                .setTiltakspenger_vedtaksdato_tom(tidspunkt3);

        var nullBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setTiltakspenger(false);


        var liste = List.of(midtImellomBruker, senestTomBruker, tidligstTomBruker, nullBruker);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        Filtervalg filtervalg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setYtelseTiltakspenger(List.of(YtelseTiltakspenger.HAR_TILTAKSPENGER, YtelseTiltakspenger.HAR_IKKE_TILTAKSPENGER));

        BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.TILTAKSPENGER_VEDTAKSDATO_TOM,
                filtervalg,
                null,
                null
        );
        BrukereMedAntall brukereMedAntall2 = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                Sorteringsrekkefolge.SYNKENDE,
                Sorteringsfelt.TILTAKSPENGER_VEDTAKSDATO_TOM,
                filtervalg,
                null,
                null
        );

        List<Bruker> brukereStigende = brukereMedAntall.getBrukere();
        List<Bruker> brukereSynkende = brukereMedAntall2.getBrukere();

        assertThat(brukereStigende.size()).isEqualTo(4);
        assertThat(brukereStigende.get(0).getFnr()).isEqualTo(tidligstTomBruker.getFnr());
        assertThat(brukereStigende.get(3).getFnr()).isEqualTo(nullBruker.getFnr());

        assertThat(brukereSynkende.get(0).getFnr()).isEqualTo(nullBruker.getFnr());
        assertThat(brukereSynkende.get(1).getFnr()).isEqualTo(senestTomBruker.getFnr());
        assertThat(brukereSynkende.get(3).getFnr()).isEqualTo(tidligstTomBruker.getFnr());
    }

    @Test
    void skal_sortere_brukere_pa_aap_rettighetstype() {
        var bistandsbehovBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setAap_kelvin(true)
                .setAap_kelvin_rettighetstype(AapRettighetstype.BISTANDSBEHOV);

        var studentBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setAap_kelvin(true)
                .setAap_kelvin_rettighetstype(AapRettighetstype.STUDENT);

        var sykepengeerstatningBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setAap_kelvin(true)
                .setAap_kelvin_rettighetstype(AapRettighetstype.SYKEPENGEERSTATNING);

        var nullBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setAap_kelvin(false);


        var liste = List.of(sykepengeerstatningBruker, bistandsbehovBruker, studentBruker, nullBruker);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        Filtervalg filtervalg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setYtelseAapKelvin(List.of(YtelseAapKelvin.HAR_AAP, YtelseAapKelvin.HAR_IKKE_AAP));

        BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.AAP_KELVIN_RETTIGHETSTYPE,
                filtervalg,
                null,
                null
        );
        BrukereMedAntall brukereMedAntall2 = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                Sorteringsrekkefolge.SYNKENDE,
                Sorteringsfelt.AAP_KELVIN_RETTIGHETSTYPE,
                filtervalg,
                null,
                null
        );

        List<Bruker> brukereStigende = brukereMedAntall.getBrukere();
        List<Bruker> brukereSynkende = brukereMedAntall2.getBrukere();

        assertThat(brukereStigende.size()).isEqualTo(4);
        assertThat(brukereStigende.get(0).getFnr()).isEqualTo(bistandsbehovBruker.getFnr());
        assertThat(brukereStigende.get(1).getFnr()).isEqualTo(studentBruker.getFnr());
        assertThat(brukereStigende.get(3).getFnr()).isEqualTo(nullBruker.getFnr());


        assertThat(brukereSynkende.get(0).getFnr()).isEqualTo(sykepengeerstatningBruker.getFnr());
        assertThat(brukereSynkende.get(2).getFnr()).isEqualTo(bistandsbehovBruker.getFnr());
        assertThat(brukereSynkende.get(3).getFnr()).isEqualTo(nullBruker.getFnr());
    }

    @Test
    void skal_sortere_brukere_pa_tiltakspenger_rettighet() {
        var bruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setTiltakspenger(true)
                .setTiltakspenger_rettighet(TiltakspengerRettighet.TILTAKSPENGER);

        var bruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setTiltakspenger(true)
                .setTiltakspenger_rettighet(TiltakspengerRettighet.TILTAKSPENGER_OG_BARNETILLEGG);

        var nullBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setTiltakspenger(false);


        var liste = List.of(bruker1, bruker2, nullBruker);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        Filtervalg filtervalg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setYtelseTiltakspenger(List.of(YtelseTiltakspenger.HAR_TILTAKSPENGER, YtelseTiltakspenger.HAR_IKKE_TILTAKSPENGER));

        BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.TILTAKSPENGER_RETTIGHET,
                filtervalg,
                null,
                null
        );
        BrukereMedAntall brukereMedAntall2 = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                Sorteringsrekkefolge.SYNKENDE,
                Sorteringsfelt.TILTAKSPENGER_RETTIGHET,
                filtervalg,
                null,
                null
        );

        List<Bruker> brukereStigende = brukereMedAntall.getBrukere();
        List<Bruker> brukereSynkende = brukereMedAntall2.getBrukere();

        assertThat(brukereStigende.size()).isEqualTo(3);
        assertThat(brukereStigende.get(0).getFnr()).isEqualTo(bruker1.getFnr());
        assertThat(brukereStigende.get(1).getFnr()).isEqualTo(bruker2.getFnr());
        assertThat(brukereStigende.get(2).getFnr()).isEqualTo(nullBruker.getFnr());

        assertThat(brukereSynkende.get(0).getFnr()).isEqualTo(bruker2.getFnr());
        assertThat(brukereSynkende.get(2).getFnr()).isEqualTo(nullBruker.getFnr());
    }


    @Test
    void skal_sortere_brukere_pa_tildelt_tidspunkt() {
        LocalDateTime tidspunkt1 = LocalDateTime.now();
        LocalDateTime tidspunkt2 = LocalDateTime.now().plusDays(2);
        LocalDateTime tidspunkt3 = LocalDateTime.now().plusDays(3);

        var tidligstTildeltBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setTildelt_tidspunkt(tidspunkt1);

        var midtImellomBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setTildelt_tidspunkt(tidspunkt2);

        var senestTildeltBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setTildelt_tidspunkt(tidspunkt3);

        var nullBruker = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setTildelt_tidspunkt(null);


        var liste = List.of(midtImellomBruker, senestTildeltBruker, tidligstTildeltBruker, nullBruker);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        Filtervalg filtervalg = new Filtervalg()
                .setFerdigfilterListe(emptyList());

        BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.TILDELT_TIDSPUNKT,
                filtervalg,
                null,
                null
        );
        BrukereMedAntall brukereMedAntall2 = opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                Sorteringsrekkefolge.SYNKENDE,
                Sorteringsfelt.TILDELT_TIDSPUNKT,
                filtervalg,
                null,
                null
        );

        List<Bruker> brukereStigende = brukereMedAntall.getBrukere();
        List<Bruker> brukereSynkende = brukereMedAntall2.getBrukere();

        assertThat(brukereStigende.size()).isEqualTo(4);
        assertThat(brukereStigende.get(0).getFnr()).isEqualTo(tidligstTildeltBruker.getFnr());
        assertThat(brukereStigende.get(3).getFnr()).isEqualTo(nullBruker.getFnr());

        assertThat(brukereSynkende.get(0).getFnr()).isEqualTo(nullBruker.getFnr());
        assertThat(brukereSynkende.get(1).getFnr()).isEqualTo(senestTildeltBruker.getFnr());
        assertThat(brukereSynkende.get(3).getFnr()).isEqualTo(tidligstTildeltBruker.getFnr());
    }

        @Test
    @SneakyThrows
    void skal_indeksere_hendelse_data_riktig_for_utgatt_varsel() {
        Hendelse hendelse = genererRandomHendelse(Kategori.UTGATT_VARSEL);
        OppfolgingsBruker oppfolgingsBruker = new OppfolgingsBruker()
                .setFnr("11111199999")
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setUtgatt_varsel(hendelse.getHendelse());
        skrivBrukereTilTestindeks(oppfolgingsBruker);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == 1);

        BrukereMedAntall respons = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                new Filtervalg().setFerdigfilterListe(emptyList()),
                null,
                null
        );
        Bruker bruker = respons.getBrukere().getFirst();
        Hendelse.HendelseInnhold utgattVarsel = bruker.getUtgattVarsel();

        assertThat(respons.getAntall()).isEqualTo(1);
        assertThat(utgattVarsel).isNotNull();
        assertThat(utgattVarsel.getBeskrivelse()).isEqualTo(oppfolgingsBruker.getUtgatt_varsel().getBeskrivelse());
        assertThat(utgattVarsel.getDetaljer()).isEqualTo(oppfolgingsBruker.getUtgatt_varsel().getDetaljer());
        assertThat(utgattVarsel.getLenke()).isEqualTo(oppfolgingsBruker.getUtgatt_varsel().getLenke());
    }

    private BrukereMedAntall sorterBrukerePaStandardsorteringenAktorid(OpensearchService osService) {
        return osService.hentBrukere(
                TEST_ENHET,
                empty(),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                new Filtervalg().setFerdigfilterListe(emptyList()),
                null,
                null
        );
    }

    private boolean veilederExistsInResponse(String veilederId, BrukereMedAntall brukere) {
        return brukere.getBrukere().stream().anyMatch(bruker -> veilederId.equals(bruker.getVeilederId()));
    }

    private boolean userExistsInResponse(OppfolgingsBruker bruker, BrukereMedAntall brukere) {
        return brukere.getBrukere().stream().anyMatch(b -> bruker.getFnr().equals(b.getFnr()));
    }

    private Long facetResultCountForVeileder(String testVeileder1, FacetResults portefoljestorrelser) {
        return portefoljestorrelser.getFacetResults().stream().filter(it -> testVeileder1.equals(it.getValue())).map(Facet::getCount).toList().getFirst();
    }

    private void skrivBrukereTilTestindeks(List<OppfolgingsBruker> brukere) {
        OppfolgingsBruker[] array = new OppfolgingsBruker[brukere.size()];
        skrivBrukereTilTestindeks(brukere.toArray(array));
    }

    @SneakyThrows
    private void skrivBrukereTilTestindeks(OppfolgingsBruker... brukere) {
        opensearchIndexer.skrivBulkTilIndeks(indexName.getValue(), List.of(brukere));
    }

    private static OppfolgingsBruker genererRandomBruker(
            String enhet,
            String veilederId
    ) {
        return genererRandomBruker(enhet, veilederId, null, false);
    }

    private static OppfolgingsBruker genererRandomBruker(
            String enhet,
            String veilederId,
            String diskresjonskode,
            boolean egenAnsatt
    ) {
        OppfolgingsBruker bruker = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().toString())
                .setFnr(randomFnr().get())
                .setOppfolging(true)
                .setEnhet_id(enhet);

        if (veilederId != null) {
            bruker.setVeileder_id(veilederId);
        }

        if (diskresjonskode != null) {
            bruker.setDiskresjonskode(diskresjonskode);
        }

        if (egenAnsatt) {
            bruker.setEgen_ansatt(true);
        }

        return bruker;
    }
}
