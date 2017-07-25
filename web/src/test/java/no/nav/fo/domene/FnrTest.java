package no.nav.fo.domene;

import no.nav.fo.exception.UgyldigFnrException;
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
        Fnr fnr = new Fnr(expected);
        String result = fnr.toString();
        assertEquals(expected, result);
    }

    @Test
    public void skalSammenligneBasertPaaVerdi() throws Exception {
        String fnr = "12345678900";
        Fnr fnr1 = new Fnr(fnr);
        Fnr fnr2 = new Fnr(fnr);
        assertTrue(fnr1.equals(fnr2));
    }

    @Test
    public void skalKasteExceptionVedUgyldigFnr() throws Exception {
        thrown.expect(UgyldigFnrException.class);
        String fnr = "123";
        new Fnr(fnr);
    }

    @Test
    public void skalBareGodtaNumeriskeStrenger() throws Exception {
        thrown.expect(UgyldigFnrException.class);
        String fnr = "aaaaaaaaaaa";
        new Fnr(fnr);
    }
}