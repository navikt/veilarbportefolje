package no.nav.pto.veilarbportefolje.domene;

import no.nav.common.types.identer.Fnr;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class FnrTest {

    @Test
    public void skalReturnereStreng() {
        String expected = "12345678900";
        Fnr fnr = Fnr.ofValidFnr(expected);
        String result = fnr.toString();
        assertEquals(expected, result);
    }

    @Test
    public void skalSammenligneBasertPaaVerdi() {
        String fnr = "12345678900";
        Fnr fnr1 = Fnr.ofValidFnr(fnr);
        Fnr fnr2 = Fnr.ofValidFnr(fnr);
        assertEquals(fnr1, fnr2);
    }

    @Test
    public void skalKasteExceptionVedUgyldigFnr() {
        assertThrows(IllegalArgumentException.class, () -> Fnr.ofValidFnr("123"));
    }

    @Test
    public void skalBareGodtaNumeriskeStrenger() {
        assertThrows(IllegalArgumentException.class, () -> Fnr.ofValidFnr("aaaaaaaaaaa"));
    }
}
