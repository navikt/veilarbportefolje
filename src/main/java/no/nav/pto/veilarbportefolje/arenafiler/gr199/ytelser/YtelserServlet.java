package no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.common.utils.Credentials;
import no.nav.pto.veilarbportefolje.util.AuthorizationUtils;
import no.nav.pto.veilarbportefolje.util.JobUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@WebServlet(
        name = "YtelserServlet",
        description = "Manuell Indeksering: Oppdatering av ytelser",
        urlPatterns = {"/internal/oppdater_tiltak"}
)
public class YtelserServlet extends HttpServlet {

    private KopierGR199FraArena kopierGR199FraArena;
    private LeaderElectionClient leaderElectionClient;
    private Credentials serviceUserCredentials;

    @Autowired
    public YtelserServlet(KopierGR199FraArena kopierGR199FraArena, LeaderElectionClient leaderElectionClient, Credentials serviceUserCredentials) {
        this.kopierGR199FraArena = kopierGR199FraArena;
        this.leaderElectionClient = leaderElectionClient;
        this.serviceUserCredentials = serviceUserCredentials;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (AuthorizationUtils.isBasicAuthAuthorized(req, serviceUserCredentials)) {
            log.info("Manuell Indeksering: Oppdatering av ytelser");
            JobUtils.runAsyncJobOnLeader(kopierGR199FraArena::startOppdateringAvYtelser, leaderElectionClient);
            resp.getWriter().write("Oppdatering av ytelser startet");
            resp.setStatus(200);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
