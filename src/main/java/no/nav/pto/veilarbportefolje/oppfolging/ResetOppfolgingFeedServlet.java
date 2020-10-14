package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.Credentials;
import no.nav.pto.veilarbportefolje.util.AuthorizationUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;

import static no.nav.pto.veilarbportefolje.util.AuthorizationUtils.isBasicAuthAuthorized;

@Slf4j
@WebServlet(
        name = " ResetOppfolgingFeed",
        description = "Spoler tilbake oppfolgingsfeeden",
        urlPatterns = {"/internal/reset_feed_oppfolging"}
)
public class ResetOppfolgingFeedServlet extends HttpServlet {

    private final OppfolgingRepository oppfolgingRepository;
    private final Credentials serviceUserCredentials;

    @Autowired
    public ResetOppfolgingFeedServlet(OppfolgingRepository oppfolgingRepository, Credentials serviceUserCredentials) {
        this.oppfolgingRepository = oppfolgingRepository;
        this.serviceUserCredentials = serviceUserCredentials;
    }

    @Override
    @SneakyThrows
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        final long fromId;
        try {
            fromId = Long.parseLong(req.getParameter("fromId"));
        } catch (NumberFormatException e) {
            return;
        }

        if (isBasicAuthAuthorized(req, serviceUserCredentials)) {
            oppfolgingRepository.updateOppfolgingFeedId(BigDecimal.valueOf(fromId));
            resp.getWriter().write(String.format("Stilte oppfolging-feeeden tilbake til id: %s", fromId));
            resp.setStatus(200);

        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
