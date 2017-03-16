package no.nav.fo.domene;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.time.LocalDate;
import java.util.Collection;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.time.LocalDate.of;
import static java.time.Month.*;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.domene.KvartalMapping.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class KvartalMappingTest {

    private final LocalDate startDato;
    private final LocalDate dato;
    private final KvartalMapping resultat;

    public KvartalMappingTest(LocalDate startDato, LocalDate dato, KvartalMapping resultat) {
        this.startDato = startDato;
        this.dato = dato;
        this.resultat = resultat;
    }

    @Parameters
    public static Collection<Object[]> data() {
        return IntStream.range(1970, 2050)
                .mapToObj((ar) -> new Object[][]{
                        // Alltid KV1 om det er i samme kvartal
                        {of(ar, JANUARY, 15), of(ar, MARCH, 15), KV1},
                        {of(ar, APRIL, 15), of(ar, MAY, 15), KV1},
                        {of(ar, AUGUST, 15), of(ar, SEPTEMBER, 15), KV1},
                        {of(ar, NOVEMBER, 15), of(ar, NOVEMBER, 16), KV1},
                        {of(ar, NOVEMBER, 15), of(ar, DECEMBER, 16), KV1},

                        // Gir KV1-KV4 for kvartal innenfor samme år
                        {of(ar, JANUARY, 15), of(ar, MAY, 15), KV2},
                        {of(ar, JANUARY, 15), of(ar, AUGUST, 15), KV3},
                        {of(ar, JANUARY, 15), of(ar, NOVEMBER, 15), KV4},
                        {of(ar, APRIL, 15), of(ar, AUGUST, 15), KV2},
                        {of(ar, APRIL, 15), of(ar, NOVEMBER, 15), KV3},
                        {of(ar, AUGUST, 15), of(ar, NOVEMBER, 15), KV2},

                        // Gir KV5-KV16 for kvartal i fremtidige år
                        {of(ar, JANUARY, 15), of(ar + 1, MARCH, 15), KV5},
                        {of(ar, JANUARY, 15), of(ar + 1, MAY, 15), KV6},
                        {of(ar, JANUARY, 15), of(ar + 1, AUGUST, 15), KV7},
                        {of(ar, JANUARY, 15), of(ar + 1, NOVEMBER, 15), KV8},
                        {of(ar, JANUARY, 15), of(ar + 2, MARCH, 15), KV9},
                        {of(ar, JANUARY, 15), of(ar + 2, MAY, 15), KV10},
                        {of(ar, JANUARY, 15), of(ar + 2, AUGUST, 15), KV11},
                        {of(ar, JANUARY, 15), of(ar + 2, NOVEMBER, 15), KV12},
                        {of(ar, JANUARY, 15), of(ar + 3, MARCH, 15), KV13},
                        {of(ar, JANUARY, 15), of(ar + 3, MAY, 15), KV14},
                        {of(ar, JANUARY, 15), of(ar + 3, AUGUST, 15), KV15},
                        {of(ar, JANUARY, 15), of(ar + 3, NOVEMBER, 15), KV16}
                })
                .flatMap(Stream::of)
                .collect(toList());
    }

    @Test
    public void skalGiRiktigVerdi() {
        assertThat(finnKvartal(this.startDato, this.dato)).isEqualTo(this.resultat);
    }
}