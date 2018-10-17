package no.nav.fo.filmottak.tiltak;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.internal.AuthorizationUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class TiltakServlet extends HttpServlet {

    private TiltakHandler tiltakHandler;

    @Override
    public void init() throws ServletException {
        this.tiltakHandler = WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean(TiltakHandler.class);
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (AuthorizationUtils.isBasicAuthAuthorized(req)) {
            log.info("Setter i gang oppdatering av tiltak");
            resp.getWriter().write("Setter i gang oppdatering av tiltak");
            tiltakHandler.startOppdateringAvTiltakIDatabasen();
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
