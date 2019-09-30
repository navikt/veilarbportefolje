package no.nav.fo.veilarbportefolje.batchjob;

public class Job {
    private String jobId;
    private String podName;

    Job(String jobId, String podName) {
        this.jobId = jobId;
        this.podName = podName;
    }

    public String getJobId() {
        return jobId;
    }

    public String getPodName() {
        return podName;
    }
}
