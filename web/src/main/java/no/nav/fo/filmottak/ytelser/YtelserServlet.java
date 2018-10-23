package no.nav.fo.filmottak.ytelser;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.internal.AuthorizationUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
public class YtelserServlet extends HttpServlet {

    private KopierGR199FraArena kopierGR199FraArena;

    @Override
    public void init() throws ServletException {
        this.kopierGR199FraArena = WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean(KopierGR199FraArena.class);
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (AuthorizationUtils.isBasicAuthAuthorized(req)) {
            log.info("Manuell Indeksering: Starter oppdatering av ytelser");
            runAsync(() -> kopierGR199FraArena.startOppdateringAvYtelser());
            resp.getWriter().write("reindeksering startet");
            resp.setStatus(200);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
