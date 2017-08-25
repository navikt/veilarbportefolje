package no.nav.fo.internal;

import no.nav.fo.consumer.GR202.KopierGR202FraArena;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ReindekserTiltakServlet extends HttpServlet {

    static Logger logger = LoggerFactory.getLogger(ReindekserTiltakServlet.class);

    private KopierGR202FraArena kopierGR202FraArena;
    private boolean ismasternode;

    @Override
    public void init() throws ServletException {
        this.kopierGR202FraArena = WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean(KopierGR202FraArena.class);
        this.ismasternode = Boolean.valueOf(System.getProperty("cluster.ismasternode", "false"));
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(this.ismasternode) {
            if(!kopierGR202FraArena.isRunning()) {
                logger.info("Setter i gang oppdatering av tiltak");
                resp.getWriter().write("Setter i gang oppdatering av tiltak");
                kopierGR202FraArena.hentTiltakOgPopulerDatabase();
            }
            else {
                logger.info("Kunne ikke starte oppdatering av tiltak fordi den allerede kjører");
                resp.getWriter().write("Kunne ikke starte oppdatering av tiltak fordi den allerede kjører");
            }
        }
    }
}
