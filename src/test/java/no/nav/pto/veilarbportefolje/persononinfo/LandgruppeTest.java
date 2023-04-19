package no.nav.pto.veilarbportefolje.persononinfo;

import no.nav.pto.veilarbportefolje.persononinfo.personopprinelse.Landgruppe;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LandgruppeTest {
    @Test
    public void testGettingLandgruppe() {
        Assertions.assertEquals(Landgruppe.getInstance().getLandgruppeForLandKode("NOR"), "0");
        Assertions.assertEquals(Landgruppe.getInstance().getLandgruppeForLandKode("CAN"), "1");
        Assertions.assertEquals(Landgruppe.getInstance().getLandgruppeForLandKode("LUX"), "1");
        Assertions.assertEquals(Landgruppe.getInstance().getLandgruppeForLandKode("HRV"), "2");
        Assertions.assertEquals(Landgruppe.getInstance().getLandgruppeForLandKode("LTU"), "2");
        Assertions.assertEquals(Landgruppe.getInstance().getLandgruppeForLandKode("MNE"), "3");
        Assertions.assertEquals(Landgruppe.getInstance().getLandgruppeForLandKode("KWT"), "3");
        Assertions.assertEquals(Landgruppe.getInstance().getLandgruppeForLandKode("SLV"), "3");
        Assertions.assertEquals(Landgruppe.getInstance().getLandgruppeForLandKode("XXA"), "3");
    }
}