package no.nav.fo.domene;

import no.nav.melding.virksomhet.loependeytelser.v1.Dagpengetellere;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;

import static java.lang.String.valueOf;
import static java.time.LocalDate.of;
import static java.time.Month.MARCH;
import static no.nav.fo.domene.Utlopsdato.utlopsdatoUtregning;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@RunWith(Parameterized.class)
public class UtlopsdatoTest {
    private static final LocalDateTime START_DATO = LocalDateTime.of(2017, MARCH, 7, 0, 0);

    private final LocalDateTime startDato;
    private final Dagpengetellere testdata;
    private final LocalDateTime result;

    public static Dagpengetellere dt(int dager, int uker) {
        return new Dagpengetellere() {{
            setAntallDagerIgjen(new BigInteger(valueOf(dager)));
            setAntallUkerIgjen(new BigInteger(valueOf(uker)));
        }};
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // Data fra excel-arket: http://confluence.adeo.no/pages/viewpage.action?pageId=208240799
                {START_DATO, dt(1, 0), START_DATO},
                {START_DATO, dt(3, 0), START_DATO.plusDays(2)},
                {START_DATO, dt(4, 0), START_DATO.plusDays(3)},
                {START_DATO, dt(0, 1), START_DATO.plusDays(6)},
                {START_DATO, dt(1, 1), START_DATO.plusDays(7)},
                {START_DATO, dt(4, 2), START_DATO.plusDays(17)},
                {START_DATO, dt(0, 3), START_DATO.plusDays(20)},

                // Andre test-caser som sjekker at man ikke havner i helgen
                {of(2017, MARCH, 17), dt(1, 0), of(2017, MARCH, 17)},
                {of(2017, MARCH, 17), dt(2, 0), of(2017, MARCH, 20)},
                {of(2017, MARCH, 18), dt(1, 0), of(2017, MARCH, 20)},
                {of(2017, MARCH, 18), dt(0, 1), of(2017, MARCH, 24)}, // Skal ha 5 virkedager igjen
                {of(2017, MARCH, 18), dt(1, 1), of(2017, MARCH, 27)}
        });
    }

    public UtlopsdatoTest(LocalDateTime startDato, Dagpengetellere testdata, LocalDateTime result) {
        this.startDato = startDato;
        this.testdata = testdata;
        this.result = result;
    }


    @Test
    public void skalGiRiktigVerdi() {
        assertThat(utlopsdatoUtregning(this.startDato, this.testdata)).isEqualTo(this.result);
    }
}