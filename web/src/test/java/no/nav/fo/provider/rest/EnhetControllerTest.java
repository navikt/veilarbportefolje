package no.nav.fo.provider.rest;


import no.nav.brukerdialog.security.context.InternbrukerSubjectHandler;
import no.nav.fo.domene.Filtervalg;
import no.nav.fo.service.PepClient;
import no.nav.fo.service.SolrService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.lang.System.setProperty;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EnhetControllerTest {

    @Mock
    private SolrService solrService;

    @Mock
    private PepClient pepClient;

    @InjectMocks
    private EnhetController enhetController;

    @Before
    public void setup() {
        setProperty("no.nav.brukerdialog.security.context.subjectHandlerImplementationClass", InternbrukerSubjectHandler.class.getName());
        InternbrukerSubjectHandler.setVeilederIdent("testident");
    }

    @Test
    public void skalHentPortefoljeFraIndeksDersomTilgang() throws Exception {
        when(pepClient.tilgangTilEnhet(any(), any())).thenReturn(true);
        when(pepClient.isSubjectMemberOfModiaOppfolging(any(), any())).thenReturn(true);

        enhetController.hentPortefoljeForEnhet("0001", 0, 0, "ikke_satt", "ikke_satt", new Filtervalg());

        verify(solrService, times(1)).hentBrukere(any(), any(), any(), any(), any(), any(), any());
    }

}