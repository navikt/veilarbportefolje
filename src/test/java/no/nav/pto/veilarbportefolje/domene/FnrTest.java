package no.nav.pto.veilarbportefolje.domene;

import no.nav.common.types.identer.Fnr;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FnrTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void skalReturnereStreng() throws Exception {
        String expected = "12345678900";
        Fnr fnr = Fnr.ofValidFnr(expected);
        String result = fnr.toString();
        assertEquals(expected, result);
    }

    @Test
    public void skalSammenligneBasertPaaVerdi() throws Exception {
        String fnr = "12345678900";
        Fnr fnr1 = Fnr.ofValidFnr(fnr);
        Fnr fnr2 = Fnr.ofValidFnr(fnr);
        assertTrue(fnr1.equals(fnr2));
    }

    @Test
    public void skalKasteExceptionVedUgyldigFnr() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        String fnr = "123";
        Fnr.ofValidFnr(fnr);
    }

    @Test
    public void skalBareGodtaNumeriskeStrenger() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        String fnr = "aaaaaaaaaaa";
        Fnr.ofValidFnr(fnr);
    }
}
