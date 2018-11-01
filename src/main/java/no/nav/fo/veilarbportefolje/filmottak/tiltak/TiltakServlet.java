package no.nav.fo.veilarbportefolje.filmottak.tiltak;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.internal.AuthorizationUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
public class TiltakServlet extends HttpServlet {

    private TiltakHandler tiltakHandler;

    public TiltakServlet(TiltakHandler tiltakHandler) {
        this.tiltakHandler = tiltakHandler;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (AuthorizationUtils.isBasicAuthAuthorized(req)) {
            log.info("Manuell Indeksering: Oppdatering av tiltak");
            resp.getWriter().write("Setter i gang oppdatering av tiltak");
            runAsync(() -> tiltakHandler.startOppdateringAvTiltakIDatabasen());
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
