package no.nav.pto.veilarbportefolje.elastic;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.Credentials;
import no.nav.pto.veilarbportefolje.util.AuthorizationUtils;
import no.nav.pto.veilarbportefolje.util.JobUtils;
import no.nav.pto.veilarbportefolje.util.RunningJob;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static no.nav.pto.veilarbportefolje.util.AuthorizationUtils.isBasicAuthAuthorized;

@Slf4j
@WebServlet(
        name = "PopulerElasticServlet",
        description = "Indeksering uten hente filer fra arena",
        urlPatterns = {"/internal/populer_elastic"}
)
public class PopulerElasticServlet extends HttpServlet {

    private ElasticIndexer elasticIndexer;
    private Credentials serviceUserCredentials;

    @Autowired
    public PopulerElasticServlet(ElasticIndexer elasticIndexer, Credentials serviceUserCredentials) {
        this.elasticIndexer = elasticIndexer;
        this.serviceUserCredentials = serviceUserCredentials;
    }

    @Override
    @SneakyThrows
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        if (isBasicAuthAuthorized(req, serviceUserCredentials)) {
            RunningJob runningJob = JobUtils.runAsyncJob(elasticIndexer::startIndeksering);
            resp.getWriter().write(String.format("Hovedindeksering i ElasticSearch startet med jobId: %s på pod %s", runningJob.getJobId(), runningJob.getPodName()));
            resp.setStatus(200);

        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
