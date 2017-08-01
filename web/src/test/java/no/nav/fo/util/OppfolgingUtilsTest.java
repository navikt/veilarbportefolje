package no.nav.fo.util;

import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.Oppfolgingstatus;
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
        Oppfolgingstatus oppfolgingstatus =  new Oppfolgingstatus().setVeileder(null);

        assertTrue(OppfolgingUtils.skalArbeidslisteSlettes(oppfolgingstatus, "NY VEILEDER"));
    }

    @Test
    public void brukerSkalVaereUnderOppfolging() {

        Oppfolgingstatus status = new Oppfolgingstatus()
                .setFormidlingsgruppekode((String) ARBEIDSOKERKODER.toArray()[0])
                .setServicegruppekode("DUMMY")
                .setOppfolgingsbruker(true);

        assertTrue(OppfolgingUtils.erBrukerUnderOppfolging(status));
    }

    @Test
    public void brukerSkalVaereUnderOppfolging2() {
        Oppfolgingstatus status = new Oppfolgingstatus()
                .setFormidlingsgruppekode("DUMMY")
                .setServicegruppekode("DUMMY")
                .setOppfolgingsbruker(true);

        assertTrue(OppfolgingUtils.erBrukerUnderOppfolging(status));
    }

    @Test
    public void brukerSkalIKKEVaereUnderOppfolging1() {

        Oppfolgingstatus status = new Oppfolgingstatus()
                .setFormidlingsgruppekode("DUMMY")
                .setServicegruppekode("DUMMY")
                .setOppfolgingsbruker(false);

        assertFalse(OppfolgingUtils.erBrukerUnderOppfolging(status));
    }
}