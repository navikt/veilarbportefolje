package no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak;

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


@Slf4j
@WebServlet(
        name = "TiltakServlet",
        description = "Manuell Indeksering: Oppdatering av tiltak",
        urlPatterns = {"/internal/oppdater_ytelser"}
)
public class TiltakServlet extends HttpServlet {

    private Credentials serviceUserCredentials;


    @Autowired
    public TiltakServlet(Credentials serviceUserCredentials) {
        this.serviceUserCredentials = serviceUserCredentials;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (AuthorizationUtils.isBasicAuthAuthorized(req, serviceUserCredentials)) {
            resp.setStatus(410);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
