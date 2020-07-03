package no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.pto.veilarbportefolje.internal.AuthorizationUtils;
import no.nav.pto.veilarbportefolje.util.JobUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class YtelserServlet extends HttpServlet {

    private KopierGR199FraArena kopierGR199FraArena;
    private LeaderElectionClient leaderElectionClient;

    public YtelserServlet(KopierGR199FraArena kopierGR199FraArena, LeaderElectionClient leaderElectionClient) {
        this.kopierGR199FraArena = kopierGR199FraArena;
        this.leaderElectionClient = leaderElectionClient;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (AuthorizationUtils.isBasicAuthAuthorized(req)) {
            log.info("Manuell Indeksering: Oppdatering av ytelser");
            JobUtils.runAsyncJobOnLeader(kopierGR199FraArena::startOppdateringAvYtelser, leaderElectionClient);
            resp.getWriter().write("Oppdatering av ytelser startet");
            resp.setStatus(200);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
