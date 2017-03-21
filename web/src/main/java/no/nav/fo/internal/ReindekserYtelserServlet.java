package no.nav.fo.internal;

import com.google.common.io.CharStreams;
import no.nav.fo.consumer.KopierGR199FraArena;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import static java.lang.String.format;

public class ReindekserYtelserServlet extends HttpServlet {

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
            kopierGR199FraArena.kopierOgIndekser();
            resp.getWriter().write("reindeksering startet");
            resp.setStatus(200);
        } else if (this.masternode.isEmpty()) {
            resp.getWriter().write("fant ikke masternoden");
            resp.setStatus(500);
        } else {
            URL masterUrl = new URL(format("https://%s/internal/reindekserytelser", masternode));
            InputStreamReader reader = new InputStreamReader(masterUrl.openStream());
            String masterResp = CharStreams.toString(reader);
            resp.getWriter().write("master sa: " + masterResp);
            resp.setStatus(200);
        }
    }
}
