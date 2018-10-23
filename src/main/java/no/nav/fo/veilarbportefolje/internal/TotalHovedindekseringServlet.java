package no.nav.fo.veilarbportefolje.internal;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.config.IndekseringScheduler;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
public class TotalHovedindekseringServlet extends HttpServlet {

    private IndekseringScheduler indekseringScheduler;

    @Override
    public void init() throws ServletException {
        this.indekseringScheduler = WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean(IndekseringScheduler.class);
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        log.info("Manuell Indeksering: Total indeksering");
        runAsync(() -> indekseringScheduler.totalIndexering());
    }

}
