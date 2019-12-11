package no.nav.fo.veilarbportefolje.batchjob;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectHandler;
import org.slf4j.MDC;

import java.util.concurrent.CompletableFuture;

import static java.net.InetAddress.getLocalHost;
import static no.nav.common.utils.IdUtils.generateId;

@Slf4j
public class BatchJob {

    private static String MDC_JOB_ID = "jobId";

    @SneakyThrows
    public static RunningJob runAsyncJobWithSubject(Subject subject, Runnable runnable) {

        String jobId = generateId();

        log.info("Running job with jobId {}", jobId);

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            MDC.put(MDC_JOB_ID, jobId);
            SubjectHandler.withSubject(subject, runnable::run);
            MDC.remove(MDC_JOB_ID);
        });

        future.exceptionally(e -> {
            throw new RuntimeException(e);
        });

        String podName = getLocalHost().getHostName();
        return new RunningJob(jobId, podName);
    }

}

