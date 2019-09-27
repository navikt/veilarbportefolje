package no.nav.fo.veilarbportefolje.filmottak.ytelser;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.internal.AuthorizationUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static no.nav.fo.veilarbportefolje.batchjob.BatchJob.runAsyncJobOnLeader;

@Slf4j
public class YtelserServlet extends HttpServlet {

    private KopierGR199FraArena kopierGR199FraArena;

    public YtelserServlet(KopierGR199FraArena kopierGR199FraArena) {
        this.kopierGR199FraArena = kopierGR199FraArena;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (AuthorizationUtils.isBasicAuthAuthorized(req)) {
            log.info("Manuell Indeksering: Oppdatering av ytelser");
            runAsyncJobOnLeader(kopierGR199FraArena::startOppdateringAvYtelser, "startOppdateringAvYtelser");
            resp.getWriter().write("Oppdatering av ytelser startet");
            resp.setStatus(200);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
