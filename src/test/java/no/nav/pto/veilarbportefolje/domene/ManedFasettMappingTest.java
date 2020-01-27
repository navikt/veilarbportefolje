package no.nav.pto.veilarbportefolje.domene;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
import static no.nav.pto.veilarbportefolje.domene.ManedFasettMapping.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ManedFasettMappingTest {
    private final LocalDateTime startDato;
    private final LocalDateTime dato;
    private final Optional<ManedFasettMapping> resultat;

    public ManedFasettMappingTest(LocalDateTime startDato, LocalDateTime dato, Optional<ManedFasettMapping> resultat) {
        this.startDato = startDato;
        this.dato = dato;
        this.resultat = resultat;
    }

    public static LocalDateTime dato(int ar, Month maned, int dag) {
        return LocalDateTime.of(ar, maned, dag, 12, 00);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return IntStream.range(1970, 2050)
                .mapToObj((ar) -> new Object[][]{
                        // Med utgangspunkt i januar
                        {dato(ar, JANUARY, 15), dato(ar, JANUARY, 15), of(MND1)},
                        {dato(ar, JANUARY, 15), dato(ar, FEBRUARY, 15), of(MND2)},
                        {dato(ar, JANUARY, 15), dato(ar, JUNE, 15), of(MND6)},
                        {dato(ar, JANUARY, 15), dato(ar, AUGUST, 15), of(MND8)},
                        {dato(ar, JANUARY, 15), dato(ar, OCTOBER, 15), of(MND10)},
                        {dato(ar, JANUARY, 15), dato(ar, DECEMBER, 15), of(MND12)},

                        // Med utgangspunkt i Mai
                        {dato(ar, MAY, 15), dato(ar, MAY, 15), of(MND1)},
                        {dato(ar, MAY, 15), dato(ar, JUNE, 15), of(MND2)},
                        {dato(ar, MAY, 15), dato(ar, OCTOBER, 15), of(MND6)},
                        {dato(ar, MAY, 15), dato(ar, DECEMBER, 15), of(MND8)},
                        {dato(ar, MAY, 15), dato(ar + 1, FEBRUARY, 15), of(MND10)},
                        {dato(ar, MAY, 15), dato(ar + 1, APRIL, 15), of(MND12)},

                        // Med utgangspunkt i Desember
                        {dato(ar, DECEMBER, 15), dato(ar, DECEMBER, 15), of(MND1)},
                        {dato(ar, DECEMBER, 15), dato(ar + 1, JANUARY, 15), of(MND2)},
                        {dato(ar, DECEMBER, 15), dato(ar + 1, MAY, 15), of(MND6)},
                        {dato(ar, DECEMBER, 15), dato(ar + 1, JULY, 15), of(MND8)},
                        {dato(ar, DECEMBER, 15), dato(ar + 1, SEPTEMBER, 15), of(MND10)},
                        {dato(ar, DECEMBER, 15), dato(ar + 1, NOVEMBER, 15), of(MND12)},

                        // Utenfor ett-års rammen
                        {dato(ar, JANUARY, 15), dato(ar + 1, FEBRUARY, 15), empty()},
                        {dato(ar, JANUARY, 15), dato(ar - 3, NOVEMBER, 15), empty()},
                })
                .flatMap(Stream::of)
                .collect(toList());
    }

    @Test
    public void skalGiRiktigVerdi() {
        Optional<ManedFasettMapping> actual = finnManed(this.startDato, this.dato);

        assertThat(actual.isPresent()).isEqualTo(this.resultat.isPresent());
        this.resultat.ifPresent((res) -> assertThat(actual.get()).isEqualTo(res));
    }
}
