package no.nav.fo.provider.rest;

import no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler;
import no.nav.fo.domene.*;
import no.nav.fo.service.BrukertilgangService;
import no.nav.fo.service.SolrService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
            new Bruker().setFnr("1").setUtlopsdatoFasett(MND1).setDagputlopUkeFasett(UKE_UNDER2).setAapmaxtidUkeFasett(UKE_UNDER12),
            new Bruker().setFnr("2").setUtlopsdatoFasett(MND2).setDagputlopUkeFasett(UKE2_5).setAapmaxtidUkeFasett(UKE12_23),
            new Bruker().setFnr("3").setUtlopsdatoFasett(MND3).setDagputlopUkeFasett(UKE6_9).setAapmaxtidUkeFasett(UKE24_35),
            new Bruker().setFnr("4").setUtlopsdatoFasett(MND4).setDagputlopUkeFasett(UKE10_13).setAapmaxtidUkeFasett(UKE36_47),
            new Bruker().setFnr("5").setUtlopsdatoFasett(MND5).setDagputlopUkeFasett(UKE14_17).setAapmaxtidUkeFasett(UKE48_59),
            new Bruker().setFnr("6").setUtlopsdatoFasett(MND6).setDagputlopUkeFasett(UKE18_21).setAapmaxtidUkeFasett(UKE60_71),
            new Bruker().setFnr("7").setUtlopsdatoFasett(MND7).setDagputlopUkeFasett(UKE22_25).setAapmaxtidUkeFasett(UKE72_83),
            new Bruker().setFnr("8").setUtlopsdatoFasett(MND8).setDagputlopUkeFasett(UKE26_29).setAapmaxtidUkeFasett(UKE84_95),
            new Bruker().setFnr("9").setUtlopsdatoFasett(MND9).setDagputlopUkeFasett(UKE30_33).setAapmaxtidUkeFasett(UKE96_107),
            new Bruker().setFnr("10").setUtlopsdatoFasett(MND10).setDagputlopUkeFasett(UKE34_37).setAapmaxtidUkeFasett(UKE108_119),
            new Bruker().setFnr("11").setUtlopsdatoFasett(MND11).setDagputlopUkeFasett(UKE38_41).setAapmaxtidUkeFasett(UKE120_131),
            new Bruker().setFnr("12").setUtlopsdatoFasett(MND12).setDagputlopUkeFasett(UKE42_45).setAapmaxtidUkeFasett(UKE132_143),
            new Bruker().setFnr("13").setUtlopsdatoFasett(MND12).setDagputlopUkeFasett(UKE46_49).setAapmaxtidUkeFasett(UKE144_155),
            new Bruker().setFnr("14").setUtlopsdatoFasett(MND12).setDagputlopUkeFasett(UKE50_52).setAapmaxtidUkeFasett(UKE156_167),
            new Bruker().setFnr("15").setUtlopsdatoFasett(MND12).setDagputlopUkeFasett(UKE50_52).setAapmaxtidUkeFasett(UKE168_179),
            new Bruker().setFnr("16").setUtlopsdatoFasett(MND12).setDagputlopUkeFasett(UKE50_52).setAapmaxtidUkeFasett(UKE180_191),
            new Bruker().setFnr("17").setUtlopsdatoFasett(MND12).setDagputlopUkeFasett(UKE50_52).setAapmaxtidUkeFasett(UKE192_203),
            new Bruker().setFnr("18").setUtlopsdatoFasett(MND12).setDagputlopUkeFasett(UKE50_52).setAapmaxtidUkeFasett(UKE204_215)
    );
    @Mock
    private SolrService solr;

    @Mock
    private BrukertilgangService brukertilgangService;

    private DiagramController controller;

    @BeforeClass
    public static void before() {
        System.setProperty("disable.metrics.report", "true");
        System.setProperty("no.nav.brukerdialog.security.context.subjectHandlerImplementationClass", ThreadLocalSubjectHandler.class.getName());
    }

    @Before
    public void setUp() throws Exception {
        controller = new DiagramController(brukertilgangService, solr);
        when(solr.hentBrukere(anyString(), any(), anyString(), anyString(), any(Filtervalg.class))).thenReturn(BRUKERE);
    }

    @Test
    public void skalSjekkeTilgang() throws Exception {
        when(brukertilgangService.harBrukerTilgang(anyString(), anyString())).thenReturn(false);

        Response response = controller.hentDiagramData("Z999000", "0100", new Filtervalg().setYtelse(YtelseFilter.DAGPENGER));

        assertThat(response.getStatus()).isEqualTo(FORBIDDEN.getStatusCode());
    }

    @Test
    public void skalGiFeilOmYtelseIkkeErValgt() throws Exception {
        when(brukertilgangService.harBrukerTilgang(anyString(), anyString())).thenReturn(true);

        Response response = controller.hentDiagramData("Z999000", "0100", new Filtervalg());

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void skalHenteUtBrukereForEnhet() throws Exception {
        when(brukertilgangService.harBrukerTilgang(anyString(), anyString())).thenReturn(true);

        controller.hentDiagramData(null, "0100", new Filtervalg().setYtelse(YtelseFilter.DAGPENGER));

        verify(solr, times(1)).hentBrukere(anyString(), any(), anyString(), anyString(), any(Filtervalg.class));
    }

    @Test
    public void skalHenteUtBrukereForVeileder() throws Exception {
        when(brukertilgangService.harBrukerTilgang(anyString(), anyString())).thenReturn(true);

        controller.hentDiagramData("Z999000", "0100", new Filtervalg().setYtelse(YtelseFilter.DAGPENGER));

        verify(solr, times(1)).hentBrukere(anyString(), any(), anyString(), anyString(), any(Filtervalg.class));
    }

    @Test
    public void skalGrupperePaUtlopsdato() throws Exception {
        List<Bruker> brukere = new ArrayList<>(BRUKERE);
        brukere.add(BRUKERE.get(3));

        when(brukertilgangService.harBrukerTilgang(anyString(), anyString())).thenReturn(true);
        when(solr.hentBrukere(anyString(), any(), anyString(), anyString(), any(Filtervalg.class))).thenReturn(brukere);

        Response response = controller.hentDiagramData("Z999000", "0100", new Filtervalg().setYtelse(YtelseFilter.DAGPENGER));

        Map<FasettMapping, Long> gruppering = (Map<FasettMapping, Long>) response.getEntity();
        Optional<Long> storsteGruppe = gruppering.values().stream().max(Long::compare);

        assertThat(gruppering).hasSize(14);
        assertThat(storsteGruppe).contains(5L);
        assertThat(gruppering.get(UKE10_13)).isEqualTo(2L);
        assertThat(gruppering.get(UKE50_52)).isEqualTo(5L);
    }

    @Test
    public void skalGrupperePaAApUtlopsdato() throws Exception {
        List<Bruker> brukere = new ArrayList<>(BRUKERE);
        brukere.add(BRUKERE.get(3));

        when(brukertilgangService.harBrukerTilgang(anyString(), anyString())).thenReturn(true);
        when(solr.hentBrukere(anyString(), any(), anyString(), anyString(), any(Filtervalg.class))).thenReturn(brukere);

        Response response = controller.hentDiagramData("Z999000", "0100", new Filtervalg().setYtelse(YtelseFilter.AAP_MAXTID));

        Map<FasettMapping, Long> gruppering = (Map<FasettMapping, Long>) response.getEntity();
        Optional<Long> storsteGruppe = gruppering.values().stream().max(Long::compare);

        assertThat(gruppering).hasSize(18);
        assertThat(storsteGruppe).contains(2L);
        assertThat(gruppering.get(UKE36_47)).isEqualTo(2L);
    }

}