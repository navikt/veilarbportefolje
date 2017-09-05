package no.nav.fo.util;

import org.junit.Test;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class MetricsUtilsTest {

    @Test
    void supplier() {
        Supplier<String> supplier = () -> "Hello, world!";
        String value = MetricsUtils.timed("name", supplier);

        assertThat(value, is("Hello, world!"));
    }

    @Test
    void consumer() {
        Consumer<String> consumer = mock(Consumer.class);
        MetricsUtils.timed("name", consumer).accept("Hello, world!");

        verify(consumer, times(1)).accept("Hello, world!");
    }

    @Test
    void runnable() {
        Runnable runnable = mock(Runnable.class);
        MetricsUtils.timed("name", runnable);

        verify(runnable, times(1)).run();
    }
}