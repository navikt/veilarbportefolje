package no.nav.pto.veilarbportefolje.persononinfo;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class LandgruppeTest {
    @Test
    public void testGettingLandgruppe() {
        Assert.assertEquals(Landgruppe.getLandgruppe("NOR"), "0");
        Assert.assertEquals(Landgruppe.getLandgruppe("CAN"), "1");
        Assert.assertEquals(Landgruppe.getLandgruppe("LUX"), "1");
        Assert.assertEquals(Landgruppe.getLandgruppe("HRV"), "2");
        Assert.assertEquals(Landgruppe.getLandgruppe("LTU"), "2");
        Assert.assertEquals(Landgruppe.getLandgruppe("MNE"), "3");
        Assert.assertEquals(Landgruppe.getLandgruppe("KWT"), "3");
        Assert.assertEquals(Landgruppe.getLandgruppe("SLV"), "3");
    }

}