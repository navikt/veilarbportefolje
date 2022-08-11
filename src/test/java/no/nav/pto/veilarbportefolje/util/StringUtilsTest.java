package no.nav.pto.veilarbportefolje.util;

import org.junit.Assert;
import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void testCapitalization() {
        Assert.assertEquals("Det Britiske Territoriet i Indiahavet", StringUtils.capitalize("DET BRITISKE TERRITORIET I INDIAHAVET"));
        Assert.assertEquals("Grønland", StringUtils.capitalize("GRØNLAND"));
        Assert.assertEquals("Nord-Makedonia", StringUtils.capitalize("NORD-MAKEDONIA"));
        Assert.assertEquals("Sør-Georgia Og Sør-Sandwichøye", StringUtils.capitalize("SØR-GEORGIA OG SØR-SANDWICHØYE"));

    }
}
