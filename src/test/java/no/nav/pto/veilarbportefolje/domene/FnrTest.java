package no.nav.pto.veilarbportefolje.domene;

import no.nav.common.types.identer.Fnr;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FnrTest {

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
        assertEquals(fnr1, fnr2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void skalKasteExceptionVedUgyldigFnr() {
        String fnr = "123";
        Fnr.ofValidFnr(fnr);
    }

    @Test(expected = IllegalArgumentException.class)
    public void skalBareGodtaNumeriskeStrenger() {
        String fnr = "aaaaaaaaaaa";
        Fnr.ofValidFnr(fnr);
    }
}
