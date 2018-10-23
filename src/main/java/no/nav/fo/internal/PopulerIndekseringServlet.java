package no.nav.fo.internal;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.service.SolrService;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
public class PopulerIndekseringServlet extends HttpServlet {

    private SolrService solrService;

    @Override
    public void init() throws ServletException {
        this.solrService = WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean(SolrService.class);
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        log.info("Manuell Indeksering: Hovedindeksering");
        runAsync(() -> solrService.hovedindeksering());
    }

}
