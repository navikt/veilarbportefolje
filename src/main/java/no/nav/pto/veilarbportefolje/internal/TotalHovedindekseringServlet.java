package no.nav.pto.veilarbportefolje.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser.KopierGR199FraArena;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakHandler;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.krr.KrrService;
import no.nav.jobutils.RunningJob;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static no.nav.jobutils.JobUtils.runAsyncJob;

@Slf4j
public class TotalHovedindekseringServlet extends HttpServlet {

    private ElasticIndexer elasticIndexer;

    private TiltakHandler tiltakHandler;

    private KopierGR199FraArena kopierGR199FraArena;

    private KrrService krrService;

    public TotalHovedindekseringServlet(ElasticIndexer elasticIndexer, TiltakHandler tiltakHandler, KopierGR199FraArena kopierGR199FraArena, KrrService krrService) {
        this.elasticIndexer = elasticIndexer;
        this.tiltakHandler = tiltakHandler;
        this.kopierGR199FraArena = kopierGR199FraArena;
        this.krrService = krrService;
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
                            krrService.hentDigitalKontaktInformasjonBolk();
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
