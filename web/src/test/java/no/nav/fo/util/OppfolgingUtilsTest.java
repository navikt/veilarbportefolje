package no.nav.fo.util;

import no.nav.fo.database.BrukerRepository;
import no.nav.fo.service.AktoerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static no.nav.fo.util.UnderOppfolgingRegler.ARBEIDSOKERKODER;
import static org.junit.Assert.*;
import static org.mockito.Mockito.reset;

@RunWith(MockitoJUnitRunner.class)
public class OppfolgingUtilsTest {

    @Mock
    private BrukerRepository brukerRepository;

    @Mock
    private AktoerService aktoerService;

    @Before
    public void setUp() {
        reset(brukerRepository);
        reset(aktoerService);
    }

    @Test
    public void skalSletteArbeidsliste() {
        assertTrue(OppfolgingUtils.skalArbeidslisteSlettes(null, "NY VEILEDER", true));
    }

    @Test
    public void brukerSkalVaereUnderOppfolging() {
        String formidlingsgruppekode = (String) ARBEIDSOKERKODER.toArray()[0];
        assertTrue(OppfolgingUtils.erBrukerUnderOppfolging(formidlingsgruppekode, "DUMMY", true));
    }

    @Test
    public void brukerSkalVaereUnderOppfolging2() {
        assertTrue(OppfolgingUtils.erBrukerUnderOppfolging("DUMMY", "DUMMY", true));
    }

    @Test
    public void brukerSkalIKKEVaereUnderOppfolging1() {
        assertFalse(OppfolgingUtils.erBrukerUnderOppfolging("DUMMY", "DUMMY",  false));
    }
}