package no.nav.fo.internal;

import no.nav.fo.consumer.GR202.KopierGR202FraArena;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ReindekserTiltakServlet extends HttpServlet {

    private KopierGR202FraArena kopierGR202FraArena;

    @Override
    public void init() throws ServletException {
        this.kopierGR202FraArena = WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean(KopierGR202FraArena.class);
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        kopierGR202FraArena.kopier();
    }
}
