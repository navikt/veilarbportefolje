package no.nav.pto.veilarbportefolje.util;

import java.util.Optional;
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

    public Optional<Throwable> err() {
        return Optional.ofNullable(err);
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

    public <U> Result<U> mapOk(Function<V, U> function) {
        return ok == null ? err(err) : Result.of(() -> function.apply(ok));
    }

    public Result<V> mapError(Function<Throwable, Result<V>> function) {
        return ok == null ? function.apply(err) : ok(ok) ;
    }

    public V orElse(V value) {
        return ok == null ? value : ok;
    }

    public V orElseThrowException() {
        if (err == null) {
            return ok;
        }
        throw new RuntimeException(err);
    }

    @Override
    public String toString() {
        return ok == null ? "Err(" + err + ")" : "Ok(" + ok + ")";
    }
}
