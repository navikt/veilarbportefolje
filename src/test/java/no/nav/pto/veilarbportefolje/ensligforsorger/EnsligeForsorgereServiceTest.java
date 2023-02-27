package no.nav.pto.veilarbportefolje.ensligforsorger;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.familie.eksterne.kontrakter.arbeidsoppfolging.*;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.empty;
import static no.nav.pto.veilarbportefolje.domene.EnsligeForsorgere.OVERGANGSSTØNAD;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class EnsligeForsorgereServiceTest extends EndToEndTest {
    private static NavKontor navKontor = NavKontor.of("1123");
    private static VeilederId veilederId = VeilederId.of("1402");

    @Autowired
    private OpensearchService opensearchService;

    @Autowired
    private EnsligeForsorgereService ensligeForsorgereService;
    @Autowired
    private AktorClient aktorClient;

    @Test
    public void testNyOvergangsstonadForBrukerIndex() {
        Fnr bruker1_fnr = Fnr.of("2449920301");
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
        List<Periode> periodeType = List.of(new Periode(LocalDate.of(2023, 4, 4), LocalDate.of(2024, 4, 4), no.nav.familie.eksterne.kontrakter.arbeidsoppfolging.Periodetype.NY_PERIODE_FOR_NYTT_BARN, Aktivitetstype.BARN_UNDER_ETT_ÅR));
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

        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setEnsligeForsorgere(List.of(OVERGANGSSTØNAD));
        filtervalg.setFerdigfilterListe(List.of());

        verifiserAsynkront(2, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall responseBrukere = opensearchService.hentBrukere(
                            navKontor.toString(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            filtervalg,
                            null,
                            null);

                    assertThat(responseBrukere.getAntall()).isEqualTo(1);
                    assertThat(responseBrukere.getBrukere().get(0).getEnsligeForsorgereOvergangsstonad().vedtaksPeriodetype()).isEqualTo("NY_PERIODE_FOR_NYTT_BARN");
                    assertThat(responseBrukere.getBrukere().get(0).getEnsligeForsorgereOvergangsstonad().aktivitetsType()).isEqualTo("BARN_UNDER_ETT_ÅR");
                    assertThat(responseBrukere.getBrukere().get(0).getEnsligeForsorgereOvergangsstonad().til_dato()).isEqualTo(LocalDate.of(2024, 4, 4));
                    assertThat(responseBrukere.getBrukere().get(0).getEnsligeForsorgereOvergangsstonad().yngsteBarnsFødselsdato()).isEqualTo(LocalDate.of(2023, 5, 4));
                }
        );


    }

    @Test
    public void testAvsluttetOvergangsstonadForBrukerIndex() {
        Fnr bruker1_fnr = Fnr.of("2449920301");
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
        List<Periode> periodeType = List.of(new Periode(LocalDate.of(2023, 4, 4), LocalDate.of(2024, 4, 4), no.nav.familie.eksterne.kontrakter.arbeidsoppfolging.Periodetype.NY_PERIODE_FOR_NYTT_BARN, Aktivitetstype.BARN_UNDER_ETT_ÅR));
        ensligeForsorgereService.behandleKafkaMeldingLogikk(
                new VedtakOvergangsstønadArbeidsoppfølging(
                        54321L,
                        bruker1_fnr.toString(),
                        barn,
                        Stønadstype.OVERGANGSSTØNAD,
                        periodeType,
                        Vedtaksresultat.OPPHØRT
                )
        );

        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setEnsligeForsorgere(List.of(OVERGANGSSTØNAD));
        filtervalg.setFerdigfilterListe(List.of());

        verifiserAsynkront(2, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall responseBrukere = opensearchService.hentBrukere(
                            navKontor.toString(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            filtervalg,
                            null,
                            null);

                    assertThat(responseBrukere.getAntall()).isEqualTo(0);
                }
        );
    }
}