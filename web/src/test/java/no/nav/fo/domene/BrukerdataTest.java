package no.nav.fo.domene;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BrukerdataTest {

    @Test
    public void skalReturnereJaNeiStreng() throws Exception {
        String JA = "J";
        String NEI = "N";
        String shouldBeJa = Brukerdata.safeToJaNei(true);
        String shouldBeNei = Brukerdata.safeToJaNei(false);
        String shouldBeNeiIfFalse = Brukerdata.safeToJaNei(null);
        assertEquals(JA, shouldBeJa);
        assertEquals(NEI, shouldBeNei);
        assertEquals(NEI, shouldBeNeiIfFalse);
    }
}