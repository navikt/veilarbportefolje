package no.nav.fo.veilarbportefolje.internal;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.batchjob.BatchJob;
import no.nav.fo.veilarbportefolje.batchjob.RunningJob;
import no.nav.fo.veilarbportefolje.service.KrrService;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
public class PopulerKrrServlet extends HttpServlet {

    private KrrService krrService;

    public PopulerKrrServlet(KrrService krrService) {
        this.krrService = krrService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (AuthorizationUtils.isBasicAuthAuthorized(req)) {

            RunningJob runningJob = BatchJob.runAsyncJob(krrService::hentDigitalKontaktInformasjonBolk, "oppdaterkrr");
            resp.getWriter().write(String.format("Startet oppdatering av reservesjonsdata fra krr (via dkif) med jobId %s på pod %s", runningJob.getJobId(), runningJob.getPodName()));
            resp.setStatus(SC_OK);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }

}
