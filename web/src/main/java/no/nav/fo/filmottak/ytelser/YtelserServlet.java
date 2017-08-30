package no.nav.fo.filmottak.ytelser;

import com.google.common.io.CharStreams;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.runAsync;

public class YtelserServlet extends HttpServlet {

    private KopierGR199FraArena kopierGR199FraArena;
    private boolean ismasternode;
    private String masternode;

    @Override
    public void init() throws ServletException {
        this.kopierGR199FraArena = WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean(KopierGR199FraArena.class);
        this.ismasternode = Boolean.valueOf(System.getProperty("cluster.ismasternode", "false"));
        this.masternode = System.getProperty("cluster.masternode", "");
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (this.ismasternode) {
            if (kopierGR199FraArena.isRunning()) {
                resp.getWriter().write("reindeksering kjører");
            } else {
                runAsync(() -> kopierGR199FraArena.kopierOgIndekser());
                resp.getWriter().write("reindeksering startet");
            }
            resp.setStatus(200);
        } else if (this.masternode.isEmpty()) {
            resp.getWriter().write("fant ikke masternoden");
            resp.setStatus(500);
        } else {
            URL masterUrl = new URL(format("http://%s:8443/veilarbportefolje/internal/reindekserytelser", masternode));
            InputStreamReader reader = new InputStreamReader(masterUrl.openStream());
            String masterResp = CharStreams.toString(reader);
            resp.getWriter().write("master sa: " + masterResp);
            resp.setStatus(200);
        }
    }
}
