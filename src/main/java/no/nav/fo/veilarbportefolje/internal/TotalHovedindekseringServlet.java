package no.nav.fo.veilarbportefolje.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.batchjob.Job;
import no.nav.fo.veilarbportefolje.filmottak.tiltak.TiltakHandler;
import no.nav.fo.veilarbportefolje.filmottak.ytelser.KopierGR199FraArena;
import no.nav.fo.veilarbportefolje.indeksering.ElasticIndexer;
import no.nav.fo.veilarbportefolje.service.KrrService;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static no.nav.fo.veilarbportefolje.batchjob.BatchJob.runAsyncJob;

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

            Job job = runAsyncJob(
                    () -> {
                        kopierGR199FraArena.startOppdateringAvYtelser();
                        tiltakHandler.startOppdateringAvTiltakIDatabasen();
                        krrService.hentDigitalKontaktInformasjonBolk();
                        elasticIndexer.startIndeksering();
                    }
                    , "totalIndexering"
            );

            resp.getWriter().write(String.format("Total indeksering startet med jobId %s på pod %s", job.getJobId(), job.getPodName()));
            resp.setStatus(200);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }

}
