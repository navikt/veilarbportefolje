package no.nav.fo.veilarbportefolje.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.batch.BatchJob;
import no.nav.fo.veilarbportefolje.indeksering.ElasticIndexer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Slf4j
public class PopulerElasticServlet extends HttpServlet {

    private ElasticIndexer elasticIndexer;

    public PopulerElasticServlet(ElasticIndexer elasticIndexer) {
        this.elasticIndexer = elasticIndexer;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (AuthorizationUtils.isBasicAuthAuthorized(req)) {
            Optional<String> maybeJobId = BatchJob.runAsync(() -> elasticIndexer.hovedindeksering());
            maybeJobId.ifPresent(id -> setResponse(resp, id));
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }

    @SneakyThrows
    private static void setResponse(HttpServletResponse resp, String id) {
        resp.getWriter().write(String.format("Hovedindeksering i ElasticSearch startet med jobId: %s", id));
        resp.setStatus(200);
    }

}
