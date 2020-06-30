package no.nav.pto.veilarbportefolje.api;


import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EnhetControllerTest {

    @Rule
    public SubjectRule subjectRule = new SubjectRule(new Subject("testident", InternBruker, oidcToken("token", new HashMap<>())));

    @Mock
    private ElasticIndexer elasticIndexer;

    @Mock
    private AuthService authService;

    @InjectMocks
    private EnhetController enhetController;

    @Test
    public void skalHentPortefoljeFraIndeksDersomTilgang() throws Exception {
        when(pepClient.tilgangTilEnhet(any(), any())).thenReturn(true);
        when(pepClient.isSubjectMemberOfModiaOppfolging(any(), any())).thenReturn(true);

        enhetController.hentPortefoljeForEnhet("0001", 0, 0, "ikke_satt", "ikke_satt", new Filtervalg());

        verify(elasticIndexer, times(1)).hentBrukere(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void skalHenteHelePortefoljeFraIndeksDersomManMangleAntall() throws Exception {
        when(pepClient.tilgangTilEnhet(any(), any())).thenReturn(true);
        when(pepClient.isSubjectMemberOfModiaOppfolging(any(), any())).thenReturn(true);

        enhetController.hentPortefoljeForEnhet("0001", 0, null, "ikke_satt", "ikke_satt", new Filtervalg());

        verify(elasticIndexer, times(1)).hentBrukere(any(), any(), any(), any(), any(), any(), isNull());
    }

    @Test
    public void skalHenteHelePortefoljeFraIndeksDersomManMangleFra() throws Exception {
        when(pepClient.tilgangTilEnhet(any(), any())).thenReturn(true);
        when(pepClient.isSubjectMemberOfModiaOppfolging(any(), any())).thenReturn(true);

        enhetController.hentPortefoljeForEnhet("0001", null, 20, "ikke_satt", "ikke_satt", new Filtervalg());

        verify(elasticIndexer, times(1)).hentBrukere(any(), any(), any(), any(), any(), isNull(), any());
    }
}
