package no.nav.fo.routes;

import no.nav.melding.virksomhet.loependeytelser.v1.Dagpengetellere;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;

import static java.lang.String.valueOf;
import static java.time.Month.MARCH;
import static no.nav.fo.routes.IndekserHandler.utlopsdatoUtregning;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class IndekserHandlerTest {
    public static final LocalDate START_DATO = LocalDate.of(2017, MARCH, 7);

    private final Dagpengetellere testdata;
    private final LocalDate result;

    public static Dagpengetellere dt(int dager, int uker) {
        return new Dagpengetellere() {{
            setAntallDagerIgjen(new BigInteger(valueOf(dager)));
            setAntallUkerIgjen(new BigInteger(valueOf(uker)));
        }};
    }

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {dt(1, 0), START_DATO},
                {dt(3, 0), START_DATO.plusDays(2)},
                {dt(4, 0), START_DATO.plusDays(3)},
                {dt(0, 1), START_DATO.plusDays(6)},
                {dt(1, 1), START_DATO.plusDays(7)},
                {dt(4, 2), START_DATO.plusDays(17)},
                {dt(0, 3), START_DATO.plusDays(20)}
        });
    }

    public IndekserHandlerTest(Dagpengetellere testdata, LocalDate result) {
        this.testdata = testdata;
        this.result = result;
    }


    @Test
    public void skalGiRiktigVerdi() {
        assertThat(utlopsdatoUtregning(START_DATO, this.testdata)).isEqualTo(this.result);
    }

}