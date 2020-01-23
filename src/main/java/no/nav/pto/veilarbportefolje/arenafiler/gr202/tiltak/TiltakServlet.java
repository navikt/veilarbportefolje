package no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.internal.AuthorizationUtils;
import no.nav.jobutils.JobUtils;
import no.nav.jobutils.RunningJob;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Slf4j
public class TiltakServlet extends HttpServlet {

    private TiltakHandler tiltakHandler;

    public TiltakServlet(TiltakHandler tiltakHandler) {
        this.tiltakHandler = tiltakHandler;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (AuthorizationUtils.isBasicAuthAuthorized(req)) {
            RunningJob runningJob = JobUtils.runAsyncJob(tiltakHandler::startOppdateringAvTiltakIDatabasen);
            resp.getWriter().write(String.format("Oppdatering av tiltak startet med jobId %s på pod %s", runningJob.getJobId(), runningJob.getPodName()));

            resp.setStatus(200);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
