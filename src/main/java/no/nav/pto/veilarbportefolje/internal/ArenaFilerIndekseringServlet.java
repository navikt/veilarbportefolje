package no.nav.pto.veilarbportefolje.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser.KopierGR199FraArena;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakHandler;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.util.RunningJob;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static no.nav.pto.veilarbportefolje.util.JobUtils.runAsyncJob;
@Slf4j
@WebServlet(
        name = "Hovedindeksering",
        description = "Hent ytelser og tilltak fra arena og så indekser alle brukere på nytt",
        urlPatterns = {"/internal/totalhovedindeksering"}
)

public class ArenaFilerIndekseringServlet extends HttpServlet {

    private final ElasticIndexer elasticIndexer;

    private final TiltakHandler tiltakHandler;

    private final KopierGR199FraArena kopierGR199FraArena;

    @Autowired
    public ArenaFilerIndekseringServlet(ElasticIndexer elasticIndexer, TiltakHandler tiltakHandler, KopierGR199FraArena kopierGR199FraArena) {
        this.elasticIndexer = elasticIndexer;
        this.tiltakHandler = tiltakHandler;
        this.kopierGR199FraArena = kopierGR199FraArena;
    }

    @Override
    @SneakyThrows
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        if (AuthorizationUtils.isBasicAuthAuthorized(req)) {
            RunningJob runningJob = runAsyncJob(
                    () -> {
                        try {
                            kopierGR199FraArena.startOppdateringAvYtelser();
                            tiltakHandler.startOppdateringAvTiltakIDatabasen();
                        } finally {
                            elasticIndexer.startIndeksering();
                        }
                    }
            );

            resp.getWriter().write(String.format("Total elastic startet med jobId %s på pod %s", runningJob.getJobId(), runningJob.getPodName()));
            resp.setStatus(200);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
