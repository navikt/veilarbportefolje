package no.nav.pto.veilarbportefolje.domene;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.time.Month.*;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.domene.KvartalFasettMapping.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class KvartalFasettMappingTest {

    private final LocalDateTime startDato;
    private final LocalDateTime dato;
    private final Optional<KvartalFasettMapping> resultat;

    public KvartalFasettMappingTest(LocalDateTime startDato, LocalDateTime dato, Optional<KvartalFasettMapping> resultat) {
        this.startDato = startDato;
        this.dato = dato;
        this.resultat = resultat;
    }

    public static LocalDateTime dato(int ar, Month maned, int dag) {
        return LocalDateTime.of(ar, maned, dag, 12, 00);
    }

    @Parameters
    public static Collection<Object[]> data() {
        return IntStream.range(1970, 2050)
                .mapToObj((ar) -> new Object[][]{
                        // Alltid KV1 om det er i samme kvartal
                        {dato(ar, JANUARY, 15), dato(ar, MARCH, 15), of(KV1)},
                        {dato(ar, APRIL, 15), dato(ar, MAY, 15), of(KV1)},
                        {dato(ar, AUGUST, 15), dato(ar, SEPTEMBER, 15), of(KV1)},
                        {dato(ar, NOVEMBER, 15), dato(ar, NOVEMBER, 16), of(KV1)},
                        {dato(ar, NOVEMBER, 15), dato(ar, DECEMBER, 16), of(KV1)},

                        // Gir KV1-KV4 for kvartal innenfor samme år
                        {dato(ar, JANUARY, 15), dato(ar, MAY, 15), of(KV2)},
                        {dato(ar, JANUARY, 15), dato(ar, AUGUST, 15), of(KV3)},
                        {dato(ar, JANUARY, 15), dato(ar, NOVEMBER, 15), of(KV4)},
                        {dato(ar, APRIL, 15), dato(ar, AUGUST, 15), of(KV2)},
                        {dato(ar, APRIL, 15), dato(ar, NOVEMBER, 15), of(KV3)},
                        {dato(ar, AUGUST, 15), dato(ar, NOVEMBER, 15), of(KV2)},

                        // Gir KV5-KV16 for kvartal i fremtidige år
                        {dato(ar, JANUARY, 15), dato(ar + 1, MARCH, 15), of(KV5)},
                        {dato(ar, JANUARY, 15), dato(ar + 1, MAY, 15), of(KV6)},
                        {dato(ar, JANUARY, 15), dato(ar + 1, AUGUST, 15), of(KV7)},
                        {dato(ar, JANUARY, 15), dato(ar + 1, NOVEMBER, 15), of(KV8)},
                        {dato(ar, JANUARY, 15), dato(ar + 2, MARCH, 15), of(KV9)},
                        {dato(ar, JANUARY, 15), dato(ar + 2, MAY, 15), of(KV10)},
                        {dato(ar, JANUARY, 15), dato(ar + 2, AUGUST, 15), of(KV11)},
                        {dato(ar, JANUARY, 15), dato(ar + 2, NOVEMBER, 15), of(KV12)},
                        {dato(ar, JANUARY, 15), dato(ar + 3, MARCH, 15), of(KV13)},
                        {dato(ar, JANUARY, 15), dato(ar + 3, MAY, 15), of(KV14)},
                        {dato(ar, JANUARY, 15), dato(ar + 3, AUGUST, 15), of(KV15)},
                        {dato(ar, JANUARY, 15), dato(ar + 3, NOVEMBER, 15), of(KV16)},

                        // Utenfor fire-års rammen
                        {dato(ar, JANUARY, 15), dato(ar - 1, DECEMBER, 15), empty()},
                        {dato(ar, JANUARY, 15), dato(ar + 4, JANUARY, 15), empty()},
                        {dato(ar, JANUARY, 15), dato(ar + 4, NOVEMBER, 15), empty()}
                })
                .flatMap(Stream::of)
                .collect(toList());
    }

    @Test
    public void skalGiRiktigVerdi() {
        Optional<KvartalFasettMapping> actual = finnKvartal(this.startDato, this.dato);

        assertThat(actual.isPresent()).isEqualTo(this.resultat.isPresent());
        this.resultat.ifPresent((res) -> assertThat(actual.get()).isEqualTo(res));
    }
}
