package no.nav.fo.veilarbportefolje.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.util.DateUtils;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.Optional;

import static no.nav.fo.veilarbportefolje.internal.AuthorizationUtils.isBasicAuthAuthorized;

@Slf4j
public class ResetAktivitetFeedServlet extends HttpServlet {

    private BrukerRepository brukerRepository;

    @Inject
    public ResetAktivitetFeedServlet(BrukerRepository brukerRepository) {
        this.brukerRepository = brukerRepository;
    }

    @Override
    @SneakyThrows
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        Timestamp fromTimestamp = Optional.ofNullable(req.getParameter("fromId"))
                .map(DateUtils::getTimestampFromSimpleISODate)
                .orElseThrow(() -> new WebApplicationException("Feil i datoformatering, bruk formatet: YYYY-MM-DD / 2020-12-31", Response.Status.BAD_REQUEST));

        final String localDateTimeString = fromTimestamp.toLocalDateTime().toString() + "Z";

        if (isBasicAuthAuthorized(req)) {
            brukerRepository.setAktiviteterSistOppdatert(localDateTimeString);
            resp.getWriter().write(String.format("Stilte aktivitet-feeeden tilbake til tidspunkt: %s", localDateTimeString));
            resp.setStatus(200);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
