package no.nav.pto.veilarbportefolje.ensligforsorger;

import lombok.SneakyThrows;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.ensligforsorger.client.EnsligForsorgerClient;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.*;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.output.EnsligeForsorgerOvergangsstønadTiltakDto;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static io.vavr.API.println;
import static java.util.Optional.empty;
import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.pto.veilarbportefolje.domene.EnsligeForsorgere.OVERGANGSSTONAD;
import static no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.Periodetype.NY_PERIODE_FOR_NYTT_BARN;
import static no.nav.pto.veilarbportefolje.util.OpensearchTestClient.pollOpensearchUntil;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static no.nav.pto.veilarbportefolje.util.TestUtil.readTestResourceFile;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class EnsligeForsorgereServiceTest extends EndToEndTest {
    private static NavKontor navKontor = NavKontor.of("1123");
    private static VeilederId veilederId = VeilederId.of("1402");

    private static Fnr hoved_fnr = Fnr.of("2449920301");

    @Autowired
    private OpensearchService opensearchService;

    @Autowired
    private EnsligeForsorgereService ensligeForsorgereService;

    @Autowired
    private EnsligeForsorgereRepository ensligeForsorgereRepository;

    @Autowired
    private AktorClient aktorClient;

    @Autowired
    EnsligForsorgerClient ensligForsorgerClient;

    @Autowired
    private JdbcTemplate postgres;


    @BeforeEach
    public void setup() {
        postgres.update("truncate TABLE enslige_forsorgere CASCADE");
    }

    @Test
    public void testNyOvergangsstonadForBrukerIndex() {
        setInitialState();

        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setEnsligeForsorgere(List.of(OVERGANGSSTONAD));
        filtervalg.setFerdigfilterListe(List.of());

        verifiserAsynkront(2, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall responseBrukere = opensearchService.hentBrukere(
                            navKontor.toString(),
                            empty(),
                            Sorteringsrekkefolge.STIGENDE,
                            Sorteringsfelt.IKKE_SATT,
                            filtervalg,
                            null,
                            null);

                    assertThat(responseBrukere.getAntall()).isEqualTo(1);
                    assertThat(responseBrukere.getBrukere().get(0).getEnsligeForsorgereOvergangsstonad().vedtaksPeriodetype()).isEqualTo("Ny periode for nytt barn");
                    assertThat(responseBrukere.getBrukere().get(0).getEnsligeForsorgereOvergangsstonad().harAktivitetsplikt()).isEqualTo(false);
                    assertThat(responseBrukere.getBrukere().get(0).getEnsligeForsorgereOvergangsstonad().utlopsDato()).isEqualTo(LocalDate.now().plusDays(20));
                    assertThat(responseBrukere.getBrukere().get(0).getEnsligeForsorgereOvergangsstonad().yngsteBarnsFodselsdato()).isEqualTo(LocalDate.of(2023, 5, 4));
                }
        );
    }

    @Test
    public void testAvsluttetOvergangsstonadForBrukerIndex() {
        setInitialState();

        List<Barn> barn = List.of(new Barn("11032245678", null), new Barn(null, LocalDate.of(2023, 5, 4)));
        List<Periode> periodeType = List.of(new Periode(LocalDate.of(2023, 4, 4), LocalDate.of(2024, 4, 4), Periodetype.NY_PERIODE_FOR_NYTT_BARN, Aktivitetstype.BARN_UNDER_ETT_ÅR));
        ensligeForsorgereService.behandleKafkaMeldingLogikk(
                new VedtakOvergangsstønadArbeidsoppfølging(
                        54321L,
                        hoved_fnr.toString(),
                        barn,
                        Stønadstype.OVERGANGSSTØNAD,
                        periodeType,
                        Vedtaksresultat.OPPHØRT
                )
        );

        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setEnsligeForsorgere(List.of(OVERGANGSSTONAD));
        filtervalg.setFerdigfilterListe(List.of());

        verifiserAsynkront(2, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall responseBrukere = opensearchService.hentBrukere(
                            navKontor.toString(),
                            empty(),
                            Sorteringsrekkefolge.STIGENDE,
                            Sorteringsfelt.IKKE_SATT,
                            filtervalg,
                            null,
                            null);

                    assertThat(responseBrukere.getAntall()).isEqualTo(0);
                }
        );
    }

    @Test
    public void test_filtrering_enslige_forsorgere() {
        var bruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(veilederId.toString())
                .setEnhet_id(navKontor.toString());

        var bruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(veilederId.toString())
                .setNy_for_veileder(false)
                .setEnhet_id(navKontor.toString())
                .setEnslige_forsorgere_overgangsstonad(new EnsligeForsorgereOvergangsstonad("Forlengelse", false, LocalDate.now().plusMonths(3), LocalDate.now().plusMonths(7)));

        var bruker3 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(veilederId.toString())
                .setNy_for_veileder(false)
                .setEnhet_id(navKontor.toString())
                .setEnslige_forsorgere_overgangsstonad(new EnsligeForsorgereOvergangsstonad("Utvidelse", false, LocalDate.now().plusMonths(1), LocalDate.now().minusMonths(3)));

        var bruker4 = new OppfolgingsBruker()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setVeileder_id(veilederId.toString())
                .setNy_for_veileder(false)
                .setEnhet_id(navKontor.toString());

        var liste = List.of(bruker1, bruker2, bruker3, bruker4);

        skrivBrukereTilTestindeks(liste);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == liste.size());


        Filtervalg filterValg = new Filtervalg()
                .setFerdigfilterListe(List.of())
                .setEnsligeForsorgere(List.of(OVERGANGSSTONAD));

        BrukereMedAntall response = opensearchService.hentBrukere(
                navKontor.toString(),
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                filterValg,
                null,
                null
        );

        Assertions.assertThat(response.getAntall()).isEqualTo(2);
        Assertions.assertThat(response.getBrukere().get(0).getFnr().equals(bruker2.getFnr()));
        Assertions.assertThat(response.getBrukere().get(1).getFnr().equals(bruker3.getFnr()));
    }

    @Test
    public void testHentingAvAlleBrukere() {
        List<Fnr> fnrList = List.of(Fnr.of("11018012321"), Fnr.of("12018012321"), Fnr.of("13018012321"),
                Fnr.of("14018012321"), Fnr.of("15018012321"));
        List<Long> vedtakIds = List.of(new Random().nextLong(10000l), new Random().nextLong(10000l), new Random().nextLong(10000l),
                new Random().nextLong(10000l), new Random().nextLong(10000l));

        Mockito.when(aktorClient.hentAktorId(fnrList.get(0))).thenReturn(randomAktorId());
        Mockito.when(aktorClient.hentAktorId(fnrList.get(1))).thenReturn(randomAktorId());
        Mockito.when(aktorClient.hentAktorId(fnrList.get(2))).thenReturn(randomAktorId());
        Mockito.when(aktorClient.hentAktorId(fnrList.get(3))).thenReturn(randomAktorId());
        Mockito.when(aktorClient.hentAktorId(fnrList.get(4))).thenReturn(randomAktorId());

        lagreRandomVedtakIdatabase(vedtakIds.get(0), fnrList.get(0), LocalDate.now().minusDays(4), LocalDate.now().plusMonths(1));
        lagreRandomVedtakIdatabase(vedtakIds.get(1), fnrList.get(1), LocalDate.now().minusDays(10), LocalDate.now().plusMonths(1));
        lagreRandomVedtakIdatabase(vedtakIds.get(2), fnrList.get(2), LocalDate.now().minusDays(20), LocalDate.now().plusMonths(1));
        lagreRandomVedtakIdatabase(vedtakIds.get(3), fnrList.get(3), LocalDate.now().minusDays(30), LocalDate.now().plusMonths(1));
        lagreRandomVedtakIdatabase(vedtakIds.get(4), fnrList.get(4), LocalDate.now().minusDays(13), LocalDate.now().plusMonths(1));

        Map<Fnr, EnsligeForsorgerOvergangsstønadTiltakDto> fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap = ensligeForsorgereService.hentEnsligeForsorgerOvergangsstønadTiltak(fnrList);
        Assert.assertEquals(fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap.size(), 5);
        Assert.assertTrue(fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap.containsKey(fnrList.get(0)));
        Assert.assertTrue(fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap.containsKey(fnrList.get(1)));
        Assert.assertTrue(fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap.containsKey(fnrList.get(2)));
        Assert.assertTrue(fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap.containsKey(fnrList.get(3)));
        Assert.assertTrue(fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap.containsKey(fnrList.get(4)));
    }

    @Test
    public void testHentingAvBrukerMedFlereTiltak() {
        Fnr fnr = Fnr.of("11018012321");
        List<Long> vedtakIds = List.of(
                new Random().nextLong(10000l),
                new Random().nextLong(10000l),
                new Random().nextLong(10000l));

        Mockito.when(aktorClient.hentAktorId(fnr)).thenReturn(randomAktorId());

        lagreRandomVedtakIdatabase(vedtakIds.get(0), fnr, LocalDate.now().minusDays(4), LocalDate.now().plusMonths(2));
        lagreRandomVedtakIdatabase(vedtakIds.get(1), fnr, LocalDate.now().minusDays(10), LocalDate.now().plusMonths(1));
        lagreRandomVedtakIdatabase(vedtakIds.get(2), fnr, LocalDate.now().plusDays(20), LocalDate.now().plusMonths(1));

        Map<Fnr, EnsligeForsorgerOvergangsstønadTiltakDto> fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap = ensligeForsorgereService.hentEnsligeForsorgerOvergangsstønadTiltak(List.of(fnr));
        Assert.assertEquals(fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap.size(), 1);
        Assert.assertTrue(fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap.containsKey(fnr));
        Assert.assertTrue(fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap.get(fnr).utløpsDato().equals(LocalDate.now().plusMonths(1)));
    }

    @Test
    public void testHentingAvAlleBrukereMedAktiveEllerKommendeVedtakPeriode() {
        List<Fnr> fnrList = List.of(Fnr.of("11018012321"),
                Fnr.of("12018012321"),
                Fnr.of("13018012321"),
                Fnr.of("14018012321"),
                Fnr.of("15018012321"),
                Fnr.of("18048012321"));
        List<Long> vedtakIds = List.of(new Random().nextLong(10000l),
                new Random().nextLong(10000l),
                new Random().nextLong(10000l),
                new Random().nextLong(10000l),
                new Random().nextLong(10000l),
                new Random().nextLong(10000l));

        Mockito.when(aktorClient.hentAktorId(fnrList.get(0))).thenReturn(randomAktorId());
        Mockito.when(aktorClient.hentAktorId(fnrList.get(1))).thenReturn(randomAktorId());
        Mockito.when(aktorClient.hentAktorId(fnrList.get(2))).thenReturn(randomAktorId());
        Mockito.when(aktorClient.hentAktorId(fnrList.get(3))).thenReturn(randomAktorId());
        Mockito.when(aktorClient.hentAktorId(fnrList.get(4))).thenReturn(randomAktorId());
        Mockito.when(aktorClient.hentAktorId(fnrList.get(5))).thenReturn(randomAktorId());

        lagreRandomVedtakIdatabase(vedtakIds.get(0), fnrList.get(0), LocalDate.now().plusMonths(4), LocalDate.now().plusMonths(10));
        lagreRandomVedtakIdatabase(vedtakIds.get(1), fnrList.get(1), LocalDate.now().plusDays(3), LocalDate.now().plusMonths(1));
        lagreRandomVedtakIdatabase(vedtakIds.get(2), fnrList.get(2), LocalDate.now().minusDays(20), LocalDate.now().plusMonths(1));
        lagreRandomVedtakIdatabase(vedtakIds.get(3), fnrList.get(3), LocalDate.now().minusDays(30), LocalDate.now().minusDays(2));
        lagreRandomVedtakIdatabase(vedtakIds.get(4), fnrList.get(4), LocalDate.now().minusDays(13), LocalDate.now().plusMonths(1));
        lagreRandomVedtakIdatabase(vedtakIds.get(5), fnrList.get(5), LocalDate.now().plusMonths(7), LocalDate.now().plusMonths(10));

        Map<Fnr, EnsligeForsorgerOvergangsstønadTiltakDto> fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap = ensligeForsorgereService.hentEnsligeForsorgerOvergangsstønadTiltak(fnrList);
        Assert.assertEquals(fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap.size(), 4);
        Assert.assertTrue(fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap.containsKey(fnrList.get(0)));
        Assert.assertTrue(fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap.containsKey(fnrList.get(1)));
        Assert.assertTrue(fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap.containsKey(fnrList.get(2)));
        Assert.assertTrue(fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap.containsKey(fnrList.get(4)));
    }

    @Test
    public void testVedtakOppdatering() {
        Long vedtakId = new Random().nextLong(10000l);
        Fnr fnr = Fnr.of("11018012321");
        Mockito.when(aktorClient.hentAktorId(fnr)).thenReturn(randomAktorId());
        lagreRandomVedtakIdatabase(vedtakId, fnr, LocalDate.now().minusDays(10), LocalDate.now().plusDays(20));

        Optional<EnsligeForsorgerOvergangsstønadTiltakDto> ensligeForsorgerOvergangsstønadTiltakDto = ensligeForsorgereService.hentEnsligeForsorgerOvergangsstønadTiltak(fnr.get());
        Assert.assertTrue(ensligeForsorgerOvergangsstønadTiltakDto.isPresent());
        Assert.assertTrue(ensligeForsorgerOvergangsstønadTiltakDto.get().utløpsDato().equals(LocalDate.now().plusDays(20)));

        lagreRandomVedtakIdatabase(vedtakId, fnr, LocalDate.now().minusDays(3), LocalDate.now().plusDays(80));
        ensligeForsorgerOvergangsstønadTiltakDto = ensligeForsorgereService.hentEnsligeForsorgerOvergangsstønadTiltak(fnr.get());
        Assert.assertTrue(ensligeForsorgerOvergangsstønadTiltakDto.isPresent());
        Assert.assertTrue(ensligeForsorgerOvergangsstønadTiltakDto.get().utløpsDato().equals(LocalDate.now().plusDays(80)));
    }

    private void lagreRandomVedtakIdatabase(Long vedtakId, Fnr fnr, LocalDate vedtakPeriodeFra, LocalDate vedtakPeriodeTil) {
        List<Barn> barn = List.of(new Barn(randomFnr().toString(), null));
        List<Periode> periodeType = List.of(new Periode(vedtakPeriodeFra, vedtakPeriodeTil, NY_PERIODE_FOR_NYTT_BARN, Aktivitetstype.BARN_UNDER_ETT_ÅR));
        ensligeForsorgereService.behandleKafkaMeldingLogikk(
                new VedtakOvergangsstønadArbeidsoppfølging(
                        vedtakId,
                        fnr.toString(),
                        barn,
                        Stønadstype.OVERGANGSSTØNAD,
                        periodeType,
                        Vedtaksresultat.INNVILGET

                )
        );
    }

    private void setInitialState() {
        Fnr bruker1_fnr = hoved_fnr;
        Fnr bruker2_fnr = Fnr.of("2911883838");
        Fnr bruker3_fnr = Fnr.of("0304299922");
        AktorId bruker1_aktorId = AktorId.of("9938");
        AktorId bruker2_aktorId = AktorId.of("3939");
        AktorId bruker3_aktorId = AktorId.of("23112");

        Mockito.when(aktorClient.hentAktorId(bruker1_fnr)).thenReturn(bruker1_aktorId);
        Mockito.when(aktorClient.hentAktorId(bruker2_fnr)).thenReturn(bruker2_aktorId);
        Mockito.when(aktorClient.hentAktorId(bruker3_fnr)).thenReturn(bruker3_aktorId);

        insertOppfolgingsInformasjon(bruker1_aktorId, bruker1_fnr);
        insertOppfolgingsInformasjon(bruker2_aktorId, bruker2_fnr);
        insertOppfolgingsInformasjon(bruker3_aktorId, bruker3_fnr);
        populateOpensearch(navKontor, veilederId, bruker1_aktorId.get(), bruker2_aktorId.get(), bruker3_aktorId.get());

        List<Barn> barn = List.of(new Barn("11032245678", null), new Barn(null, LocalDate.of(2023, 5, 4)));
        List<Periode> periodeType = List.of(new Periode(LocalDate.now().minusDays(10), LocalDate.now().plusDays(20), Periodetype.NY_PERIODE_FOR_NYTT_BARN, Aktivitetstype.BARN_UNDER_ETT_ÅR));
        ensligeForsorgereService.behandleKafkaMeldingLogikk(
                new VedtakOvergangsstønadArbeidsoppfølging(
                        54321L,
                        bruker1_fnr.toString(),
                        barn,
                        Stønadstype.OVERGANGSSTØNAD,
                        periodeType,
                        Vedtaksresultat.INNVILGET
                )
        );
    }

    private void skrivBrukereTilTestindeks(List<OppfolgingsBruker> brukere) {
        OppfolgingsBruker[] array = new OppfolgingsBruker[brukere.size()];
        skrivBrukereTilTestindeks(brukere.toArray(array));
    }

    @SneakyThrows
    private void skrivBrukereTilTestindeks(OppfolgingsBruker... brukere) {
        opensearchIndexer.skrivBulkTilIndeks(indexName.getValue(), List.of(brukere));
    }

    @Test
    public void testHentOgLagreEnsligForsorgerDataFraApi() {
        Fnr fnr = Fnr.of("12518904661");
        AktorId aktorId = AktorId.of("9938");
        String ensligForsorgerJson = readTestResourceFile("ensligForsorgerApiData.json");
        Optional<OvergangsstønadResponseDto> expected = Optional.of(fromJson(ensligForsorgerJson, OvergangsstønadResponseDto.class));
        Mockito.when(ensligForsorgerClient.hentEnsligForsorgerOvergangsstonad(fnr)).thenReturn(expected);
        Mockito.when(aktorClient.hentFnr(aktorId)).thenReturn(fnr);
        ensligeForsorgereService.hentOgLagreEnsligForsorgerDataFraApi(aktorId);
        String vedtakid = postgres.queryForObject("select vedtakid from enslige_forsorgere where personident = ?", (rs, row) -> {return rs.getString("vedtakid");}, fnr.get());
        assertThat(vedtakid).isNotNull();
        assertThat(vedtakid).isEqualTo("20532");
    }
}
