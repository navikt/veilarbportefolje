package no.nav.pto.veilarbportefolje.result;

import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.supplyAsync;

public class Result<T> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(Result.class);
    private final Optional<Throwable> err;
    private final Optional<T> ok;

    private Result(Optional<Throwable> err, Optional<T> ok) {
        this.err = err;
        this.ok = ok;
    }

    public static <T> Result<T> of(Supplier<T> supplier) {
        T t;
        try {
            t = supplier.get();
        } catch (Throwable throwable) {
            return Result.err(throwable);
        }
        return Result.ok(t);
    }

    public static <T> CompletableFuture<Result<T>> ofAsync(Supplier<T> supplier) {
        CompletableFuture<T> future = supplyAsync(supplier);

        return future.handle((value, throwable) -> {
            if (throwable != null) {
                log.error("Async call failed", throwable);
                return Result.err(throwable);
            }
            return Result.ok(value);
        });
    }

    public static <T> Result<T> ok(T t) {
        return new Result<T>(Optional.empty(), Optional.ofNullable(t));
    }

    public static <T> Result<T> err(Throwable throwable) {
        return new Result<>(Optional.of(throwable), Optional.empty());
    }

    public Optional<Throwable> err() {
        return err;
    }

    public Optional<T> ok() {
        return ok;
    }

    public boolean isErr() {
        return err.isPresent();
    }

    public boolean isOk() {
        return ok.isPresent();
    }
}
