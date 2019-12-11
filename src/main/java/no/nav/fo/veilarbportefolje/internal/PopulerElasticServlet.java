package no.nav.fo.veilarbportefolje.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.indeksering.ElasticIndexer;
import no.nav.jobutils.JobUtils;
import no.nav.jobutils.RunningJob;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static no.nav.fo.veilarbportefolje.internal.AuthorizationUtils.isBasicAuthAuthorized;

@Slf4j
public class PopulerElasticServlet extends HttpServlet {

    private ElasticIndexer elasticIndexer;

    public PopulerElasticServlet(ElasticIndexer elasticIndexer) {
        this.elasticIndexer = elasticIndexer;
    }

    @Override
    @SneakyThrows
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        if (isBasicAuthAuthorized(req)) {
            RunningJob runningJob = JobUtils.runAsyncJob(elasticIndexer::startIndeksering);
            resp.getWriter().write(String.format("Hovedindeksering i ElasticSearch startet med jobId: %s på pod %s", runningJob.getJobId(), runningJob.getPodName()));
            resp.setStatus(200);

        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
