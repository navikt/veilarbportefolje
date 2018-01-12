package no.nav.fo.domene;

import no.nav.fo.exception.UgyldigAntallDagerIgjenException;
import org.junit.jupiter.api.Test;

import static no.nav.fo.domene.AAPUnntakUkerIgjenFasettMapping.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AAPUnntakUkerIgjenFasettMappingTest {

    @Test
    void finnUkemapping() {
        assertEquals(UKE_UNDER12, finnUkeMapping(-1).get());
        assertEquals(UKE_UNDER12, finnUkeMapping(0).get());
        assertEquals(UKE_UNDER12, finnUkeMapping(5*12-1).get());

        assertEquals(UKE12_23, finnUkeMapping(5*12).get());
        assertEquals(UKE12_23, finnUkeMapping(5*23-1).get());

        assertEquals(UKE24_35, finnUkeMapping(5*24).get());
        assertEquals(UKE24_35, finnUkeMapping(5*35-1).get());

        assertEquals(UKE36_47, finnUkeMapping(5*36).get());
        assertEquals(UKE36_47, finnUkeMapping(5*47-1).get());

        assertEquals(UKE48_59, finnUkeMapping(5*48).get());
        assertEquals(UKE48_59, finnUkeMapping(5*59-1).get());

        assertEquals(UKE60_71, finnUkeMapping(5*60).get());
        assertEquals(UKE60_71, finnUkeMapping(5*71-1).get());

        assertEquals(UKE72_83, finnUkeMapping(5*72).get());
        assertEquals(UKE72_83, finnUkeMapping(5*83-1).get());

        assertEquals(UKE84_95, finnUkeMapping(5*84).get());
        assertEquals(UKE84_95, finnUkeMapping(5*95-1).get());

        assertEquals(UKE96_107, finnUkeMapping(5*96).get());
        assertEquals(UKE96_107, finnUkeMapping(5*104).get());
        assertEquals(UKE96_107, finnUkeMapping(522).get());
        assertEquals(UKE96_107, finnUkeMapping(510).get());
    }

    @Test
    void skalKasteUgyldigAntallDagerIgjenException() {
        assertThrows(UgyldigAntallDagerIgjenException.class, () -> finnUkeMapping(523));
    }
}