package no.nav.pto.veilarbportefolje.krr;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.Credentials;
import no.nav.pto.veilarbportefolje.util.AuthorizationUtils;
import no.nav.pto.veilarbportefolje.util.JobUtils;
import no.nav.pto.veilarbportefolje.util.RunningJob;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@WebServlet(
        name = "PopulerKrr",
        description = "Hent krr fra difi",
        urlPatterns = {"/internal/populer_krr"}
)
public class PopulerKrrServlet extends HttpServlet {

    private final KrrService krrService;
    private final Credentials serviceUserCredentials;

    @Autowired
    public PopulerKrrServlet(KrrService krrService, Credentials serviceUserCredentials) {
        this.serviceUserCredentials = serviceUserCredentials;
        this.krrService = krrService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (AuthorizationUtils.isBasicAuthAuthorized(req, serviceUserCredentials)) {

            RunningJob runningJob = JobUtils.runAsyncJob(krrService::hentDigitalKontaktInformasjonBolk);
            resp.getWriter().write(String.format("Startet oppdatering av reservesjonsdata fra krr (via dkif) med jobId %s på pod %s", runningJob.getJobId(), runningJob.getPodName()));
            resp.setStatus(SC_OK);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }

}
