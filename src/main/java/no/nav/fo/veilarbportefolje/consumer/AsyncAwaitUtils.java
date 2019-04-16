package no.nav.fo.veilarbportefolje.consumer;

import java.util.List;
import java.util.concurrent.*;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;
import static no.nav.common.utils.CollectionUtils.listOf;

public class AsyncAwaitUtils {

    private static ExecutorService exceptionThread = Executors.newSingleThreadExecutor();
    private static ExecutorService jobThread = Executors.newSingleThreadExecutor();

    public static CompletableFuture async(Runnable job) {
        return async(listOf(job));
    }

    public static CompletableFuture async(List<Runnable> jobs) {
        CompletableFuture[] futures = jobs.stream()
                .map(job -> runAsync(job, jobThread))
                .toArray(CompletableFuture[]::new);

        return allOf(futures);
    }

    public static void await(CompletableFuture<?> future) {
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            exceptionThread.execute(() -> {
                throw new RuntimeException(e);
            });
        }
    }
}
