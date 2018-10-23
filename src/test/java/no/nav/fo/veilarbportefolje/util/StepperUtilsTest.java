package no.nav.fo.veilarbportefolje.util;

import io.vavr.Tuple2;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class StepperUtilsTest {

    @Test
    public void dagpenger() {
        testDagpenger(0, 0, 1);
        testDagpenger(1, 0, 1);
        testDagpenger(2, 2, 5);
        testDagpenger(5, 2, 5);
        testDagpenger(6, 6, 9);
        testDagpenger(9, 6, 9);
        testDagpenger(46, 46, 49);
        testDagpenger(45, 42, 45);
        testDagpenger(52, 50, 52);
    }

    @Test
    public void aapMaxTid() {
        testAAPMaxtid(0, 0, 1);
        testAAPMaxtid(1, 0, 1);
        testAAPMaxtid(2, 2, 10);
        testAAPMaxtid(10, 2, 10);
        testAAPMaxtid(209, 209, 215);
        testAAPMaxtid(215, 209, 215);
    }

    private void testDagpenger(int value, int from, int to) {
        Tuple2<Integer, Integer> step = StepperUtils.findStep(2, 3, 52, value);
        assertThat(step._1, is(from));
        assertThat(step._2, is(to));
    }

    private void testAAPMaxtid(int value, int from, int to) {
        Tuple2<Integer, Integer> step = StepperUtils.findStep(2, 8, 215, value);
        assertThat(step._1, is(from));
        assertThat(step._2, is(to));
    }
}
