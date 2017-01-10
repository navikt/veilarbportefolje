package no.nav.fo.service;

import no.nav.fo.config.ApplicationTestConfig;
import no.nav.virksomhet.tjenester.enhet.meldinger.v1.WSHentEnhetListeRequest;
import no.nav.virksomhet.tjenester.enhet.meldinger.v1.WSHentEnhetListeResponse;
import no.nav.virksomhet.tjenester.enhet.v1.Enhet;
import no.nav.virksomhet.tjenester.enhet.v1.HentEnhetListeRessursIkkeFunnet;
import no.nav.virksomhet.tjenester.enhet.v1.HentEnhetListeUgyldigInput;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;

import static java.util.Collections.singletonList;
import static no.nav.fo.domene.Utils.createRessurs;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ApplicationTestConfig.class})
public class VirksomhetEnhetTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private WSHentEnhetListeResponse response;

    @Inject
    private Enhet virksomhetEnhet;

    @Inject
    private VirksomhetEnhetServiceImpl virksomhetEnhetServiceImpl;

    @Before
    public void before() throws Exception {
        reset(virksomhetEnhet);
        response = createWSHentEnhetListeResponse();
        when(virksomhetEnhet.hentEnhetListe(any(WSHentEnhetListeRequest.class))).thenReturn(response);
    }

    @Test
    public void hentEnhetListeOk() throws Exception {
        WSHentEnhetListeResponse response = virksomhetEnhetServiceImpl.hentEnhetListe("X123456");
        verify(virksomhetEnhet).hentEnhetListe(any(WSHentEnhetListeRequest.class));
        assertThat(response.getEnhetListe().get(0).getEnhetId(), is("1"));
    }

    @Test
    public void shouldThrowExceptionWhenNotFound() throws Exception {
        HentEnhetListeRessursIkkeFunnet exception = new HentEnhetListeRessursIkkeFunnet("msg");
        when(virksomhetEnhet.hentEnhetListe(any(WSHentEnhetListeRequest.class))).thenThrow(exception);

        expectedException.expect(HentEnhetListeRessursIkkeFunnet.class);
        expectedException.expectMessage(containsString("msg"));

        virksomhetEnhetServiceImpl.hentEnhetListe("ident");
    }

    @Test
    public void shouldThrowExceptionWhenInvalidInputException() throws Exception {
        HentEnhetListeUgyldigInput exception = new HentEnhetListeUgyldigInput("msg");
        when(virksomhetEnhet.hentEnhetListe(any(WSHentEnhetListeRequest.class))).thenThrow(exception);

        expectedException.expect(HentEnhetListeUgyldigInput.class);
        expectedException.expectMessage(containsString("msg"));

        virksomhetEnhetServiceImpl.hentEnhetListe("ident");
    }

    @Test
    public void shouldThrowExceptionWhenRuntimeExceptionIsThrown() throws Exception {
        IllegalArgumentException exception = new IllegalArgumentException("feil");
        when(virksomhetEnhet.hentEnhetListe(any(WSHentEnhetListeRequest.class))).thenThrow(exception);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(containsString("feil"));

        virksomhetEnhetServiceImpl.hentEnhetListe("ident");
    }


    private WSHentEnhetListeResponse createWSHentEnhetListeResponse() {
        WSHentEnhetListeResponse response = new WSHentEnhetListeResponse();
        response.getEnhetListe().addAll(singletonList(createEnhet("1", "navn1")));
        response.setRessurs(createRessurs());
        return response;
    }

    private no.nav.virksomhet.organisering.enhetogressurs.v1.Enhet createEnhet(String enhetId, String navn) {
        no.nav.virksomhet.organisering.enhetogressurs.v1.Enhet enhet = mock(no.nav.virksomhet.organisering.enhetogressurs.v1.Enhet.class);
        when(enhet.getEnhetId()).thenReturn(enhetId);
        when(enhet.getNavn()).thenReturn(navn);
        return enhet;
    }
}
