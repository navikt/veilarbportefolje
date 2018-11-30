package no.nav.fo.veilarbportefolje.provider.rest;


import no.nav.brukerdialog.security.context.SubjectRule;
import no.nav.common.auth.Subject;
import no.nav.fo.veilarbportefolje.domene.Filtervalg;
import no.nav.fo.veilarbportefolje.service.PepClient;
import no.nav.fo.veilarbportefolje.indeksering.IndekseringService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.brukerdialog.security.domain.IdentType.InternBruker;
import static no.nav.common.auth.SsoToken.oidcToken;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EnhetControllerTest {

    @Rule
    public SubjectRule subjectRule = new SubjectRule(new Subject("testident", InternBruker, oidcToken("token")));

    @Mock
    private IndekseringService indekseringService;

    @Mock
    private PepClient pepClient;

    @InjectMocks
    private EnhetController enhetController;

    @Test
    public void skalHentPortefoljeFraIndeksDersomTilgang() throws Exception {
        when(pepClient.tilgangTilEnhet(any(), any())).thenReturn(true);
        when(pepClient.isSubjectMemberOfModiaOppfolging(any(), any())).thenReturn(true);

        enhetController.hentPortefoljeForEnhet("0001", 0, 0, "ikke_satt", "ikke_satt", new Filtervalg());

        verify(indekseringService, times(1)).hentBrukere(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void skalHenteHelePortefoljeFraIndeksDersomManMangleAntall() throws Exception {
        when(pepClient.tilgangTilEnhet(any(), any())).thenReturn(true);
        when(pepClient.isSubjectMemberOfModiaOppfolging(any(), any())).thenReturn(true);

        enhetController.hentPortefoljeForEnhet("0001", 0, null, "ikke_satt", "ikke_satt", new Filtervalg());

        verify(indekseringService, times(1)).hentBrukere(any(), any(), any(), any(), any(), any(), isNull());
    }

    @Test
    public void skalHenteHelePortefoljeFraIndeksDersomManMangleFra() throws Exception {
        when(pepClient.tilgangTilEnhet(any(), any())).thenReturn(true);
        when(pepClient.isSubjectMemberOfModiaOppfolging(any(), any())).thenReturn(true);

        enhetController.hentPortefoljeForEnhet("0001", null, 20, "ikke_satt", "ikke_satt", new Filtervalg());

        verify(indekseringService, times(1)).hentBrukere(any(), any(), any(), any(), any(), isNull(), any());
    }
}
