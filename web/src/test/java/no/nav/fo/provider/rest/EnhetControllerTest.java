package no.nav.fo.provider.rest;


import no.nav.brukerdialog.security.context.InternbrukerSubjectHandler;
import no.nav.fo.domene.Filtervalg;
import no.nav.fo.service.BrukertilgangService;
import no.nav.fo.service.PepClient;
import no.nav.fo.service.SolrService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;

import java.util.Optional;

import static java.lang.System.setProperty;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EnhetControllerTest {

    @Mock
    private BrukertilgangService brukertilgangService;

    @Mock
    private SolrService solrService;

    @Mock
    private PepClient pepClient;

    @InjectMocks
    private EnhetController enhetController;

    @Before
    public void setup() {
        setProperty("no.nav.modig.core.context.subjectHandlerImplementationClass", InternbrukerSubjectHandler.class.getName());
        InternbrukerSubjectHandler.setVeilederIdent("testident");
        System.clearProperty("portefolje.pilot.enhetliste");

    }

    @Test
    public void skalReturnereTomResponsNaarEnhetIkkeErIPilot() throws Exception {
        System.setProperty("portefolje.pilot.enhetliste", "0000,0001");

        Response response = enhetController.hentPortefoljeForEnhet("0002", 0, 0, "ikke_satt", "ikke_satt", new Filtervalg());
        verify(brukertilgangService, never()).harBrukerTilgang(any(),any());
    }

    @Test
    public void skalHentPortefoljeFraIndeksDersomEnhetErIPilot() throws Exception {
        System.setProperty("portefolje.pilot.enhetliste", "0000,0001");
        when(brukertilgangService.harBrukerTilgang(any(), any())).thenReturn(true);
        when(pepClient.isSubjectMemberOfModiaOppfolging(any(), any())).thenReturn(true);

        Response response = enhetController.hentPortefoljeForEnhet("0001", 0, 0, "ikke_satt", "ikke_satt", new Filtervalg());

        verify(solrService, times(1)).hentBrukere(any(), any(), any(), any(), any());
    }

    @Test
    public void skalHentPortefoljeFraIndeksDersomIngenEnheterIPilotliste() throws Exception {
        when(brukertilgangService.harBrukerTilgang(any(), any())).thenReturn(true);
        when(pepClient.isSubjectMemberOfModiaOppfolging(any(), any())).thenReturn(true);

        Response response = enhetController.hentPortefoljeForEnhet("0000", 0, 0, "ikke_satt", "ikke_satt", new Filtervalg());

        verify(solrService, times(1)).hentBrukere(any(), any(), any(), any(), any());
    }

    @Test
    public void skalhentePortefoljeDersomListeIkkeInneholderGyldigEnhet() throws Exception {
        System.setProperty("portefolje.pilot.enhetliste", "[]");

        when(brukertilgangService.harBrukerTilgang(any(), any())).thenReturn(true);
        when(pepClient.isSubjectMemberOfModiaOppfolging(any(), any())).thenReturn(true);

        Response response = enhetController.hentPortefoljeForEnhet("0000", 0, 0, "ikke_satt", "ikke_satt", new Filtervalg());

        verify(solrService, times(1)).hentBrukere(any(), any(), any(), any(), any());

    }

}