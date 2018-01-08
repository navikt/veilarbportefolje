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
        assertEquals(UKE72_83, finnUkeMapping(550).get());
        assertEquals(UKE72_83, finnUkeMapping(587).get());

        assertEquals(UKE84_95, finnUkeMapping(588).get());
        assertEquals(UKE84_95, finnUkeMapping(600).get());
        assertEquals(UKE84_95, finnUkeMapping(671).get());

        assertEquals(UKE96_107, finnUkeMapping(672).get());
        assertEquals(UKE96_107, finnUkeMapping(699).get());
        assertEquals(UKE96_107, finnUkeMapping(755).get());

        assertEquals(UKE108_119, finnUkeMapping(756).get());
        assertEquals(UKE108_119, finnUkeMapping(800).get());
        assertEquals(UKE108_119, finnUkeMapping(839).get());

        assertEquals(UKE120_131, finnUkeMapping(840).get());
        assertEquals(UKE120_131, finnUkeMapping(900).get());
        assertEquals(UKE120_131, finnUkeMapping(923).get());

        assertEquals(UKE132_143, finnUkeMapping(924).get());
        assertEquals(UKE132_143, finnUkeMapping(999).get());
        assertEquals(UKE132_143, finnUkeMapping(1007).get());

        assertEquals(UKE144_155, finnUkeMapping(1008).get());
        assertEquals(UKE144_155, finnUkeMapping(1050).get());
        assertEquals(UKE144_155, finnUkeMapping(1091).get());

        assertEquals(UKE156_167, finnUkeMapping(1092).get());
        assertEquals(UKE156_167, finnUkeMapping(1100).get());
        assertEquals(UKE156_167, finnUkeMapping(1175).get());

        assertEquals(UKE168_179, finnUkeMapping(1176).get());
        assertEquals(UKE168_179, finnUkeMapping(1200).get());
        assertEquals(UKE168_179, finnUkeMapping(1259).get());

        assertEquals(UKE180_191, finnUkeMapping(1260).get());
        assertEquals(UKE180_191, finnUkeMapping(1300).get());
        assertEquals(UKE180_191, finnUkeMapping(1343).get());

        assertEquals(UKE192_203, finnUkeMapping(1344).get());
        assertEquals(UKE192_203, finnUkeMapping(1399).get());
        assertEquals(UKE192_203, finnUkeMapping(1427).get());

        assertEquals(UKE204_215, finnUkeMapping(1428).get());
        assertEquals(UKE204_215, finnUkeMapping(1499).get());
        assertEquals(UKE204_215, finnUkeMapping(1511).get());
    }

    @Test
    void skalKasteUgyldigAntallDagerIgjenException() {
        assertThrows(UgyldigAntallDagerIgjenException.class, () -> finnUkeMapping(1512));
        assertThrows(UgyldigAntallDagerIgjenException.class, () -> finnUkeMapping(1666));
    }
}