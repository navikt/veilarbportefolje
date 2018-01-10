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
        assertEquals(UKE_UNDER12, finnUkeMapping(1).get());
        assertEquals(UKE_UNDER12, finnUkeMapping(31).get());
        assertEquals(UKE_UNDER12, finnUkeMapping(83).get());

        assertEquals(UKE12_23, finnUkeMapping(84).get());
        assertEquals(UKE12_23, finnUkeMapping(99).get());
        assertEquals(UKE12_23, finnUkeMapping(167).get());

        assertEquals(UKE24_35, finnUkeMapping(168).get());
        assertEquals(UKE24_35, finnUkeMapping(200).get());
        assertEquals(UKE24_35, finnUkeMapping(251).get());

        assertEquals(UKE36_47, finnUkeMapping(252).get());
        assertEquals(UKE36_47, finnUkeMapping(300).get());
        assertEquals(UKE36_47, finnUkeMapping(335).get());

        assertEquals(UKE48_59, finnUkeMapping(336).get());
        assertEquals(UKE48_59, finnUkeMapping(370).get());
        assertEquals(UKE48_59, finnUkeMapping(419).get());

        assertEquals(UKE60_71, finnUkeMapping(420).get());
        assertEquals(UKE60_71, finnUkeMapping(450).get());
        assertEquals(UKE60_71, finnUkeMapping(503).get());

        assertEquals(UKE72_83, finnUkeMapping(504).get());
        assertEquals(UKE72_83, finnUkeMapping(522).get());
    }

    @Test
    void skalKasteUgyldigAntallDagerIgjenException() {
        assertThrows(UgyldigAntallDagerIgjenException.class, () -> finnUkeMapping(523));
        assertThrows(UgyldigAntallDagerIgjenException.class, () -> finnUkeMapping(1512));
        assertThrows(UgyldigAntallDagerIgjenException.class, () -> finnUkeMapping(1666));
    }
}