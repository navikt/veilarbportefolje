package no.nav.fo.veilarbportefolje.filmottak.tiltak;

import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.batchjob.RunningJob;
import no.nav.fo.veilarbportefolje.internal.AuthorizationUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static no.nav.fo.veilarbportefolje.batchjob.BatchJob.runAsyncJob;
import static no.nav.metrics.MetricsFactory.getMeterRegistry;


@Slf4j
public class TiltakServlet extends HttpServlet {

    private TiltakHandler tiltakHandler;
    private Counter counter;

    public TiltakServlet(TiltakHandler tiltakHandler) {
        this.tiltakHandler = tiltakHandler;
        this.counter = Counter.builder("portefolje_oppdatertiltak_feilet").register(getMeterRegistry());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (AuthorizationUtils.isBasicAuthAuthorized(req)) {
            RunningJob runningJob = runAsyncJob(tiltakHandler::startOppdateringAvTiltakIDatabasen, counter);
            resp.getWriter().write(String.format("Oppdatering av tiltak startet med jobId %s på pod %s", runningJob.getJobId(), runningJob.getPodName()));

            resp.setStatus(200);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
