package no.nav.pto.veilarbportefolje.persononinfo;

import no.nav.pto.veilarbportefolje.persononinfo.personopprinelse.Landgruppe;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class LandgruppeTest {
    @Test
    public void testGettingLandgruppe() {
        Assert.assertEquals(Landgruppe.getInstance().getLandgruppeForLandKode("NOR"), "0");
        Assert.assertEquals(Landgruppe.getInstance().getLandgruppeForLandKode("CAN"), "1");
        Assert.assertEquals(Landgruppe.getInstance().getLandgruppeForLandKode("LUX"), "1");
        Assert.assertEquals(Landgruppe.getInstance().getLandgruppeForLandKode("HRV"), "2");
        Assert.assertEquals(Landgruppe.getInstance().getLandgruppeForLandKode("LTU"), "2");
        Assert.assertEquals(Landgruppe.getInstance().getLandgruppeForLandKode("MNE"), "3");
        Assert.assertEquals(Landgruppe.getInstance().getLandgruppeForLandKode("KWT"), "3");
        Assert.assertEquals(Landgruppe.getInstance().getLandgruppeForLandKode("SLV"), "3");
    }
}