package no.nav.pto.veilarbportefolje.util;

import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

public class RunningJob {
    private final String jobId;
    private final String podName;
    private final ZonedDateTime startTime;
    private final CompletableFuture<Void> completableFuture;

    RunningJob(String jobId, String podName, CompletableFuture<Void> completableFuture) {
        this.jobId = jobId;
        this.podName = podName;
        this.completableFuture = completableFuture;
        this.startTime = ZonedDateTime.now();
    }

    public String getJobId() {
        return jobId;
    }

    public String getPodName() {
        return podName;
    }

    public CompletableFuture<Void> getCompletableFuture() {
        return completableFuture;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }
}
