package no.nav.pto.veilarbportefolje.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.invoke.MethodHandles;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static java.net.InetAddress.getLocalHost;
import static no.nav.common.utils.IdUtils.generateId;

public class JobUtils {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static String MDC_JOB_ID = "jobId";

    public static RunningJob runAsyncJob(Runnable runnable) {
        String jobId = generateId();

        CompletableFuture<Void> future = CompletableFuture.runAsync(
                () -> {
                    MDC.put(MDC_JOB_ID, jobId);
                    runnable.run();
                    MDC.remove(MDC_JOB_ID);
                },
                Executors.newSingleThreadExecutor()
        );

        String podName;
        try {
            podName = getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        RunningJob runningJob = new RunningJob(jobId, podName, future);
        log.info("{}: Running job with jobId {} on pod {}", runningJob.getStartTime(), runningJob.getJobId(), runningJob.getPodName());
        return runningJob;
    }
}

