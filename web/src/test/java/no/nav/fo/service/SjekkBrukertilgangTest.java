package no.nav.fo.service;

import no.nav.fo.config.ApplicationTestConfig;
import no.nav.virksomhet.tjenester.enhet.meldinger.v1.WSHentEnhetListeRequest;
import no.nav.virksomhet.tjenester.enhet.meldinger.v1.WSHentEnhetListeResponse;
import no.nav.virksomhet.tjenester.enhet.v1.Enhet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;

import static no.nav.fo.domene.Utils.createRessurs;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ApplicationTestConfig.class})
public class SjekkBrukertilgangTest {

    @Rule
    public ExpectedException expectedException =ExpectedException.none();

    private WSHentEnhetListeResponse response;

    @Inject
    private Enhet virksomhetEnhet;

    @Inject
    private VirksomhetEnhetServiceImpl virksomhetEnhetServiceImpl;

    @Inject
    private BrukertilgangService brukertilgangService;

    @Before
    public void before() throws Exception {
        reset(virksomhetEnhet);
        response = createWSHentEnhetListeResponse();
        when(virksomhetEnhet.hentEnhetListe(any(WSHentEnhetListeRequest.class))).thenReturn(response);
    }

    @Test
    public void brukerSkalIkkeHaTilgangTilEnhet() throws Exception {
        boolean brukerHarTilgang = brukertilgangService.harBrukerTilgangTilEnhet("X123456", "5555");
        assertThat(brukerHarTilgang, is(false));
    }

    @Test
    public void brukerSkalHaTilgangTilEnhet() throws Exception {
        boolean brukerHarTilgang = brukertilgangService.harBrukerTilgangTilEnhet("X123456", "1111");
        assertThat(brukerHarTilgang, is(true));
    }

    private WSHentEnhetListeResponse createWSHentEnhetListeResponse() {
        WSHentEnhetListeResponse response = new WSHentEnhetListeResponse();
        List<no.nav.virksomhet.organisering.enhetogressurs.v1.Enhet> enhetListe = new ArrayList<>();
        enhetListe.add(createEnhet("1111", "Nord"));
        enhetListe.add(createEnhet("2222","Øst"));
        enhetListe.add(createEnhet("3333","Sør"));
        enhetListe.add(createEnhet("4444","Vest"));
        response.getEnhetListe().addAll(enhetListe);
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
