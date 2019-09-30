package no.nav.fo.veilarbportefolje.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.batchjob.Job;
import no.nav.fo.veilarbportefolje.indeksering.ElasticIndexer;
import no.nav.fo.veilarbportefolje.batchjob.BatchJob;

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
            Job job = BatchJob.runAsyncJob(elasticIndexer::startIndeksering, "populerElastic");
            resp.getWriter().write(String.format("Hovedindeksering i ElasticSearch startet med jobId: %s på pod %s", job.getJobId(), job.getPodName()));
            resp.setStatus(200);

        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
