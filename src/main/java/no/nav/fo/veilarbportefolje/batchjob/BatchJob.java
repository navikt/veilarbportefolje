package no.nav.fo.veilarbportefolje.batchjob;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.metrics.Event;
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

    public static Optional<Job> runAsyncJobOnLeader(Runnable runnable, String jobName) {
        if (isNotLeader()) {
            return Optional.empty();
        }
        Job job = runAsyncJob(runnable, jobName);
        return Optional.of(job);
    }


    @SneakyThrows
    public static Job runAsyncJob(Runnable runnable, String jobName) {

        String jobId = generateId();

        log.info("Running job with jobId {}", jobId);

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            MDC.put(MDC_JOB_ID, jobId);
            runnable.run();
            MDC.remove(MDC_JOB_ID);
        });

        future.exceptionally(e -> {
            String name = jobName + ".jobb.feilet";
            Event event = MetricsFactory.createEvent(name);
            event.report();

            throw new RuntimeException(e);
        });

        String podName = getLocalHost().getHostName();
        return new Job(jobId, podName);
    }

}

