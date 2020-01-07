package no.nav.fo.veilarbportefolje.batchjob;

import io.micrometer.core.instrument.Counter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.metrics.MetricsFactory;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.net.InetAddress.getLocalHost;
import static no.nav.common.leaderelection.LeaderElection.isNotLeader;
import static no.nav.common.utils.IdUtils.generateId;

@Slf4j
public class BatchJob {
    private static String MDC_JOB_ID = "jobId";

    public static Optional<RunningJob> runAsyncJobOnLeader(Runnable runnable, Counter counter) {
        if (isNotLeader()) {
            return Optional.empty();
        }
        RunningJob job = runAsyncJob(runnable, counter);
        return Optional.of(job);
    }


    @SneakyThrows
    public static RunningJob runAsyncJob(Runnable runnable, Counter counter) {

        String jobId = generateId();

        log.info("Running job with jobId {}", jobId);

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            MDC.put(MDC_JOB_ID, jobId);
            runnable.run();
            MDC.remove(MDC_JOB_ID);
        });

        future.exceptionally(e -> {
            counter.increment();
            throw new RuntimeException(e);
        });

        String podName = getLocalHost().getHostName();
        return new RunningJob(jobId, podName);
    }

}

