package no.nav.pto.veilarbportefolje.persononinfo;

import no.nav.pto.veilarbportefolje.persononinfo.personopprinelse.Landgruppe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LandgruppeTest {
    @Test
    public void testGettingLandgruppe() {
        assertEquals("0", Landgruppe.getInstance().getLandgruppeForLandKode("NOR"));
        assertEquals("1", Landgruppe.getInstance().getLandgruppeForLandKode("CAN"));
        assertEquals("1", Landgruppe.getInstance().getLandgruppeForLandKode("LUX"));
        assertEquals("2", Landgruppe.getInstance().getLandgruppeForLandKode("HRV"));
        assertEquals("2", Landgruppe.getInstance().getLandgruppeForLandKode("LTU"));
        assertEquals("3", Landgruppe.getInstance().getLandgruppeForLandKode("MNE"));
        assertEquals("3", Landgruppe.getInstance().getLandgruppeForLandKode("KWT"));
        assertEquals("3", Landgruppe.getInstance().getLandgruppeForLandKode("SLV"));
    }
}