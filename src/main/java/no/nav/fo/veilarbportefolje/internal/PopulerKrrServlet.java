package no.nav.fo.veilarbportefolje.internal;

import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.batchjob.BatchJob;
import no.nav.fo.veilarbportefolje.batchjob.RunningJob;
import no.nav.fo.veilarbportefolje.krr.KrrService;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static no.nav.metrics.MetricsFactory.getMeterRegistry;

@Slf4j
public class PopulerKrrServlet extends HttpServlet {

    private KrrService krrService;

    private final Counter counter;

    public PopulerKrrServlet(KrrService krrService) {
        this.krrService = krrService;
        this.counter = Counter.builder("portefolje_oppdaterkrr_feilet").register(getMeterRegistry());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (AuthorizationUtils.isBasicAuthAuthorized(req)) {

            RunningJob runningJob = BatchJob.runAsyncJob(krrService::oppdaterDigitialKontaktinformasjon, counter);
            resp.getWriter().write(String.format("Startet oppdatering av reservesjonsdata fra krr (via dkif) med jobId %s på pod %s", runningJob.getJobId(), runningJob.getPodName()));
            resp.setStatus(SC_OK);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }

}
