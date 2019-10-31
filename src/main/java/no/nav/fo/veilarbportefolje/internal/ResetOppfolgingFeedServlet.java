package no.nav.fo.veilarbportefolje.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.database.OppfolgingFeedRepository;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.Optional;

import static no.nav.fo.veilarbportefolje.internal.AuthorizationUtils.isBasicAuthAuthorized;

@Slf4j
public class ResetOppfolgingFeedServlet extends HttpServlet {

    private OppfolgingFeedRepository oppfolgingFeedRepository;

    @Inject
    public ResetOppfolgingFeedServlet(OppfolgingFeedRepository oppfolgingFeedRepository) {
        this.oppfolgingFeedRepository = oppfolgingFeedRepository;
    }

    @Override
    @SneakyThrows
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        BigDecimal fromId;
        try {
            fromId = Optional.ofNullable(req.getParameter("fromId"))
                    .map(BigDecimal::new).get();
        } catch (NumberFormatException e) {
            throw new WebApplicationException("Kunne ikke lese fromId, bruk kun tall", Response.Status.BAD_REQUEST);
        }

        if (isBasicAuthAuthorized(req)) {
            oppfolgingFeedRepository.updateOppfolgingFeedId(fromId);
            resp.getWriter().write(String.format("Stilte oppfolging-feeeden tilbake til id: %s", fromId));
            resp.setStatus(200);

        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
