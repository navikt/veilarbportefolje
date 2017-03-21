package no.nav.fo.util;

import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class MetricsUtils {

    public static <S, T> Function<S, T> timed(String navn, Function<S, T> function) {
        return (S s) -> {
            Timer timer = MetricsFactory.createTimer(navn);
            T t = null;
            try {
                timer.start();
                t = function.apply(s);
            } catch (Throwable e) {
                timer.setFailed();
                throw e;
            } finally {
                timer.stop();
                timer.report();
            }
            return t;
        };
    }

    public static <S> S timed(String navn, Supplier<S> function) {
        Timer timer = MetricsFactory.createTimer(navn);
        S s = null;
        try {
            timer.start();
            s = function.get();
        } catch (Throwable e) {
            timer.setFailed();
            throw e;
        } finally {
            timer.stop();
            timer.report();
        }
        return s;
    }

    public static <S> Consumer<S> timed(String navn, Consumer<S> function) {
        return (S s) -> {
            Timer timer = MetricsFactory.createTimer(navn);
            try {
                timer.start();
                function.accept(s);
            } catch (Throwable e) {
                timer.setFailed();
                throw e;
            } finally {
                timer.stop();
                timer.report();
            }
        };
    }
}
