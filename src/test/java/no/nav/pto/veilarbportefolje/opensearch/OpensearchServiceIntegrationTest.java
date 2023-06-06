package no.nav.pto.veilarbportefolje.opensearch;

import lombok.SneakyThrows;
import no.nav.common.abac.Pep;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;
import no.nav.poao_tilgang.client.Decision;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.auth.PoaoTilgangWrapper;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.opensearch.domene.OpensearchResponse;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData;
import no.nav.pto.veilarbportefolje.persononinfo.domene.Adressebeskyttelse;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Avvik14aVedtak;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import static no.nav.pto.veilarbportefolje.opensearch.BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ;
import static no.nav.pto.veilarbportefolje.opensearch.BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchQueryBuilder.byggArbeidslisteQuery;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static no.nav.pto.veilarbportefolje.util.OpensearchTestClient.pollOpensearchUntil;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OpensearchServiceIntegrationTest extends EndToEndTest {
    private static String TEST_ENHET = randomNavKontor().getValue();
    private static final String TEST_VEILEDER_0 = randomVeilederId().getValue();
    private static final String TEST_VEILEDER_1 = randomVeilederId().getValue();
    private static final String TEST_VEILEDER_2 = randomVeilederId().getValue();
    private static final String TEST_VEILEDER_3 = randomVeilederId().getValue();
    private static final String LITE_PRIVILEGERT_VEILEDER = randomVeilederId().getValue();

    private final OpensearchService opensearchService;
    private final OpensearchIndexer opensearchIndexer;
    private final VeilarbVeilederClient veilarbVeilederClientMock;
    private final Pep pep;
    private final AuthContextHolder authContextHolder;

    private PoaoTilgangWrapper poaoTilgangWrapper;

    @Autowired
    public OpensearchServiceIntegrationTest(
            OpensearchService opensearchService,
            OpensearchIndexer opensearchIndexer,
            VeilarbVeilederClient veilarbVeilederClientMock,
            AuthContextHolder authContextHolder,
            Pep pep,
            PoaoTilgangWrapper poaoTilgangWrapper
    ) {
        this.opensearchService = opensearchService;
        this.opensearchIndexer = opensearchIndexer;
        this.veilarbVeilederClientMock = veilarbVeilederClientMock;
        this.authContextHolder = authContextHolder;
        this.pep = pep;
        this.poaoTilgangWrapper = mock(PoaoTilgangWrapper.class);
    }

    @BeforeEach
    void byttenhet() {
        TEST_ENHET = randomNavKontor().getValue();
    }

    @Test
    void skal_kun_hente_brukere_som_innlogget_veileder_har_innsynsrett_pa_nar_man_henter_enhetens_portefolje() {
        List<String> veilederePaEnhet = List.of(TEST_VEILEDER_0, TEST_VEILEDER_1, TEST_VEILEDER_2, TEST_VEILEDER_3);

        doReturn(veilederePaEnhet).when(veilarbVeilederClientMock).hentVeilederePaaEnhet(EnhetId.of(TEST_ENHET));
        doReturn(false).when(unleashService).isEnabled(FeatureToggle.POAO_TILGANG_ENABLED);
        doReturn(true).when(unleashService).isEnabled(FeatureToggle.BRUK_FILTER_FOR_BRUKERINNSYN_TILGANGER);

        doReturn(true).when(pep).harVeilederTilgangTilKode6(NavIdent.of(TEST_VEILEDER_0));
        doReturn(false).when(pep).harVeilederTilgangTilKode7(NavIdent.of(TEST_VEILEDER_0));
        doReturn(false).when(pep).harVeilederTilgangTilEgenAnsatt(NavIdent.of(TEST_VEILEDER_0));

        doReturn(false).when(pep).harVeilederTilgangTilKode6(NavIdent.of(TEST_VEILEDER_1));
        doReturn(true).when(pep).harVeilederTilgangTilKode7(NavIdent.of(TEST_VEILEDER_1));
        doReturn(false).when(pep).harVeilederTilgangTilEgenAnsatt(NavIdent.of(TEST_VEILEDER_1));

        doReturn(false).when(pep).harVeilederTilgangTilKode6(NavIdent.of(TEST_VEILEDER_2));
        doReturn(false).when(pep).harVeilederTilgangTilKode7(NavIdent.of(TEST_VEILEDER_2));
        doReturn(true).when(pep).harVeilederTilgangTilEgenAnsatt(NavIdent.of(TEST_VEILEDER_2));

        doReturn(false).when(pep).harVeilederTilgangTilKode6(NavIdent.of(TEST_VEILEDER_3));
        doReturn(true).when(pep).harVeilederTilgangTilKode7(NavIdent.of(TEST_VEILEDER_3));
        doReturn(true).when(pep).harVeilederTilgangTilEgenAnsatt(NavIdent.of(TEST_VEILEDER_3));

        OppfolgingsBruker kode6Bruker_0 = genererRandomBruker(true, TEST_ENHET, null, "6", false);
        OppfolgingsBruker kode6Bruker_1 = genererRandomBruker(true, TEST_ENHET, null, "6", false);
        OppfolgingsBruker kode6Bruker_2 = genererRandomBruker(true, TEST_ENHET, null, "6", false);
        OppfolgingsBruker kode6Bruker_3 = genererRandomBruker(true, TEST_ENHET, null, "6", false);

        OppfolgingsBruker kode7Bruker_0 = genererRandomBruker(true, TEST_ENHET, null, "7", false);
        OppfolgingsBruker kode7Bruker_1 = genererRandomBruker(true, TEST_ENHET, null, "7", false);
        OppfolgingsBruker kode7Bruker_2 = genererRandomBruker(true, TEST_ENHET, null, "7", false);
        OppfolgingsBruker kode7Bruker_3 = genererRandomBruker(true, TEST_ENHET, null, "7", false);

        OppfolgingsBruker egenAnsattBruker_0 = genererRandomBruker(true, TEST_ENHET, null, null, true);
        OppfolgingsBruker egenAnsattBruker_1 = genererRandomBruker(true, TEST_ENHET, null, null, true);
        OppfolgingsBruker egenAnsattBruker_2 = genererRandomBruker(true, TEST_ENHET, null, null, true);
        OppfolgingsBruker egenAnsattBruker_3 = genererRandomBruker(true, TEST_ENHET, null, null, true);

        OppfolgingsBruker egenAnsattOgKode7Bruker_0 = genererRandomBruker(true, TEST_ENHET, null, "7", true);
        OppfolgingsBruker egenAnsattOgKode7Bruker_1 = genererRandomBruker(true, TEST_ENHET, null, "7", true);
        OppfolgingsBruker egenAnsattOgKode7Bruker_2 = genererRandomBruker(true, TEST_ENHET, null, "7", true);
        OppfolgingsBruker egenAnsattOgKode7Bruker_3 = genererRandomBruker(true, TEST_ENHET, null, "7", true);

        List<OppfolgingsBruker> brukere = List.of(
                kode6Bruker_0,
                kode6Bruker_1,
                kode6Bruker_2,
                kode6Bruker_3,
                kode7Bruker_0,
                kode7Bruker_1,
                kode7Bruker_2,
                kode7Bruker_3,
                egenAnsattBruker_0,
                egenAnsattBruker_1,
                egenAnsattBruker_2,
                egenAnsattBruker_3,
                egenAnsattOgKode7Bruker_0,
                egenAnsattOgKode7Bruker_1,
                egenAnsattOgKode7Bruker_2,
                egenAnsattOgKode7Bruker_3
        );

        skrivBrukereTilTestindeks(brukere);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == brukere.size());

        BrukereMedAntall brukereSomVeilederMedKode6TilgangHarInnsynsrettPa = loggInnVeilederOgHentEnhetPortefolje(opensearchService, TEST_VEILEDER_0, authContextHolder);
        BrukereMedAntall brukereSomVeilederMedKode7TilgangHarInnsynsrettPa = loggInnVeilederOgHentEnhetPortefolje(opensearchService, TEST_VEILEDER_1, authContextHolder);
        BrukereMedAntall brukereSomVeilederMedEgenAnsattTilgangHarInnsynsrettPa = loggInnVeilederOgHentEnhetPortefolje(opensearchService, TEST_VEILEDER_2, authContextHolder);
        BrukereMedAntall brukereSomVeilederMedEgenAnsattOgKode7TilgangHarInnsynsrettPa = loggInnVeilederOgHentEnhetPortefolje(opensearchService, TEST_VEILEDER_3, authContextHolder);

        assertThat(brukereSomVeilederMedKode6TilgangHarInnsynsrettPa.getAntall()).isEqualTo(4);
        assertThat(brukereSomVeilederMedKode7TilgangHarInnsynsrettPa.getAntall()).isEqualTo(4);
        assertThat(brukereSomVeilederMedEgenAnsattTilgangHarInnsynsrettPa.getAntall()).isEqualTo(4);
        assertThat(brukereSomVeilederMedEgenAnsattOgKode7TilgangHarInnsynsrettPa.getAntall()).isEqualTo(12);

        assertThat(brukereSomVeilederMedKode6TilgangHarInnsynsrettPa.getBrukere()).containsExactlyInAnyOrder(
                Bruker.of(kode6Bruker_0, true, false),
                Bruker.of(kode6Bruker_1, true, false),
                Bruker.of(kode6Bruker_2, true, false),
                Bruker.of(kode6Bruker_3, true, false)
        );
        assertThat(brukereSomVeilederMedKode7TilgangHarInnsynsrettPa.getBrukere()).containsExactlyInAnyOrder(
                Bruker.of(kode7Bruker_0, true, false),
                Bruker.of(kode7Bruker_1, true, false),
                Bruker.of(kode7Bruker_2, true, false),
                Bruker.of(kode7Bruker_3, true, false)
        );
        assertThat(brukereSomVeilederMedEgenAnsattTilgangHarInnsynsrettPa.getBrukere()).containsExactlyInAnyOrder(
                Bruker.of(egenAnsattBruker_0, true, false),
                Bruker.of(egenAnsattBruker_1, true, false),
                Bruker.of(egenAnsattBruker_2, true, false),
                Bruker.of(egenAnsattBruker_3, true, false)
        );
        assertThat(brukereSomVeilederMedEgenAnsattOgKode7TilgangHarInnsynsrettPa.getBrukere()).containsExactlyInAnyOrder(
                Bruker.of(kode7Bruker_0, true, false),
                Bruker.of(kode7Bruker_1, true, false),
                Bruker.of(kode7Bruker_2, true, false),
                Bruker.of(kode7Bruker_3, true, false),
                Bruker.of(egenAnsattBruker_0, true, false),
                Bruker.of(egenAnsattBruker_1, true, false),
                Bruker.of(egenAnsattBruker_2, true, false),
                Bruker.of(egenAnsattBruker_3, true, false),
                Bruker.of(egenAnsattOgKode7Bruker_0, true, false),
                Bruker.of(egenAnsattOgKode7Bruker_1, true, false),
                Bruker.of(egenAnsattOgKode7Bruker_2, true, false),
                Bruker.of(egenAnsattOgKode7Bruker_3, true, false)
        );
    }

    @Test
    void skal_kun_hente_brukere_som_innlogget_veileder_har_innsynsrett_pa_nar_man_henter_veileders_portefolje() {
        List<String> veilederePaEnhet = List.of(TEST_VEILEDER_0, TEST_VEILEDER_1, TEST_VEILEDER_2, TEST_VEILEDER_3);

        doReturn(veilederePaEnhet).when(veilarbVeilederClientMock).hentVeilederePaaEnhet(EnhetId.of(TEST_ENHET));
        doReturn(false).when(unleashService).isEnabled(FeatureToggle.POAO_TILGANG_ENABLED);
        doReturn(true).when(unleashService).isEnabled(FeatureToggle.BRUK_FILTER_FOR_BRUKERINNSYN_TILGANGER);

        doReturn(true).when(pep).harVeilederTilgangTilKode6(NavIdent.of(TEST_VEILEDER_0));
        doReturn(false).when(pep).harVeilederTilgangTilKode7(NavIdent.of(TEST_VEILEDER_0));
        doReturn(false).when(pep).harVeilederTilgangTilEgenAnsatt(NavIdent.of(TEST_VEILEDER_0));

        doReturn(false).when(pep).harVeilederTilgangTilKode6(NavIdent.of(TEST_VEILEDER_1));
        doReturn(true).when(pep).harVeilederTilgangTilKode7(NavIdent.of(TEST_VEILEDER_1));
        doReturn(false).when(pep).harVeilederTilgangTilEgenAnsatt(NavIdent.of(TEST_VEILEDER_1));

        doReturn(false).when(pep).harVeilederTilgangTilKode6(NavIdent.of(TEST_VEILEDER_2));
        doReturn(false).when(pep).harVeilederTilgangTilKode7(NavIdent.of(TEST_VEILEDER_2));
        doReturn(true).when(pep).harVeilederTilgangTilEgenAnsatt(NavIdent.of(TEST_VEILEDER_2));

        doReturn(false).when(pep).harVeilederTilgangTilKode6(NavIdent.of(TEST_VEILEDER_3));
        doReturn(true).when(pep).harVeilederTilgangTilKode7(NavIdent.of(TEST_VEILEDER_3));
        doReturn(true).when(pep).harVeilederTilgangTilEgenAnsatt(NavIdent.of(TEST_VEILEDER_3));

        OppfolgingsBruker kode6Bruker_medVeileder0Tilordnet = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, "6", false);
        OppfolgingsBruker kode7Bruker_medVeileder0Tilordnet = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, "7", false);
        OppfolgingsBruker egenAnsattBruker_medVeileder0Tilordnet = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, null, true);
        OppfolgingsBruker egenAnsattOgKode7Bruker_medVeileder0Tilordnet = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, "7", true);

        OppfolgingsBruker kode6Bruker_medVeileder1Tilordnet = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, "6", false);
        OppfolgingsBruker kode7Bruker_medVeileder1Tilordnet = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, "7", false);
        OppfolgingsBruker egenAnsattBruker_medVeileder1Tilordnet = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, null, true);
        OppfolgingsBruker egenAnsattOgKode7Bruker_medVeileder1Tilordnet = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, "7", true);

        OppfolgingsBruker kode6Bruker_medVeileder2Tilordnet = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_2, "6", false);
        OppfolgingsBruker kode7Bruker_medVeileder2Tilordnet = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_2, "7", false);
        OppfolgingsBruker egenAnsattBruker_medVeileder2Tilordnet = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_2, null, true);
        OppfolgingsBruker egenAnsattOgKode7Bruker_medVeileder2Tilordnet = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_2, "7", true);

        OppfolgingsBruker kode6Bruker_medVeileder3Tilordnet = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_3, "6", false);
        OppfolgingsBruker kode7Bruker_medVeileder3Tilordnet = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_3, "7", false);
        OppfolgingsBruker egenAnsattBruker_medVeileder3Tilordnet = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_3, null, true);
        OppfolgingsBruker egenAnsattOgKode7Bruker_medVeileder3Tilordnet = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_3, "7", true);

        List<OppfolgingsBruker> brukere = List.of(
                kode6Bruker_medVeileder0Tilordnet,
                kode7Bruker_medVeileder0Tilordnet,
                egenAnsattBruker_medVeileder0Tilordnet,
                egenAnsattOgKode7Bruker_medVeileder0Tilordnet,
                kode6Bruker_medVeileder1Tilordnet,
                kode7Bruker_medVeileder1Tilordnet,
                egenAnsattBruker_medVeileder1Tilordnet,
                egenAnsattOgKode7Bruker_medVeileder1Tilordnet,
                kode6Bruker_medVeileder2Tilordnet,
                kode7Bruker_medVeileder2Tilordnet,
                egenAnsattBruker_medVeileder2Tilordnet,
                egenAnsattOgKode7Bruker_medVeileder2Tilordnet,
                kode6Bruker_medVeileder3Tilordnet,
                kode7Bruker_medVeileder3Tilordnet,
                egenAnsattBruker_medVeileder3Tilordnet,
                egenAnsattOgKode7Bruker_medVeileder3Tilordnet
        );

        skrivBrukereTilTestindeks(brukere);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == brukere.size());

        BrukereMedAntall brukereSomVeilederMedKode6TilgangHarInnsynsrettPa = loggInnVeilederOgHentVeilederPortefolje(opensearchService, TEST_VEILEDER_0, authContextHolder);
        BrukereMedAntall brukereSomVeilederMedKode7TilgangHarInnsynsrettPa = loggInnVeilederOgHentVeilederPortefolje(opensearchService, TEST_VEILEDER_1, authContextHolder);
        BrukereMedAntall brukereSomVeilederMedEgenAnsattTilgangHarInnsynsrettPa = loggInnVeilederOgHentVeilederPortefolje(opensearchService, TEST_VEILEDER_2, authContextHolder);
        BrukereMedAntall brukereSomVeilederMedEgenAnsattOgKode7TilgangHarInnsynsrettPa = loggInnVeilederOgHentVeilederPortefolje(opensearchService, TEST_VEILEDER_3, authContextHolder);

        assertThat(brukereSomVeilederMedKode6TilgangHarInnsynsrettPa.getAntall()).isEqualTo(1);
        assertThat(brukereSomVeilederMedKode7TilgangHarInnsynsrettPa.getAntall()).isEqualTo(1);
        assertThat(brukereSomVeilederMedEgenAnsattTilgangHarInnsynsrettPa.getAntall()).isEqualTo(1);
        assertThat(brukereSomVeilederMedEgenAnsattOgKode7TilgangHarInnsynsrettPa.getAntall()).isEqualTo(3);

        assertThat(brukereSomVeilederMedKode6TilgangHarInnsynsrettPa.getBrukere()).containsExactlyInAnyOrder(
                Bruker.of(kode6Bruker_medVeileder0Tilordnet, false, false)
        );
        assertThat(brukereSomVeilederMedKode7TilgangHarInnsynsrettPa.getBrukere()).containsExactlyInAnyOrder(
                Bruker.of(kode7Bruker_medVeileder1Tilordnet, false, false)
        );
        assertThat(brukereSomVeilederMedEgenAnsattTilgangHarInnsynsrettPa.getBrukere()).containsExactlyInAnyOrder(
                Bruker.of(egenAnsattBruker_medVeileder2Tilordnet, false, false)
        );
        assertThat(brukereSomVeilederMedEgenAnsattOgKode7TilgangHarInnsynsrettPa.getBrukere()).containsExactlyInAnyOrder(
                Bruker.of(kode7Bruker_medVeileder3Tilordnet, false, false),
                Bruker.of(egenAnsattBruker_medVeileder3Tilordnet, false, false),
                Bruker.of(egenAnsattOgKode7Bruker_medVeileder3Tilordnet, false, false)
        );
    }

    @Test
    void skal_kun_hente_ut_brukere_under_oppfolging() {
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
    void skal_hente_riktige_statustall_for_veileder() {
        doReturn(false).when(pep).harVeilederTilgangTilKode6(NavIdent.of(TEST_VEILEDER_0));
        doReturn(false).when(pep).harVeilederTilgangTilKode7(NavIdent.of(TEST_VEILEDER_0));
        doReturn(false).when(pep).harVeilederTilgangTilEgenAnsatt(NavIdent.of(TEST_VEILEDER_0));

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

        var kode6BrukerSomVeilederIkkeHarInnsynsrettPa = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode, false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));
        var kode7BrukerSomVeilederIkkeHarInnsynsrettPa = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, Adressebeskyttelse.FORTROLIG.diskresjonskode, false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));
        var egenAnsattBrukerSomVeilederIkkeHarInnsynsrettPa = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, null, true).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        var liste = List.of(testBruker1, testBruker2, inaktivBruker, kode6BrukerSomVeilederIkkeHarInnsynsrettPa, kode7BrukerSomVeilederIkkeHarInnsynsrettPa, egenAnsattBrukerSomVeilederIkkeHarInnsynsrettPa);
        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var statustall = opensearchService.hentStatustallForVeilederPortefolje(TEST_VEILEDER_0, TEST_ENHET);
        assertThat(statustall.getErSykmeldtMedArbeidsgiver()).isEqualTo(0);
        assertThat(statustall.getIavtaltAktivitet()).isEqualTo(1);
        assertThat(statustall.getIkkeIavtaltAktivitet()).isEqualTo(2);
        assertThat(statustall.getInaktiveBrukere()).isEqualTo(1);
        assertThat(statustall.getMinArbeidsliste()).isEqualTo(1);
        assertThat(statustall.getNyeBrukereForVeileder()).isEqualTo(1);
        assertThat(statustall.getTrengerVurdering()).isEqualTo(1);
        assertThat(statustall.getVenterPaSvarFraNAV()).isEqualTo(1);
        assertThat(statustall.getUtlopteAktiviteter()).isEqualTo(1);
    }

    @Test
    public void skal_hente_riktige_statustall_for_enhet_nar_veileder_ikke_har_spesiell_brukerinnsyn_tilgang() {
        List<String> veilederePaEnhet = List.of(TEST_VEILEDER_0, TEST_VEILEDER_1, TEST_VEILEDER_2, TEST_VEILEDER_3);

        doReturn(veilederePaEnhet).when(veilarbVeilederClientMock).hentVeilederePaaEnhet(EnhetId.of(TEST_ENHET));
        doReturn(false).when(unleashService).isEnabled(FeatureToggle.POAO_TILGANG_ENABLED);
        doReturn(true).when(unleashService).isEnabled(FeatureToggle.BRUK_FILTER_FOR_BRUKERINNSYN_TILGANGER);
        doReturn(false).when(pep).harVeilederTilgangTilKode6(any());
        doReturn(false).when(pep).harVeilederTilgangTilKode7(any());
        doReturn(false).when(pep).harVeilederTilgangTilEgenAnsatt(any());

        OppfolgingsBruker kode_6_bruker = genererRandomBruker(true, TEST_ENHET, null, "6", false);
        OppfolgingsBruker kode_6_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, "6", false);
        OppfolgingsBruker kode_6_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, "6", false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker kode_7_bruker = genererRandomBruker(true, TEST_ENHET, null, "7", false);
        OppfolgingsBruker kode_7_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, "7", false);
        OppfolgingsBruker kode_7_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, "7", false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker egen_ansatt_bruker = genererRandomBruker(true, TEST_ENHET, null, null, true);
        OppfolgingsBruker egen_ansatt_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_2, null, true);
        OppfolgingsBruker egen_ansatt_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_2, null, true).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker egen_ansatt_og_kode_7_bruker = genererRandomBruker(true, TEST_ENHET, null, "7", true);
        OppfolgingsBruker egen_ansatt_og_kode_7_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_3, "7", true);
        OppfolgingsBruker egen_ansatt_og_kode_7_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_3, "7", true).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker fordelt_bruker_som_ikke_skal_inkluderes = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_3, null, false);
        OppfolgingsBruker ufordelt_bruker_som_ikke_skal_inkluderes = genererRandomBruker(true, TEST_ENHET, null, null, false);
        OppfolgingsBruker bruker_som_venter_pa_svar_fra_nav_som_ikke_skal_inkluderes = genererRandomBruker(true, TEST_ENHET, null, null, false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

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
                fordelt_bruker_som_ikke_skal_inkluderes,
                ufordelt_bruker_som_ikke_skal_inkluderes,
                bruker_som_venter_pa_svar_fra_nav_som_ikke_skal_inkluderes
        );

        skrivBrukereTilTestindeks(brukere);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == brukere.size());

        Statustall respons = opensearchService.hentStatusTallForEnhetPortefolje(TEST_ENHET, BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ);

        assertThat(respons.getTotalt()).isEqualTo(12);
        assertThat(respons.getUfordelteBrukere()).isEqualTo(4);
        assertThat(respons.getVenterPaSvarFraNAV()).isEqualTo(4);
    }

    @Test
    public void skal_hente_riktige_statustall_for_veileder_naar_veileder_har_brukerinnsyn_tilgang_til_kode_7_brukere() {
        List<String> veilederePaEnhet = List.of(TEST_VEILEDER_0, TEST_VEILEDER_1, TEST_VEILEDER_2, TEST_VEILEDER_3);

        doReturn(veilederePaEnhet).when(veilarbVeilederClientMock).hentVeilederePaaEnhet(EnhetId.of(TEST_ENHET));
        doReturn(false).when(unleashService).isEnabled(FeatureToggle.POAO_TILGANG_ENABLED);
        doReturn(false).when(pep).harVeilederTilgangTilKode6(NavIdent.of(TEST_VEILEDER_0));
        doReturn(true).when(pep).harVeilederTilgangTilKode7(NavIdent.of(TEST_VEILEDER_0));
        doReturn(false).when(pep).harVeilederTilgangTilEgenAnsatt(NavIdent.of(TEST_VEILEDER_0));

        OppfolgingsBruker kode_6_bruker = genererRandomBruker(true, TEST_ENHET, null, "6", false);
        OppfolgingsBruker kode_6_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, "6", false);
        OppfolgingsBruker kode_6_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, "6", false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker kode_7_bruker = genererRandomBruker(true, TEST_ENHET, null, "7", false);
        OppfolgingsBruker kode_7_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, "7", false);
        OppfolgingsBruker kode_7_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, "7", false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker egen_ansatt_bruker = genererRandomBruker(true, TEST_ENHET, null, null, true);
        OppfolgingsBruker egen_ansatt_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, null, true);
        OppfolgingsBruker egen_ansatt_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, null, true).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker egen_ansatt_og_kode_7_bruker = genererRandomBruker(true, TEST_ENHET, null, "7", true);
        OppfolgingsBruker egen_ansatt_og_kode_7_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, "7", true);
        OppfolgingsBruker egen_ansatt_og_kode_7_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, "7", true).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker tilfeldig_fordelt_bruker = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, null, false);
        OppfolgingsBruker tilfeldig_ufordelt_bruker = genererRandomBruker(true, TEST_ENHET, null, null, false);
        OppfolgingsBruker tilfeldig_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, null, false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

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

        Statustall respons = opensearchService.hentStatusTallForEnhetPortefolje(TEST_ENHET, BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ);

        assertThat(respons.getTotalt()).isEqualTo(3);
        assertThat(respons.getVenterPaSvarFraNAV()).isEqualTo(1);
    }

    @Test
    public void skal_hente_riktige_statustall_for_enhet_naar_veileder_har_alle_brukerinnsyn_tilganger() {
        List<String> veilederePaEnhet = List.of(TEST_VEILEDER_0, TEST_VEILEDER_1, TEST_VEILEDER_2, TEST_VEILEDER_3);

        doReturn(veilederePaEnhet).when(veilarbVeilederClientMock).hentVeilederePaaEnhet(EnhetId.of(TEST_ENHET));
        doReturn(false).when(unleashService).isEnabled(FeatureToggle.POAO_TILGANG_ENABLED);
        doReturn(true).when(pep).harVeilederTilgangTilKode6(NavIdent.of(TEST_VEILEDER_0));
        doReturn(true).when(pep).harVeilederTilgangTilKode7(NavIdent.of(TEST_VEILEDER_0));
        doReturn(true).when(pep).harVeilederTilgangTilEgenAnsatt(NavIdent.of(TEST_VEILEDER_0));

        OppfolgingsBruker kode_6_bruker = genererRandomBruker(true, TEST_ENHET, null, "6", false);
        OppfolgingsBruker kode_6_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, "6", false);
        OppfolgingsBruker kode_6_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, "6", false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker kode_7_bruker = genererRandomBruker(true, TEST_ENHET, null, "7", false);
        OppfolgingsBruker kode_7_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, "7", false);
        OppfolgingsBruker kode_7_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, "7", false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker egen_ansatt_bruker = genererRandomBruker(true, TEST_ENHET, null, null, true);
        OppfolgingsBruker egen_ansatt_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, null, true);
        OppfolgingsBruker egen_ansatt_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, null, true).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker egen_ansatt_og_kode_7_bruker = genererRandomBruker(true, TEST_ENHET, null, "7", true);
        OppfolgingsBruker egen_ansatt_og_kode_7_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, "7", true);
        OppfolgingsBruker egen_ansatt_og_kode_7_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, "7", true).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker tilfeldig_fordelt_bruker = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, null, false);
        OppfolgingsBruker tilfeldig_ufordelt_bruker = genererRandomBruker(true, TEST_ENHET, null, null, false);
        OppfolgingsBruker tilfeldig_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, null, false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

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


        Statustall responsUtenBrukerinnsyn = authContextHolder.withContext(
                new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDER_0)),
                () -> opensearchService.hentStatusTallForEnhetPortefolje(TEST_ENHET, BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ)
        );

        Statustall responsMedBrukerinnsyn = authContextHolder.withContext(
                new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDER_0)),
                () -> opensearchService.hentStatusTallForEnhetPortefolje(TEST_ENHET, BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ)
        );

        assertThat(responsMedBrukerinnsyn.getTotalt()).isEqualTo(15);
        assertThat(responsUtenBrukerinnsyn.getTotalt()).isEqualTo(0);
        assertThat(responsMedBrukerinnsyn.getVenterPaSvarFraNAV()).isEqualTo(5);
        assertThat(responsMedBrukerinnsyn.getUfordelteBrukere()).isEqualTo(5);
    }

    @Test
    public void skal_hente_riktige_statustall_for_veileder_når_feature_toggle_er_av() {
        List<String> veilederePaEnhet = List.of(TEST_VEILEDER_0, TEST_VEILEDER_1, TEST_VEILEDER_2, TEST_VEILEDER_3);

        doReturn(veilederePaEnhet).when(veilarbVeilederClientMock).hentVeilederePaaEnhet(EnhetId.of(TEST_ENHET));
        doReturn(false).when(unleashService).isEnabled(FeatureToggle.POAO_TILGANG_ENABLED);
        doReturn(false).when(unleashService).isEnabled(FeatureToggle.BRUK_FILTER_FOR_BRUKERINNSYN_TILGANGER);

        OppfolgingsBruker kode_6_bruker = genererRandomBruker(true, TEST_ENHET, null, "6", false);
        OppfolgingsBruker kode_6_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, "6", false);
        OppfolgingsBruker kode_6_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, "6", false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker kode_7_bruker = genererRandomBruker(true, TEST_ENHET, null, "7", false);
        OppfolgingsBruker kode_7_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, "7", false);
        OppfolgingsBruker kode_7_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, "7", false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker egen_ansatt_bruker = genererRandomBruker(true, TEST_ENHET, null, null, true);
        OppfolgingsBruker egen_ansatt_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, null, true);
        OppfolgingsBruker egen_ansatt_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, null, true).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker egen_ansatt_og_kode_7_bruker = genererRandomBruker(true, TEST_ENHET, null, "7", true);
        OppfolgingsBruker egen_ansatt_og_kode_7_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, "7", true);
        OppfolgingsBruker egen_ansatt_og_kode_7_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, "7", true).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker tilfeldig_fordelt_bruker = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, null, false);
        OppfolgingsBruker tilfeldig_ufordelt_bruker = genererRandomBruker(true, TEST_ENHET, null, null, false);
        OppfolgingsBruker tilfeldig_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, null, false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

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
        doReturn(false).when(unleashService).isEnabled(FeatureToggle.POAO_TILGANG_ENABLED);
        doReturn(false).when(unleashService).isEnabled(FeatureToggle.BRUK_FILTER_FOR_BRUKERINNSYN_TILGANGER);

        OppfolgingsBruker kode_6_bruker = genererRandomBruker(true, TEST_ENHET, null, "6", false);
        OppfolgingsBruker kode_6_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, "6", false);
        OppfolgingsBruker kode_6_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, "6", false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker kode_7_bruker = genererRandomBruker(true, TEST_ENHET, null, "7", false);
        OppfolgingsBruker kode_7_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, "7", false);
        OppfolgingsBruker kode_7_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, "7", false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker egen_ansatt_bruker = genererRandomBruker(true, TEST_ENHET, null, null, true);
        OppfolgingsBruker egen_ansatt_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, null, true);
        OppfolgingsBruker egen_ansatt_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, null, true).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker egen_ansatt_og_kode_7_bruker = genererRandomBruker(true, TEST_ENHET, null, "7", true);
        OppfolgingsBruker egen_ansatt_og_kode_7_bruker_med_tilordnet_veileder = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, "7", true);
        OppfolgingsBruker egen_ansatt_og_kode_7_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_1, "7", true).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

        OppfolgingsBruker tilfeldig_fordelt_bruker = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, null, false);
        OppfolgingsBruker tilfeldig_ufordelt_bruker = genererRandomBruker(true, TEST_ENHET, null, null, false);
        OppfolgingsBruker tilfeldig_bruker_som_venter_pa_svar_fra_nav = genererRandomBruker(true, TEST_ENHET, TEST_VEILEDER_0, null, false).setVenterpasvarfranav(toIsoUTC(LocalDateTime.now()));

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

        var statustall = opensearchService.hentStatusTallForEnhetPortefolje(TEST_ENHET, BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ);
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
                "asc",
                "ikke_satt",
                new Filtervalg().setFerdigfilterListe(List.of(UFORDELTE_BRUKERE)),
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
        var testBruker1 = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setFodselsdag_i_mnd(7)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        var testBruker2 = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().get())
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
                .setEnhet_id(TEST_ENHET)
                .setTalespraaktolk(null)
                .setTegnspraaktolk(null);

        var brukerUttenTalkBehov1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setTalespraaktolk("")
                .setTegnspraaktolk("");

        var liste = List.of(brukerMedTalkBehov1, brukerMedTalkBehov2, brukerUttenTalkBehov, brukerUttenTalkBehov1);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        var filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setTolkebehov(List.of("TALESPRAAKTOLK"));

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
        assertThat(response.getBrukere().stream().filter(x -> x.getTalespraaktolk().equals("JPN")).anyMatch(x -> x.getTolkBehovSistOppdatert().equals("2022-02-22")));
        assertThat(response.getBrukere().stream().filter(x -> x.getTalespraaktolk().equals("SWE")).anyMatch(x -> x.getTolkBehovSistOppdatert().equals("2021-03-23")));


        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setTolkebehov(List.of("TEGNSPRAAKTOLK"));

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
        assertThat(response.getBrukere().stream().filter(x -> x.getTalespraaktolk().equals("SWE")).anyMatch(x -> x.getTolkBehovSistOppdatert().equals("2021-03-23")));

        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setTolkebehov(List.of("TEGNSPRAAKTOLK", "TALESPRAAKTOLK"));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "ikke_satt",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(2);
        assertThat(response.getBrukere().stream().filter(x -> x.getTalespraaktolk().equals("JPN")).anyMatch(x -> x.getTolkBehovSistOppdatert().equals("2022-02-22")));
        assertThat(response.getBrukere().stream().filter(x -> x.getTalespraaktolk().equals("SWE")).anyMatch(x -> x.getTolkBehovSistOppdatert().equals("2021-03-23")));


        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setTolkebehov(List.of("TEGNSPRAAKTOLK", "TALESPRAAKTOLK"))
                .setTolkBehovSpraak(List.of("JPN"));

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
        assertThat(response.getBrukere().stream().filter(x -> x.getTalespraaktolk().equals("JPN")).anyMatch(x -> x.getTolkBehovSistOppdatert().equals("2022-02-22")));

        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setTolkebehov(List.of())
                .setTolkBehovSpraak(List.of("JPN"));

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
        assertThat(response.getBrukere().stream().filter(x -> x.getTalespraaktolk().equals("JPN")).anyMatch(x -> x.getTolkBehovSistOppdatert().equals("2022-02-22")));
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

        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setLandgruppe(List.of("LANDGRUPPE_UKJENT", "LANDGRUPPE_3"));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "ikke_satt",
                filterValg,
                null,
                null
        );
        assertThat(response.getAntall()).isEqualTo(3);
        assertThat(response.getBrukere().stream().filter(x -> x.getFoedeland() != null).anyMatch(x -> x.getFoedeland().equals("SGP")));
        assertThat(response.getBrukere().stream().filter(x -> x.getFoedeland() != null).anyMatch(x -> x.getFoedeland().equals("AZE")));
        assertThat(response.getBrukere().stream().anyMatch(x -> x.getFoedeland() == null));
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
                "ascending",
                "fodeland",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(6);
        assertThat(response.getBrukere().get(0).getFoedeland().equals("Aserbajdsjan"));
        assertThat(response.getBrukere().get(2).getFoedeland().equals("Botswana"));
        assertThat(response.getBrukere().get(2).getFoedeland().equals("Estland"));
        assertThat(response.getBrukere().get(3).getFoedeland().equals("Norge"));
        assertThat(response.getBrukere().get(4).getFoedeland().equals("Singapore"));
        assertThat(response.getBrukere().get(5).getFoedeland() == null);

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "descending",
                "fodeland",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(6);
        assertThat(response.getBrukere().get(0).getFoedeland().equals("Singapore"));
        assertThat(response.getBrukere().get(1).getFoedeland().equals("Norge"));
        assertThat(response.getBrukere().get(2).getFoedeland().equals("Estland"));
        assertThat(response.getBrukere().get(3).getFoedeland().equals("Botswana"));
        assertThat(response.getBrukere().get(4).getFoedeland().equals("Aserbajdsjan"));
        assertThat(response.getBrukere().get(5).getFoedeland() == null);

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "statsborgerskap",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(6);
        assertThat(response.getBrukere().get(0).getHovedStatsborgerskap().getStatsborgerskap().equals("Estland"));
        assertThat(response.getBrukere().get(1).getHovedStatsborgerskap().getStatsborgerskap().equals("Norge"));
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
                "ascending",
                "ikke_satt",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(2);
        assertThat(response.getBrukere().stream().allMatch(x -> x.getBostedKommune().equals("10")));


        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setGeografiskBosted(List.of("1233"));

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
        assertThat(response.getBrukere().stream().allMatch(x -> x.getBostedBydel().equals("1233")));


        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setGeografiskBosted(List.of("10", "1233"));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "ikke_satt",
                filterValg,
                null,
                null
        );
        assertThat(response.getAntall()).isEqualTo(3);
        assertThat(response.getBrukere().stream().filter(x -> x.getBostedKommune().equalsIgnoreCase("10")).count()).isEqualTo(2);
        assertThat(response.getBrukere().stream().anyMatch(x -> x.getBostedBydel().equalsIgnoreCase("1233")));
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
                "ascending",
                "kommunenummer",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(5);
        assertThat(response.getBrukere().get(0).getBostedKommune().equals("10"));
        assertThat(response.getBrukere().get(1).getBostedKommune().equals("10"));
        assertThat(response.getBrukere().get(2).getBostedKommune().equals("12"));
        assertThat(response.getBrukere().get(3).getBostedKommune().equals("12"));
        assertThat(response.getBrukere().get(4).getBostedKommune() == null);

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "descending",
                "bydelsnummer",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(5);
        assertThat(response.getBrukere().get(0).getBostedBydel().equals("1010"));
        assertThat(response.getBrukere().get(1).getBostedBydel().equals("1233"));
        assertThat(response.getBrukere().get(2).getBostedBydel().equals("1233"));
        assertThat(response.getBrukere().get(3).getBostedBydel() == null);
        assertThat(response.getBrukere().get(4).getBostedBydel() == null);

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
                "ascending",
                "ikke_satt",
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
                "ascending",
                "ikke_satt",
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
                "ascending",
                "ikke_satt",
                filterValg,
                null,
                null
        );

        assertThat(response.getBrukere()).hasSize(5);
    }

    @Test
    void skal_ikke_automatisk_sortere_nye_brukere_paa_top() {
        when(unleashService.isEnabled(anyString())).thenReturn(true);
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
                "descending",
                "etternavn",
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
        when(unleashService.isEnabled(anyString())).thenReturn(true);
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
                "descending",
                "etternavn",
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
                .setEnslige_forsorgere_overgangsstonad(new EnsligeForsorgereOvergangsstonad("Hovedperiode", true, LocalDate.now().plusMonths(4), LocalDate.now().minusMonths(2)));

        var bruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setEnslige_forsorgere_overgangsstonad(new EnsligeForsorgereOvergangsstonad("Forlengelse", false, LocalDate.now().plusMonths(3), LocalDate.now().plusMonths(7)));

        var bruker3 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setEnslige_forsorgere_overgangsstonad(new EnsligeForsorgereOvergangsstonad("Utvidelse", false, LocalDate.now().plusMonths(1), LocalDate.now().minusMonths(3)));

        var bruker4 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setEnslige_forsorgere_overgangsstonad(new EnsligeForsorgereOvergangsstonad("Periode før fødsel", true, LocalDate.now().plusMonths(7), LocalDate.now().minusMonths(1)));

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
                "ascending",
                "enslige_forsorgere_utlop_ytelse",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(5);
        assertThat(response.getBrukere().get(0).getFnr().equals(bruker3.getFnr()));
        assertThat(response.getBrukere().get(1).getFnr().equals(bruker2.getFnr()));
        assertThat(response.getBrukere().get(2).getFnr().equals(bruker1.getFnr()));
        assertThat(response.getBrukere().get(3).getFnr().equals(bruker4.getFnr()));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "enslige_forsorgere_om_barnet",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(5);
        assertThat(response.getBrukere().get(0).getFnr().equals(bruker3.getFnr()));
        assertThat(response.getBrukere().get(1).getFnr().equals(bruker1.getFnr()));
        assertThat(response.getBrukere().get(2).getFnr().equals(bruker4.getFnr()));
        assertThat(response.getBrukere().get(3).getFnr().equals(bruker2.getFnr()));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "enslige_forsorgere_aktivitetsplikt",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(5);
        assertThat(response.getBrukere().get(0).getFnr().equals(bruker1.getFnr()));
        assertThat(response.getBrukere().get(1).getFnr().equals(bruker4.getFnr()));
        assertThat(response.getBrukere().get(2).getFnr().equals(bruker2.getFnr()));
        assertThat(response.getBrukere().get(3).getFnr().equals(bruker3.getFnr()));

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "enslige_forsorgere_vedtaksperiodetype",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(5);
        assertThat(response.getBrukere().get(0).getFnr().equals(bruker2.getFnr()));
        assertThat(response.getBrukere().get(1).getFnr().equals(bruker1.getFnr()));
        assertThat(response.getBrukere().get(2).getFnr().equals(bruker4.getFnr()));
        assertThat(response.getBrukere().get(3).getFnr().equals(bruker3.getFnr()));
    }

    @Test
    public void test_filtrering_barn_under_18() {
        var bruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(new BarnUnder18AarData(8, null)));

        var bruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(new BarnUnder18AarData(1, "6"), new BarnUnder18AarData(12, "7")));

        var bruker3 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(new BarnUnder18AarData(5, "7"), new BarnUnder18AarData(11, null)));


        var bruker4 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(emptyList());

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
                .setFerdigfilterListe(List.of())
                .setBarnUnder18Aar(List.of(BarnUnder18Aar.HAR_BARN_UNDER_18_AAR));

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "ikke_satt",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(2);
        assertThat(response.getBrukere().stream().map(Bruker::getFnr).toList().containsAll(List.of(bruker1.getFnr(), bruker2.getFnr(), bruker3.getFnr())));

        response.getBrukere().forEach(bruker -> {
                    if (bruker.getFnr().equals(bruker1.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(1);
                    } else if (bruker.getFnr().equals(bruker2.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(2);
                    } else if (bruker.getFnr().equals(bruker3.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(2);
                    } else if (bruker.getFnr().equals(bruker4.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(0);
                    }
                }
        );

    }

    @Test
    public void test_filtrering_barn_under_18_ingen_tilganger() {
        var bruker1BU = brukerMed1BarnUtenDiskresjonskode();
        var bruker2B67 = brukerMed2Barn6og7();
        var bruker3B67U = brukerMed3Barn67ogIngen();
        var bruker2B7U = brukerMed2Barn7ogIngen();
        var bruker2BU7 = brukerMed2BarnIngenog7();
        var bruker2B7 = brukerMed2BarnMedKode7();
        var bruker2B6 = brukerMed2BarnKode6();
        var brukerTomListe = brukerMedTomBarnliste();
        var brukerIngenListe = brukerUtenBarnliste();

        var liste = List.of(bruker1BU, bruker2B67, bruker3B67U, bruker2B7U, bruker2BU7, bruker2B7, bruker2B6, brukerTomListe, brukerIngenListe);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        Filtervalg filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setBarnUnder18Aar(List.of(BarnUnder18Aar.HAR_BARN_UNDER_18_AAR));

        when(pep.harVeilederTilgangTilKode6(any())).thenReturn(false);
        when(pep.harVeilederTilgangTilKode7(any())).thenReturn(false);
        when(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(new Decision.Deny("", ""));
        when(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(new Decision.Deny("", ""));

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "ikke_satt",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(4);
        assertThat(response.getBrukere().stream().map(Bruker::getFnr).toList().containsAll(List.of(bruker1BU.getFnr(), bruker3B67U.getFnr(), bruker2B7U.getFnr(), bruker2BU7.getFnr())));

        response.getBrukere().forEach(bruker -> {
                    if (bruker.getFnr().equals(bruker1BU.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(1);
                    } else if (bruker.getFnr().equals(bruker3B67U.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(3);
                    } else if (bruker.getFnr().equals(bruker2B7U.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(2);
                    } else if (bruker.getFnr().equals(bruker2BU7.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(2);
                    }
                }
        );

    }

    @Test
    public void test_filtrering_barn_under_18_tilgang_6() {
        var bruker1BU = brukerMed1BarnUtenDiskresjonskode();
        var bruker2B67 = brukerMed2Barn6og7();
        var bruker3B67U = brukerMed3Barn67ogIngen();
        var bruker2B7U = brukerMed2Barn7ogIngen();
        var bruker2BU7 = brukerMed2BarnIngenog7();
        var bruker2B7 = brukerMed2BarnMedKode7();
        var bruker2B6 = brukerMed2BarnKode6();
        var brukerTomListe = brukerMedTomBarnliste();
        var brukerIngenListe = brukerUtenBarnliste();

        var liste = List.of(bruker1BU, bruker2B67, bruker3B67U, bruker2B7U, bruker2BU7, bruker2B7, bruker2B6, brukerTomListe, brukerIngenListe);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        Filtervalg filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setBarnUnder18Aar(List.of(BarnUnder18Aar.HAR_BARN_UNDER_18_AAR));

        when(pep.harVeilederTilgangTilKode6(any())).thenReturn(true);
        when(pep.harVeilederTilgangTilKode7(any())).thenReturn(false);
        when(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Decision.Permit.INSTANCE);
        when(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(new Decision.Deny("", ""));

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "ikke_satt",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(6);
        assertThat(response.getBrukere().stream().map(Bruker::getFnr).toList().containsAll(List.of(bruker1BU.getFnr(), bruker2B67.getFnr(), bruker3B67U.getFnr(), bruker2B7U.getFnr(), bruker2BU7.getFnr(), bruker2B6.getFnr())));

        response.getBrukere().forEach(bruker -> {
                    if (bruker.getFnr().equals(bruker1BU.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(1);
                    } else if (bruker.getFnr().equals(bruker2B67.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(2);
                    } else if (bruker.getFnr().equals(bruker3B67U.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(3);
                    } else if (bruker.getFnr().equals(bruker2B7U.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(2);
                    } else if (bruker.getFnr().equals(bruker2BU7.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(2);
                    } else if (bruker.getFnr().equals(bruker2B6.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(2);
                    } else if (bruker.getFnr().equals(bruker2B7.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData()).isEqualTo(null);
                    } else if (bruker.getFnr().equals(brukerTomListe.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData()).isEqualTo(null);
                    } else if (bruker.getFnr().equals(brukerIngenListe.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData()).isEqualTo(null);
                    }
                }
        );

    }

    @Test
    public void test_filtrering_barn_under_18_tilgang_7() {
        var bruker1BU = brukerMed1BarnUtenDiskresjonskode();
        var bruker2B67 = brukerMed2Barn6og7();
        var bruker3B67U = brukerMed3Barn67ogIngen();
        var bruker2B7U = brukerMed2Barn7ogIngen();
        var bruker2BU7 = brukerMed2BarnIngenog7();
        var bruker2B7 = brukerMed2BarnMedKode7();
        var bruker2B6 = brukerMed2BarnKode6();
        var brukerTomListe = brukerMedTomBarnliste();
        var brukerIngenListe = brukerUtenBarnliste();

        var liste = List.of(bruker1BU, bruker2B67, bruker3B67U, bruker2B7U, bruker2BU7, bruker2B7, bruker2B6, brukerTomListe, brukerIngenListe);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        Filtervalg filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setBarnUnder18Aar(List.of(BarnUnder18Aar.HAR_BARN_UNDER_18_AAR));

        when(pep.harVeilederTilgangTilKode6(any())).thenReturn(false);
        when(pep.harVeilederTilgangTilKode7(any())).thenReturn(true);
        when(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(new Decision.Deny("", ""));
        when(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(Decision.Permit.INSTANCE);

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "ikke_satt",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(6);
        assertThat(response.getBrukere().stream().map(Bruker::getFnr).toList().containsAll(List.of(bruker1BU.getFnr(), bruker2B67.getFnr(), bruker3B67U.getFnr(), bruker2B7U.getFnr(), bruker2BU7.getFnr(), bruker2B7.getFnr())));

        response.getBrukere().forEach(bruker -> {
                    if (bruker.getFnr().equals(bruker1BU.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(1);
                    } else if (bruker.getFnr().equals(bruker2B67.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(2);
                    } else if (bruker.getFnr().equals(bruker3B67U.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(3);
                    } else if (bruker.getFnr().equals(bruker2B7U.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(2);
                    } else if (bruker.getFnr().equals(bruker2BU7.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(2);
                    } else if (bruker.getFnr().equals(bruker2B6.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData()).isEqualTo(null);
                    } else if (bruker.getFnr().equals(bruker2B7.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(2);
                    } else if (bruker.getFnr().equals(brukerTomListe.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData()).isEqualTo(null);
                    } else if (bruker.getFnr().equals(brukerIngenListe.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData()).isEqualTo(null);
                    }
                }
        );

    }

    @Test
    public void test_filtrering_barn_under_18_tilgang_6_7() {
        var bruker1BU = brukerMed1BarnUtenDiskresjonskode();
        var bruker2B67 = brukerMed2Barn6og7();
        var bruker3B67U = brukerMed3Barn67ogIngen();
        var bruker2B7U = brukerMed2Barn7ogIngen();
        var bruker2BU7 = brukerMed2BarnIngenog7();
        var bruker2B7 = brukerMed2BarnMedKode7();
        var bruker2B6 = brukerMed2BarnKode6();
        var brukerTomListe = brukerMedTomBarnliste();
        var brukerIngenListe = brukerUtenBarnliste();

        var liste = List.of(bruker1BU, bruker2B67, bruker3B67U, bruker2B7U, bruker2BU7, bruker2B7, bruker2B6, brukerTomListe, brukerIngenListe);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());

        Filtervalg filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setBarnUnder18Aar(List.of(BarnUnder18Aar.HAR_BARN_UNDER_18_AAR));

        when(pep.harVeilederTilgangTilKode6(any())).thenReturn(true);
        when(pep.harVeilederTilgangTilKode7(any())).thenReturn(true);
        when(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Decision.Permit.INSTANCE);
        when(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(Decision.Permit.INSTANCE);

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "ikke_satt",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(7);
        assertThat(response.getBrukere().stream().map(Bruker::getFnr).toList().containsAll(List.of(bruker1BU.getFnr(), bruker2B67.getFnr(), bruker3B67U.getFnr(), bruker2B7U.getFnr(), bruker2BU7.getFnr(), bruker2B6.getFnr(), bruker2B7.getFnr())));

        response.getBrukere().forEach(bruker -> {
                    if (bruker.getFnr().equals(bruker1BU.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(1);
                    } else if (bruker.getFnr().equals(bruker2B67.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(2);
                    } else if (bruker.getFnr().equals(bruker3B67U.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(3);
                    } else if (bruker.getFnr().equals(bruker2B7U.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(2);
                    } else if (bruker.getFnr().equals(bruker2BU7.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(2);
                    } else if (bruker.getFnr().equals(bruker2B6.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(2);
                    } else if (bruker.getFnr().equals(bruker2B7.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData().size()).isEqualTo(2);
                    } else if (bruker.getFnr().equals(brukerTomListe.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData()).isEqualTo(null);
                    } else if (bruker.getFnr().equals(brukerIngenListe.getFnr())) {
                        assertThat(bruker.getBarnUnder18AarData()).isEqualTo(null);
                    }
                }
        );

    }

    @Test
    public void test_filtrering_barn_under_18_med_alder_filter() {
        var bruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(new BarnUnder18AarData(8, null)));

        var bruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(new BarnUnder18AarData(1, null), new BarnUnder18AarData(12, null)));

        var bruker3 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(new BarnUnder18AarData(5, "7"), new BarnUnder18AarData(11, null)));


        var bruker4 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(new BarnUnder18AarData(16, null), new BarnUnder18AarData(12, null)));

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
                .setFerdigfilterListe(List.of())
                .setBarnUnder18Aar(List.of(BarnUnder18Aar.HAR_BARN_UNDER_18_AAR))
                .setBarnUnder18AarAlder(List.of("1-5"));
        ;

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "ikke_satt",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(2);
        assertThat(response.getBrukere().stream().map(Bruker::getFnr).toList().containsAll(List.of(bruker2.getFnr(), bruker3.getFnr())));
    }

    @Test
    public void test_sorting_barn_under_18_veileder_tilgang_6_7() {

        var bruker1B = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(
                        new BarnUnder18AarData(5, "7")));

        var bruker2barnU = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(
                        new BarnUnder18AarData(8, null),
                        new BarnUnder18AarData(1, "6")));

        var bruker3barn1m6_2U = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(
                        new BarnUnder18AarData(1, null),
                        new BarnUnder18AarData(12, null),
                        new BarnUnder18AarData(4, null)));


        var bruker4barn = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(
                        new BarnUnder18AarData(5, "7"),
                        new BarnUnder18AarData(11, null),
                        new BarnUnder18AarData(4, "6"),
                        new BarnUnder18AarData(1, "7")));


        var bruker5barn = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(
                        new BarnUnder18AarData(5, "7"),
                        new BarnUnder18AarData(11, null),
                        new BarnUnder18AarData(1, null),
                        new BarnUnder18AarData(11, "7"),
                        new BarnUnder18AarData(4, "6")));

        var bruker6b = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(
                        new BarnUnder18AarData(5, "7"),
                        new BarnUnder18AarData(11, "7"),
                        new BarnUnder18AarData(1, null),
                        new BarnUnder18AarData(11, "7"),
                        new BarnUnder18AarData(11, "7"),
                        new BarnUnder18AarData(4, "6")));


        var brukerTomListe = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(emptyList());

        var brukerIngenListe = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET);

        var liste = List.of(bruker1B, bruker2barnU, bruker3barn1m6_2U, bruker4barn, bruker5barn, bruker6b, brukerTomListe, brukerIngenListe);


        when(pep.harVeilederTilgangTilKode6(any())).thenReturn(true);
        when(pep.harVeilederTilgangTilKode7(any())).thenReturn(true);
        when(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Decision.Permit.INSTANCE);
        when(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(Decision.Permit.INSTANCE);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());


        Filtervalg filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setBarnUnder18Aar(List.of(BarnUnder18Aar.HAR_BARN_UNDER_18_AAR));

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "barn_under_18_aar",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(6);
        assertThat(response.getBrukere().get(0).getFnr()).isEqualTo(bruker1B.getFnr());
        assertThat(response.getBrukere().get(1).getFnr()).isEqualTo(bruker2barnU.getFnr());
        assertThat(response.getBrukere().get(2).getFnr()).isEqualTo(bruker3barn1m6_2U.getFnr());
        assertThat(response.getBrukere().get(3).getFnr()).isEqualTo(bruker4barn.getFnr());
        assertThat(response.getBrukere().get(4).getFnr()).isEqualTo(bruker5barn.getFnr());
        assertThat(response.getBrukere().get(5).getFnr()).isEqualTo(bruker6b.getFnr());
    }

    @Test
    public void test_sorting_barn_under_18_veileder_ikke_tilgang_6_7() {

        var bruker1B = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(
                        new BarnUnder18AarData(5, "7")));

        var bruker2barnU = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(
                        new BarnUnder18AarData(8, null),
                        new BarnUnder18AarData(1, "6")));

        var bruker3barn1m6_2U = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(
                        new BarnUnder18AarData(1, null),
                        new BarnUnder18AarData(12, null),
                        new BarnUnder18AarData(4, null)));


        var bruker4barn = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(
                        new BarnUnder18AarData(5, "7"),
                        new BarnUnder18AarData(11, null),
                        new BarnUnder18AarData(4, "6"),
                        new BarnUnder18AarData(1, "7")));


        var bruker5barn = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(
                        new BarnUnder18AarData(5, "7"),
                        new BarnUnder18AarData(11, null),
                        new BarnUnder18AarData(1, null),
                        new BarnUnder18AarData(11, "7"),
                        new BarnUnder18AarData(4, "6")));

        var bruker6b = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(
                        new BarnUnder18AarData(5, "7"),
                        new BarnUnder18AarData(11, "7"),
                        new BarnUnder18AarData(1, null),
                        new BarnUnder18AarData(11, "7"),
                        new BarnUnder18AarData(11, "7"),
                        new BarnUnder18AarData(4, "6")));


        var brukerTomListe = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(emptyList());

        var brukerIngenListe = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET);

        var liste = List.of(bruker1B, bruker2barnU, bruker3barn1m6_2U, bruker4barn, bruker5barn, bruker6b, brukerTomListe, brukerIngenListe);


        when(pep.harVeilederTilgangTilKode6(any())).thenReturn(false);
        when(pep.harVeilederTilgangTilKode7(any())).thenReturn(false);
        when(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(new Decision.Deny("",""));
        when(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(new Decision.Deny("",""));

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());


        Filtervalg filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setBarnUnder18Aar(List.of(BarnUnder18Aar.HAR_BARN_UNDER_18_AAR));

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "barn_under_18_aar",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(5);
        assertThat(response.getBrukere().get(0).getFnr()).isEqualTo(bruker1B.getFnr());
        assertThat(response.getBrukere().get(1).getFnr()).isEqualTo(bruker2barnU.getFnr());
        assertThat(response.getBrukere().get(2).getFnr()).isEqualTo(bruker3barn1m6_2U.getFnr());
        assertThat(response.getBrukere().get(3).getFnr()).isEqualTo(bruker5barn.getFnr());
        assertThat(response.getBrukere().get(4).getFnr()).isEqualTo(bruker3barn1m6_2U.getFnr());
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
                .setYtelse(YtelseFilter.AAP_MAXTID);

        BrukereMedAntall response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "aap_vurderingsfrist",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(3);
        assertThat(response.getBrukere().get(0).getFnr().equals(bruker6.getFnr()));
        assertThat(response.getBrukere().get(1).getFnr().equals(bruker2.getFnr()));
        assertThat(response.getBrukere().get(2).getFnr().equals(bruker4.getFnr()));

        filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setYtelse(YtelseFilter.AAP_UNNTAK);

        response = opensearchService.hentBrukere(
                TEST_ENHET,
                empty(),
                "ascending",
                "aap_vurderingsfrist",
                filterValg,
                null,
                null
        );

        assertThat(response.getAntall()).isEqualTo(3);
        assertThat(response.getBrukere().get(0).getFnr().equals(bruker1.getFnr()));
        assertThat(response.getBrukere().get(1).getFnr().equals(bruker5.getFnr()));
        assertThat(response.getBrukere().get(2).getFnr().equals(bruker3.getFnr()));
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

    private static BrukereMedAntall loggInnVeilederOgHentEnhetPortefolje(OpensearchService opensearchService, String veilederId, AuthContextHolder contextHolder) {
        return contextHolder.withContext(
                new AuthContext(UserRole.INTERN, generateJWT(veilederId)),
                () -> opensearchService.hentBrukere(
                        TEST_ENHET,
                        empty(),
                        "asc",
                        "ikke_satt",
                        new Filtervalg(),
                        null,
                        null
                )
        );
    }

    private static BrukereMedAntall loggInnVeilederOgHentVeilederPortefolje(OpensearchService opensearchService, String veilederId, AuthContextHolder contextHolder) {
        return contextHolder.withContext(
                new AuthContext(UserRole.INTERN, generateJWT(veilederId)),
                () -> opensearchService.hentBrukere(
                        TEST_ENHET,
                        Optional.of(veilederId),
                        "asc",
                        "ikke_satt",
                        new Filtervalg(),
                        null,
                        null
                )
        );
    }

    private static OppfolgingsBruker genererRandomBruker(
            boolean oppfolging,
            String enhet,
            String veilederId,
            String diskresjonskode,
            boolean egenAnsatt
    ) {
        OppfolgingsBruker bruker = new OppfolgingsBruker()
                .setAktoer_id(randomAktorId().toString())
                .setFnr(randomFnr().get())
                .setOppfolging(oppfolging)
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

    public OppfolgingsBruker brukerMed1BarnUtenDiskresjonskode() {
        return new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(randomVeilederId().toString())
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(new BarnUnder18AarData(8, null)));
    }

    public OppfolgingsBruker brukerMed2Barn6og7() {
        return new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(new BarnUnder18AarData(1, "6"), new BarnUnder18AarData(12, "7")));
    }

    public OppfolgingsBruker brukerMed3Barn67ogIngen() {
        return new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(new BarnUnder18AarData(1, "7"), new BarnUnder18AarData(12, "6"), new BarnUnder18AarData(12, null)));
    }

    public OppfolgingsBruker brukerMed2Barn7ogIngen() {
        return new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(new BarnUnder18AarData(5, "7"), new BarnUnder18AarData(11, null)));
    }


    public OppfolgingsBruker brukerMed2BarnIngenog7() {
        return new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(new BarnUnder18AarData(5, null), new BarnUnder18AarData(11, "7")));
    }

    public OppfolgingsBruker brukerMed2BarnMedKode7() {
        return new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(new BarnUnder18AarData(5, "7"), new BarnUnder18AarData(11, "7")));
    }

    public OppfolgingsBruker brukerMed2BarnKode6() {
        return new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(List.of(new BarnUnder18AarData(5, "6"), new BarnUnder18AarData(11, "6")));
    }

    public OppfolgingsBruker brukerMedTomBarnliste() {
        return new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET)
                .setBarn_under_18_aar(emptyList());
    }

    public OppfolgingsBruker brukerUtenBarnliste() {
        return new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_veileder(false)
                .setEnhet_id(TEST_ENHET);
    }

    OppfolgingsBruker brukerMed2BarnKode6 = new OppfolgingsBruker()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setBarn_under_18_aar(List.of(new BarnUnder18AarData(5, "6"), new BarnUnder18AarData(11, "6")));

}
