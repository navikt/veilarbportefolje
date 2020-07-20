package no.nav.pto.veilarbportefolje.controller;


import no.nav.common.abac.Pep;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakService;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;


import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EnhetControllerTest {

    private ElasticIndexer elasticIndexer;
    private AuthService authService;
    private EnhetController enhetController;
    private Pep pep;

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
