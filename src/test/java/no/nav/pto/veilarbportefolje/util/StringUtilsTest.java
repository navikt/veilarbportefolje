package no.nav.pto.veilarbportefolje.util;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringUtilsTest {

    @Test
    public void testCapitalization() {
        assertEquals("Det Britiske Territoriet i Indiahavet", StringUtils.capitalize("DET BRITISKE TERRITORIET I INDIAHAVET"));
        assertEquals("Grønland", StringUtils.capitalize("GRØNLAND"));
        assertEquals("Nord-Makedonia", StringUtils.capitalize("NORD-MAKEDONIA"));
        assertEquals("Sør-Georgia Og Sør-Sandwichøye", StringUtils.capitalize("SØR-GEORGIA OG SØR-SANDWICHØYE"));
    }
}
