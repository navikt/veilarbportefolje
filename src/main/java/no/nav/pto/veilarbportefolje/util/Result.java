package no.nav.pto.veilarbportefolje.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Result<V> {
    private final Throwable err;
    private final V ok;

    private Result(Throwable err, V ok) {
        this.err = err;
        this.ok = ok;
    }

    public static <V> Result<V> of(Supplier<V> supplier) {
        V v;
        try {
            v = supplier.get();
        } catch (Throwable throwable) {
            return Result.err(throwable);
        }
        return Result.ok(v);
    }

    public static <V> Result<V> ok(final V v) {
        return new Result<>(null, v);
    }

    public static <V> Result<V> err(final Throwable throwable) {
        return new Result<>(throwable, null);
    }

    public static <V> Result<V> err(String message) {
        return new Result<>(new RuntimeException(message), null);
    }

    public Optional<Throwable> err() {
        return Optional.ofNullable(err);
    }

    public void onError(Consumer<Throwable> consumer) {
        consumer.accept(err);
    }

    public void onOk(Consumer<V> consumer) {
        consumer.accept(ok);
    }

    public Optional<V> ok() {
        return Optional.ofNullable(ok);
    }

    public boolean isErr() {
        return err != null;
    }

    public boolean isOk() {
        return ok != null;
    }

    public <U> Result<U> andThen(Function<V, Result<U>> function) {
        return err != null ? err(err) : function.apply(ok);
    }

    public <U> Result<U> map(Function<V, U> function) {
        return err != null ? err(err) : Result.of(() -> function.apply(ok));
    }

    public <U> Result<U> map(Supplier<U> supplier) {
        return err != null ? err(err) : Result.of(supplier);
    }

    public Result<V> mapError(Function<Throwable, Result<V>> function) {
        return err != null ? function.apply(err) : ok(ok);
    }

    public V orElse(V value) {
        return err != null ? value : ok;
    }

    public V orElseThrowException() {
        if (err != null) {
            throw new RuntimeException(err);
        }
        return ok;
    }

    @Override
    public String toString() {
        return ok == null ? "Err(" + err + ")" : "Ok(" + ok + ")";
    }
}
