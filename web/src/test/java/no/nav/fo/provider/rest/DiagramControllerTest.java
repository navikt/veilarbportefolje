package no.nav.fo.provider.rest;

import no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler;
import no.nav.fo.domene.*;
import no.nav.fo.service.PepClient;
import no.nav.fo.service.SolrService;
import no.nav.fo.util.StepperUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static no.nav.fo.domene.AAPMaxtidUkeFasettMapping.*;
import static no.nav.fo.domene.DagpengerUkeFasettMapping.*;
import static no.nav.fo.domene.ManedFasettMapping.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class DiagramControllerTest {

    private static final List<Bruker> BRUKERE = asList(
            new Bruker().setFnr("1").setUtlopsdatoFasett(MND1).setDagputlopUke(0).setDagputlopUkeFasett(UKE_UNDER2).setAapmaxtidUke(0).setAapmaxtidUkeFasett(UKE_UNDER12),
            new Bruker().setFnr("2").setUtlopsdatoFasett(MND2).setDagputlopUke(2).setDagputlopUkeFasett(UKE2_5).setAapmaxtidUke(13).setAapmaxtidUkeFasett(UKE12_23),
            new Bruker().setFnr("3").setUtlopsdatoFasett(MND3).setDagputlopUke(8).setDagputlopUkeFasett(UKE6_9).setAapmaxtidUke(30).setAapmaxtidUkeFasett(UKE24_35),
            new Bruker().setFnr("4").setUtlopsdatoFasett(MND4).setDagputlopUke(13).setDagputlopUkeFasett(UKE10_13).setAapmaxtidUke(40).setAapmaxtidUkeFasett(UKE36_47),
            new Bruker().setFnr("5").setUtlopsdatoFasett(MND5).setDagputlopUke(14).setDagputlopUkeFasett(UKE14_17).setAapmaxtidUke(50).setAapmaxtidUkeFasett(UKE48_59),
            new Bruker().setFnr("6").setUtlopsdatoFasett(MND6).setDagputlopUke(19).setDagputlopUkeFasett(UKE18_21).setAapmaxtidUke(70).setAapmaxtidUkeFasett(UKE60_71),
            new Bruker().setFnr("7").setUtlopsdatoFasett(MND7).setDagputlopUke(22).setDagputlopUkeFasett(UKE22_25).setAapmaxtidUke(80).setAapmaxtidUkeFasett(UKE72_83),
            new Bruker().setFnr("8").setUtlopsdatoFasett(MND8).setDagputlopUke(27).setDagputlopUkeFasett(UKE26_29).setAapmaxtidUke(90).setAapmaxtidUkeFasett(UKE84_95),
            new Bruker().setFnr("9").setUtlopsdatoFasett(MND9).setDagputlopUke(30).setDagputlopUkeFasett(UKE30_33).setAapmaxtidUke(100).setAapmaxtidUkeFasett(UKE96_107),
            new Bruker().setFnr("10").setUtlopsdatoFasett(MND10).setDagputlopUke(37).setDagputlopUkeFasett(UKE34_37).setAapmaxtidUke(110).setAapmaxtidUkeFasett(UKE108_119),
            new Bruker().setFnr("11").setUtlopsdatoFasett(MND11).setDagputlopUke(38).setDagputlopUkeFasett(UKE38_41).setAapmaxtidUke(131).setAapmaxtidUkeFasett(UKE120_131),
            new Bruker().setFnr("12").setUtlopsdatoFasett(MND12).setDagputlopUke(42).setDagputlopUkeFasett(UKE42_45).setAapmaxtidUke(140).setAapmaxtidUkeFasett(UKE132_143),
            new Bruker().setFnr("13").setUtlopsdatoFasett(MND12).setDagputlopUke(46).setDagputlopUkeFasett(UKE46_49).setAapmaxtidUke(150).setAapmaxtidUkeFasett(UKE144_155),
            new Bruker().setFnr("14").setUtlopsdatoFasett(MND12).setDagputlopUke(52).setDagputlopUkeFasett(UKE50_52).setAapmaxtidUke(156).setAapmaxtidUkeFasett(UKE156_167),
            new Bruker().setFnr("15").setUtlopsdatoFasett(MND12).setDagputlopUke(51).setDagputlopUkeFasett(UKE50_52).setAapmaxtidUke(168).setAapmaxtidUkeFasett(UKE168_179),
            new Bruker().setFnr("16").setUtlopsdatoFasett(MND12).setDagputlopUke(50).setDagputlopUkeFasett(UKE50_52).setAapmaxtidUke(180).setAapmaxtidUkeFasett(UKE180_191),
            new Bruker().setFnr("17").setUtlopsdatoFasett(MND12).setDagputlopUke(51).setDagputlopUkeFasett(UKE50_52).setAapmaxtidUke(195).setAapmaxtidUkeFasett(UKE192_203),
            new Bruker().setFnr("18").setUtlopsdatoFasett(MND12).setDagputlopUke(52).setDagputlopUkeFasett(UKE50_52).setAapmaxtidUke(210).setAapmaxtidUkeFasett(UKE204_215)
    );

    private static final List<Bruker> BRUKERE_AAP_UNNTAK = asList(
            new Bruker().setFnr("1").setUtlopsdatoFasett(MND1).setDagputlopUkeFasett(UKE_UNDER2).setAapUnntakUkerIgjen(25).setAapUnntakUkerIgjenFasett(AAPUnntakUkerIgjenFasettMapping.UKE24_35),
            new Bruker().setFnr("2").setUtlopsdatoFasett(MND2).setDagputlopUkeFasett(UKE2_5).setAapUnntakUkerIgjen(40).setAapUnntakUkerIgjenFasett(AAPUnntakUkerIgjenFasettMapping.UKE36_47),
            new Bruker().setFnr("3").setUtlopsdatoFasett(MND3).setDagputlopUkeFasett(UKE6_9).setAapUnntakUkerIgjen(40).setAapUnntakUkerIgjenFasett(AAPUnntakUkerIgjenFasettMapping.UKE36_47)
    );

    @Mock
    private SolrService solr;

    @Mock
    private PepClient pepClient;

    private DiagramController controller;

    @BeforeClass
    public static void before() {
        System.setProperty("disable.metrics.report", "true");
        System.setProperty("no.nav.brukerdialog.security.context.subjectHandlerImplementationClass", ThreadLocalSubjectHandler.class.getName());

    }

    @Before
    public void setUp() throws Exception {
        controller = new DiagramController(pepClient, solr);
        when(pepClient.tilgangTilEnhet(any(), any())).thenReturn(true);
        when(solr.hentBrukere(anyString(), any(), any(), any(), any(Filtervalg.class))).thenReturn(new BrukereMedAntall(BRUKERE.size(),BRUKERE));
    }

    @Test
    public void skalSjekkeTilgang() throws Exception {
        when(pepClient.tilgangTilEnhet(any(),any())).thenReturn(false);
        Response response = controller.hentDiagramData("Z999000", "0100", new Filtervalg().setYtelse(YtelseFilter.DAGPENGER));
        assertThat(response.getStatus()).isEqualTo(FORBIDDEN.getStatusCode());
    }

    @Test
    public void skalGiFeilOmYtelseIkkeErValgt() throws Exception {
        Response response = controller.hentDiagramData("Z999000", "0100", new Filtervalg());
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void skalHenteUtBrukereForEnhet() throws Exception {
        controller.hentDiagramData(null, "0100", new Filtervalg().setYtelse(YtelseFilter.DAGPENGER));
        verify(solr, times(1)).hentBrukere(anyString(), any(), any(), any(), any(Filtervalg.class));
    }

    @Test
    public void skalHenteUtBrukereForVeileder() throws Exception {
        controller.hentDiagramData("Z999000", "0100", new Filtervalg().setYtelse(YtelseFilter.DAGPENGER));
        verify(solr, times(1)).hentBrukere(anyString(), any(), any(), any(), any(Filtervalg.class));
    }

    @Test
    public void skalGrupperePaUtlopsdatoV2() throws Exception {
        List<Bruker> brukere = new ArrayList<>(BRUKERE);
        brukere.add(BRUKERE.get(3));

        when(solr.hentBrukere(anyString(), any(), any(), any(), any(Filtervalg.class))).thenReturn(new BrukereMedAntall(brukere.size(),brukere));

        Response response = controller.hentDiagramData("Z999000", "0100", new Filtervalg().setYtelse(YtelseFilter.DAGPENGER));

        List<StepperUtils.Step> gruppering = (List<StepperUtils.Step>) response.getEntity();
        Optional<Long> storsteGruppe = gruppering.stream().map(StepperUtils.Step::getVerdi).max(Long::compare);
        Optional<StepperUtils.Step> uke10_13_verdi = gruppering.stream().filter((step) -> step.getFra() == 10 && step.getTil() == 13).findFirst();
        Optional<StepperUtils.Step> uke50_52_verdi = gruppering.stream().filter((step) -> step.getFra() == 50 && step.getTil() == 52).findFirst();

        assertThat(gruppering).hasSize(14);
        assertThat(storsteGruppe).contains(5L);
        assertThat(uke10_13_verdi.isPresent()).isTrue();
        assertThat(uke50_52_verdi.isPresent()).isTrue();
        assertThat(uke10_13_verdi.get().getVerdi()).isEqualTo(2L);
        assertThat(uke50_52_verdi.get().getVerdi()).isEqualTo(5L);
    }

    @Test
    public void skalGrupperePaAApUtlopsdatoV2() throws Exception {
        List<Bruker> brukere = new ArrayList<>(BRUKERE);
        brukere.add(BRUKERE.get(3));

        when(solr.hentBrukere(anyString(), any(), any(), any(), any(Filtervalg.class))).thenReturn(new BrukereMedAntall(brukere.size(),brukere));

        Response response = controller.hentDiagramData("Z999000", "0100", new Filtervalg().setYtelse(YtelseFilter.AAP_MAXTID));

        List<StepperUtils.Step> gruppering = (List<StepperUtils.Step>) response.getEntity();
        Optional<Long> storsteGruppe = gruppering.stream().map(StepperUtils.Step::getVerdi).max(Long::compare);
        Optional<StepperUtils.Step> uke38_46_verdi = gruppering.stream().filter((step) -> step.getFra() == 38 && step.getTil() == 46).findFirst();

        assertThat(gruppering).hasSize(25);
        assertThat(storsteGruppe).contains(2L);
        assertThat(uke38_46_verdi.isPresent()).isTrue();
        assertThat(uke38_46_verdi.get().getVerdi()).isEqualTo(2L);
    }

    @Test
    public void skalGrupperePaAAPUnntakUtlopsdatoV2() throws Exception {
        List<Bruker> brukere = new ArrayList<>(BRUKERE_AAP_UNNTAK);

        when(solr.hentBrukere(anyString(), any(), any(), any(), any(Filtervalg.class))).thenReturn(new BrukereMedAntall(brukere.size(), brukere));

        Response response = controller.hentDiagramData("Z999000", "0100", new Filtervalg().setYtelse(YtelseFilter.AAP_UNNTAK));

        List<StepperUtils.Step> gruppering = (List<StepperUtils.Step>) response.getEntity();
        Optional<Long> storsteGruppe = gruppering.stream().map(StepperUtils.Step::getVerdi).max(Long::compare);
        Optional<StepperUtils.Step> uke37_41_verdi = gruppering.stream().filter((step) -> step.getFra() == 37 && step.getTil() == 41).findFirst();

        assertThat(gruppering).hasSize(22);
        assertThat(storsteGruppe).contains(2L);
        assertThat(uke37_41_verdi.get().getVerdi()).isEqualTo(2L);
    }
}