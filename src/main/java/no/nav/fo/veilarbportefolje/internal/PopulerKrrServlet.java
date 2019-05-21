package no.nav.fo.veilarbportefolje.internal;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.service.KrrService;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class PopulerKrrServlet extends HttpServlet {

    private KrrService krrService;

    public PopulerKrrServlet(KrrService krrService) {
        this.krrService = krrService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (AuthorizationUtils.isBasicAuthAuthorized(req)) {
            krrService.hentDigitalKontaktInformasjonBolk();
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }

}
