package no.nav.fo.internal;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import no.nav.fo.consumer.GR202.KopierGR202FraArena;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class ReindekserTiltakServlet extends HttpServlet {

    private KopierGR202FraArena kopierGR202FraArena;

    @Override
    public void init() throws ServletException {
        this.kopierGR202FraArena = WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean(KopierGR202FraArena.class);
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<String> files = null;
        try {
            files = kopierGR202FraArena.kopier();
        } catch (JSchException | SftpException e) {
            resp.getWriter().write(e.getMessage());
            return;
        }
        if(files.isEmpty()) {
            resp.getWriter().write("Ingen filer");
        }
        else {
            for(String s : files) {
                resp.getWriter().write(s);
            }
        }
    }
}
