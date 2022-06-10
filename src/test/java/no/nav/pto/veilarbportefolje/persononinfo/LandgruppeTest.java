package no.nav.pto.veilarbportefolje.persononinfo;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class LandgruppeTest {
    @Test
    public void testGettingLandgruppe() {
        Assert.assertEquals(Landgruppe.getLandgruppe("NOR"), Optional.of(0));
        Assert.assertEquals(Landgruppe.getLandgruppe("CAN"), Optional.of(1));
        Assert.assertEquals(Landgruppe.getLandgruppe("LUX"), Optional.of(1));
        Assert.assertEquals(Landgruppe.getLandgruppe("HRV"), Optional.of(2));
        Assert.assertEquals(Landgruppe.getLandgruppe("LTU"), Optional.of(2));
        Assert.assertEquals(Landgruppe.getLandgruppe("MNE"), Optional.of(3));
        Assert.assertEquals(Landgruppe.getLandgruppe("KWT"), Optional.of(3));
        Assert.assertEquals(Landgruppe.getLandgruppe("SLV"), Optional.of(3));
    }

}