package no.nav.fo.veilarbportefolje.internal;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.config.IndekseringScheduler;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
public class TotalHovedindekseringServlet extends HttpServlet {

    private IndekseringScheduler indekseringScheduler;

    @Inject
    public TotalHovedindekseringServlet(IndekseringScheduler indekseringScheduler) {
        this.indekseringScheduler = indekseringScheduler;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        log.info("Manuell Indeksering: Total indeksering");
        runAsync(() -> indekseringScheduler.totalIndexering());
    }

}
