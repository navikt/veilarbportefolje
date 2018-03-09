package no.nav.fo.domene;

import no.nav.fo.exception.UgyldigAntallDagerIgjenException;
import org.junit.jupiter.api.Test;

import static no.nav.fo.domene.AAPUnntakUkerIgjenFasettMapping.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AAPUnntakUkerIgjenFasettMappingTest {

    @Test
    void finnUkemapping() {
        assertEquals(UKE_UNDER2, finnUkeMapping(-1).get());
        assertEquals(UKE_UNDER2, finnUkeMapping(0).get());
        assertEquals(UKE_UNDER2, finnUkeMapping(5*2-1).get());

        assertEquals(UKE2_6, finnUkeMapping(5*2).get());
        assertEquals(UKE2_6, finnUkeMapping(5*6-1).get());

        assertEquals(UKE92_96, finnUkeMapping(5*92).get());
        assertEquals(UKE92_96, finnUkeMapping(5*96-1).get());

        assertEquals(UKE102_104, finnUkeMapping(5*102).get());
        assertEquals(UKE102_104, finnUkeMapping(5*104-1).get());
        assertEquals(UKE102_104, finnUkeMapping(5*104).get());
    }

    @Test
    void skalKasteUgyldigAntallDagerIgjenException() {
        assertThrows(UgyldigAntallDagerIgjenException.class, () -> finnUkeMapping(523));
    }
}