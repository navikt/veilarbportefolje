package no.nav.fo.provider.rest;

import no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler;
import no.nav.fo.domene.Bruker;
import no.nav.fo.domene.Filtervalg;
import no.nav.fo.domene.Mapping;
import no.nav.fo.domene.YtelseFilter;
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
import static no.nav.fo.domene.KvartalMapping.*;
import static no.nav.fo.domene.ManedMapping.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class DiagramControllerTest {

    private static final List<Bruker> BRUKERE = asList(
            new Bruker().setFnr("1").setUtlopsdatoFasett(MND1).setAapMaxtidFasett(KV1),
            new Bruker().setFnr("2").setUtlopsdatoFasett(MND2).setAapMaxtidFasett(KV2),
            new Bruker().setFnr("3").setUtlopsdatoFasett(MND3).setAapMaxtidFasett(KV3),
            new Bruker().setFnr("4").setUtlopsdatoFasett(MND4).setAapMaxtidFasett(KV4),
            new Bruker().setFnr("5").setUtlopsdatoFasett(MND5).setAapMaxtidFasett(KV5),
            new Bruker().setFnr("6").setUtlopsdatoFasett(MND6).setAapMaxtidFasett(KV6),
            new Bruker().setFnr("7").setUtlopsdatoFasett(MND7).setAapMaxtidFasett(KV7),
            new Bruker().setFnr("8").setUtlopsdatoFasett(MND8).setAapMaxtidFasett(KV8),
            new Bruker().setFnr("9").setUtlopsdatoFasett(MND9).setAapMaxtidFasett(KV9),
            new Bruker().setFnr("10").setUtlopsdatoFasett(MND10).setAapMaxtidFasett(KV10),
            new Bruker().setFnr("11").setUtlopsdatoFasett(MND11).setAapMaxtidFasett(KV11),
            new Bruker().setFnr("12").setUtlopsdatoFasett(MND12).setAapMaxtidFasett(KV12),
            new Bruker().setFnr("13").setUtlopsdatoFasett(MND12).setAapMaxtidFasett(KV13),
            new Bruker().setFnr("14").setUtlopsdatoFasett(MND12).setAapMaxtidFasett(KV14),
            new Bruker().setFnr("15").setUtlopsdatoFasett(MND12).setAapMaxtidFasett(KV15),
            new Bruker().setFnr("16").setUtlopsdatoFasett(MND12).setAapMaxtidFasett(KV16)
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
        when(brukertilgangService.harBrukerTilgang(any(), any())).thenReturn(true);
        when(solr.hentBrukere(anyString(), any(), any(), any(), any(Filtervalg.class))).thenReturn(BRUKERE);
    }

    @Test
    public void skalSjekkeTilgang() throws Exception {
        when(brukertilgangService.harBrukerTilgang(any(),any())).thenReturn(false);
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
    public void skalGrupperePaUtlopsdato() throws Exception {
        List<Bruker> brukere = new ArrayList<>(BRUKERE);
        brukere.add(BRUKERE.get(3));

        when(solr.hentBrukere(anyString(), any(), any(), any(), any(Filtervalg.class))).thenReturn(brukere);

        Response response = controller.hentDiagramData("Z999000", "0100", new Filtervalg().setYtelse(YtelseFilter.DAGPENGER));

        Map<Mapping, Long> gruppering = (Map<Mapping, Long>) response.getEntity();
        Optional<Long> storsteGruppe = gruppering.values().stream().max(Long::compare);

        assertThat(gruppering).hasSize(12);
        assertThat(storsteGruppe).contains(5L);
        assertThat(gruppering.get(MND4)).isEqualTo(2L);
        assertThat(gruppering.get(MND12)).isEqualTo(5L);
    }

    @Test
    public void skalGrupperePaAApUtlopsdato() throws Exception {
        List<Bruker> brukere = new ArrayList<>(BRUKERE);
        brukere.add(BRUKERE.get(3));

        when(solr.hentBrukere(anyString(), any(), any(), any(), any(Filtervalg.class))).thenReturn(brukere);

        Response response = controller.hentDiagramData("Z999000", "0100", new Filtervalg().setYtelse(YtelseFilter.AAP_MAXTID));

        Map<Mapping, Long> gruppering = (Map<Mapping, Long>) response.getEntity();
        Optional<Long> storsteGruppe = gruppering.values().stream().max(Long::compare);

        assertThat(gruppering).hasSize(16);
        assertThat(storsteGruppe).contains(2L);
        assertThat(gruppering.get(KV4)).isEqualTo(2L);
    }

}