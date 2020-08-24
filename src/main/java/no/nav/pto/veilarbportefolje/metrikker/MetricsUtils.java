package no.nav.pto.veilarbportefolje.metrikker;

import no.nav.common.metrics.MetricsClient;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class MetricsUtils {

    public static <S, T> Function<S, T> timed(String navn, Function<S, T> function, MetricsClient metricsClient) {
        return timed(navn, function, null, metricsClient);
    }

    public static <S, T> Function<S, T> timed(String navn, Function<S, T> function, BiConsumer<Timer, Boolean> tagsAppender, MetricsClient metricsClient) {
        return (S s) -> {
            boolean hasFailed = false;
            Timer timer = new Timer(metricsClient, navn);
            try {
                timer.start();
                return function.apply(s);
            } catch (Throwable e) {
                hasFailed = true;
                timer.setFailed();
                throw e;
            } finally {
                timer.stop();

                if (tagsAppender != null) {
                    tagsAppender.accept(timer, hasFailed);
                }

                timer.report();
            }
        };
    }

    public static <S> S timed(String navn, Supplier<S> supplier, MetricsClient metricsClient) {
        return timed(navn, supplier, null, metricsClient);
    }

    public static <S> S timed(String navn, Supplier<S> supplier, BiConsumer<Timer, Boolean> tagsAppender, MetricsClient metricsClient) {
        return functionToSupplier(timed(navn, supplierToFunction(supplier), tagsAppender, metricsClient)).get();
    }

    public static <S> Consumer<S> timed(String navn, Consumer<S> consumer, MetricsClient metricsClient) {
        return timed(navn, consumer, null, metricsClient);
    }

    public static <S> Consumer<S> timed(String navn, Consumer<S> consumer, BiConsumer<Timer, Boolean> tagsAppender, MetricsClient metricsClient) {
        return functionToConsumer(timed(navn, consumerToFunction(consumer), tagsAppender, metricsClient));
    }

    public static void timed(String navn, Runnable runnable, MetricsClient metricsClient) {
        functionToRunnable(timed(navn, runnableToFunction(runnable), metricsClient)).run();
    }

    public static void timed(String navn, Runnable runnable, BiConsumer<Timer, Boolean> tagsAppender, MetricsClient metricsClient) {
        functionToRunnable(timed(navn, runnableToFunction(runnable), tagsAppender, metricsClient)).run();
    }

    private static <S> Function<S, Void> consumerToFunction(Consumer<S> consumer) {
        return (S s) -> { consumer.accept(s); return null; };
    }

    private static <S> Consumer<S> functionToConsumer(Function<S, Void> function) {
        return function::apply;
    }

    private static <S> Function<Void, S> supplierToFunction(Supplier<S> supplier) {
        return (Void v) -> supplier.get();
    }

    private static <S> Supplier<S> functionToSupplier(Function<Void, S> function) {
        return () -> function.apply(null);
    }

    private static Runnable functionToRunnable(Function<Void, Void> function) {
        return () -> function.apply(null);
    }

    private static Function<Void, Void> runnableToFunction(Runnable runnable) {
        return aVoid -> {
            runnable.run();
            return null;
        };
    }

}