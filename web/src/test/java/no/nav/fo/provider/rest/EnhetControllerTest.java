package no.nav.fo.provider.rest;


import no.nav.common.auth.SubjectHandler;
import no.nav.fo.domene.Filtervalg;
import no.nav.fo.service.PepClient;
import no.nav.fo.service.SolrService;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

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

    @Test
    public void skalHentPortefoljeFraIndeksDersomTilgang() throws Exception {
        when(pepClient.tilgangTilEnhet(any(), any())).thenReturn(true);
        when(pepClient.isSubjectMemberOfModiaOppfolging(any(), any())).thenReturn(true);
        when(SubjectHandler.getIdent()).thenReturn(Optional.of("testident"));

        enhetController.hentPortefoljeForEnhet("0001", 0, 0, "ikke_satt", "ikke_satt", new Filtervalg());

        verify(solrService, times(1)).hentBrukere(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void skalHenteHelePortefoljeFraIndeksDersomManMangleAntall() throws Exception {
        when(pepClient.tilgangTilEnhet(any(), any())).thenReturn(true);
        when(pepClient.isSubjectMemberOfModiaOppfolging(any(), any())).thenReturn(true);

        enhetController.hentPortefoljeForEnhet("0001", 0, null, "ikke_satt", "ikke_satt", new Filtervalg());

        verify(solrService, times(1)).hentBrukere(any(), any(), any(), any(), any(), any(), isNull());
    }

    @Test
    public void skalHenteHelePortefoljeFraIndeksDersomManMangleFra() throws Exception {
        when(pepClient.tilgangTilEnhet(any(), any())).thenReturn(true);
        when(pepClient.isSubjectMemberOfModiaOppfolging(any(), any())).thenReturn(true);

        enhetController.hentPortefoljeForEnhet("0001", null, 20, "ikke_satt", "ikke_satt", new Filtervalg());

        verify(solrService, times(1)).hentBrukere(any(), any(), any(), any(), any(), isNull(), any());
    }
}