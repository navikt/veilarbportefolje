package no.nav.fo.veilarbportefolje.internal;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.service.KrrService;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.runAsync;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
public class PopulerKrrServlet extends HttpServlet {

    private KrrService krrService;

    public PopulerKrrServlet(KrrService krrService) {
        this.krrService = krrService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (AuthorizationUtils.isBasicAuthAuthorized(req)) {

            CompletableFuture<Void> future = runAsync(() -> krrService.hentDigitalKontaktInformasjonBolk());
            future.exceptionally(e -> {
                throw new RuntimeException(e);
            });

            resp.getWriter().write("Startet henting av reservesjonsdata fra krr (via dkif)");
            resp.setStatus(SC_OK);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }

}
