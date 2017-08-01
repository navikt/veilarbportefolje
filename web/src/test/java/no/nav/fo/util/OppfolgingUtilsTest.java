package no.nav.fo.util;

import io.vavr.control.Try;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppfolgingUtilsTest {

    @Mock
    private BrukerRepository brukerRepository;

    @Before
    public void setUp() {
        reset(brukerRepository);
    }

    @Test
    public void skalSletteArbeidsliste() {
        BrukerOppdatertInformasjon bruker = new BrukerOppdatertInformasjon().setAktoerid("testaktoeri").setVeileder("testveileder");
        when(brukerRepository.retrieveVeileder(any(AktoerId.class))).thenReturn(Try.success(null));

        assertTrue(OppfolgingUtils.skalArbeidslisteSlettes(bruker,brukerRepository ));
    }
}