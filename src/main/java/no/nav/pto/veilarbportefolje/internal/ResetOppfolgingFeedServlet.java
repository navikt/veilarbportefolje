package no.nav.pto.veilarbportefolje.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.internal.AuthorizationUtils.isBasicAuthAuthorized;

@Slf4j
@WebServlet(
        name = " ResetOppfolgingFeed",
        description = "Spoler tilbake oppfolgingsfeeden",
        urlPatterns = {"/internal/reset_feed_oppfolging"}
)
public class ResetOppfolgingFeedServlet extends HttpServlet {

    private final OppfolgingRepository oppfolgingRepository;

    @Autowired
    public ResetOppfolgingFeedServlet(OppfolgingRepository oppfolgingRepository) {
        this.oppfolgingRepository = oppfolgingRepository;
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
            oppfolgingRepository.updateOppfolgingFeedId(fromId);
            resp.getWriter().write(String.format("Stilte oppfolging-feeeden tilbake til id: %s", fromId));
            resp.setStatus(200);

        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
