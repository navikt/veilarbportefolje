package no.nav.fo.service;

import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.InternalServerErrorException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class SjekkBrukertilgangTest {

    @Mock
    private Pep pep;

    @InjectMocks
    private PepClientImpl pepClient;

    @BeforeClass
    public static void setup() {
        System.setProperty("no.nav.brukerdialog.security.context.subjectHandlerImplementationClass", no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler.class.getName());
        System.setProperty("no.nav.modig.core.context.subjectHandlerImplementationClass", no.nav.modig.core.context.ThreadLocalSubjectHandler.class.getName());
    }

    @Test
    public void brukerSkalIkkeHaTilgangTilEnhet() throws Exception {
        gitt_at_pep_sier(Decision.Deny);
        boolean brukerHarTilgang = pepClient.tilgangTilEnhet("X123456", "5555");
        assertThat(brukerHarTilgang, is(false));
    }

    @Test
    public void brukerSkalIkkeHaTilgangTilEnhet2() throws Exception {
        gitt_at_pep_sier(Decision.Indeterminate);
        boolean brukerHarTilgang = pepClient.tilgangTilEnhet("X123456", "5555");
        assertThat(brukerHarTilgang, is(false));
    }

    @Test
    public void brukerSkalIkkeHaTilgangTilEnhet3() throws Exception {
        gitt_at_pep_sier(Decision.NotApplicable);
        boolean brukerHarTilgang = pepClient.tilgangTilEnhet("X123456", "5555");
        assertThat(brukerHarTilgang, is(false));
    }

    @Test(expected = InternalServerErrorException.class)
    public void brukerSkalIkkeHaTilgangTilEnhet4() throws Exception {
        gitt_at_pep_kaster_feil();
        pepClient.tilgangTilEnhet("X123456", "5555");
    }

    @Test
    public void brukerSkalHaTilgangTilEnhet() throws Exception {
        gitt_at_pep_sier(Decision.Permit);
        boolean brukerHarTilgang = pepClient.tilgangTilEnhet("X123456", "1111");
        assertThat(brukerHarTilgang, is(true));
    }

    private void gitt_at_pep_sier(Decision decision) throws PepException {
        BiasedDecisionResponse response = new BiasedDecisionResponse(decision, null);
        when(pep.harTilgangTilEnhet(any(), any())).thenReturn(response);
    }

    private void gitt_at_pep_kaster_feil() throws PepException {
        when(pep.harTilgangTilEnhet(any(), any())).thenThrow(new PepException("Noe gikk veldig feil"));

    }
}
