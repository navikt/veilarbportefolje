package no.nav.pto.veilarbportefolje.controller;


import no.nav.common.abac.Pep;
import no.nav.common.auth.subject.Subject;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakService;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

import static no.nav.common.auth.subject.IdentType.InternBruker;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EnhetControllerTest {

    private ElasticIndexer elasticIndexer;
    private AuthService authService;
    private EnhetController enhetController;
    private Pep pep;

    @Rule
   // public SubjectRule subjectRule = new SubjectRule(new Subject("testident", InternBruker, oidcToken("token", new HashMap<>())));
    @Before
    public void initController() {
        elasticIndexer = mock(ElasticIndexer.class);
        pep = mock(Pep.class);
        authService = new AuthService(pep);
        enhetController = new EnhetController(elasticIndexer, authService, mock(MetricsClient.class), mock(TiltakService.class));
    }

    @Test
    public void skalHentPortefoljeFraIndeksDersomTilgang() throws Exception {
        when(pep.harVeilederTilgangTilOppfolging(anyString())).thenReturn(true);

        enhetController.hentPortefoljeForEnhet("0001", 0, 0, "ikke_satt", "ikke_satt", new Filtervalg());

        verify(elasticIndexer, times(1)).hentBrukere(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void skalHenteHelePortefoljeFraIndeksDersomManMangleAntall() throws Exception {
        when(pep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(pep.harVeilederTilgangTilOppfolging(any())).thenReturn(true);

        enhetController.hentPortefoljeForEnhet("0001", 0, null, "ikke_satt", "ikke_satt", new Filtervalg());

        verify(elasticIndexer, times(1)).hentBrukere(any(), any(), any(), any(), any(), any(), isNull());
    }

    @Test
    public void skalHenteHelePortefoljeFraIndeksDersomManMangleFra() throws Exception {
        when(pep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(pep.harVeilederTilgangTilOppfolging(any())).thenReturn(true);

        enhetController.hentPortefoljeForEnhet("0001", null, 20, "ikke_satt", "ikke_satt", new Filtervalg());

        verify(elasticIndexer, times(1)).hentBrukere(any(), any(), any(), any(), any(), isNull(), any());
    }
}
