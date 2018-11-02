package no.nav.fo.veilarbportefolje.internal;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.service.SolrService;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
public class PopulerIndekseringServlet extends HttpServlet {

    private SolrService solrService;

    public PopulerIndekseringServlet(SolrService solrService) {
        this.solrService = solrService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (AuthorizationUtils.isBasicAuthAuthorized(req)) {
            log.info("Manuell Indeksering: Hovedindeksering");
            runAsync(() -> solrService.hovedindeksering());
            resp.getWriter().write("Hovedindeksering startet");
            resp.setStatus(200);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }

}
