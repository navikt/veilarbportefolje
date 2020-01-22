package no.nav.pto.veilarbportefolje.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.feed.dialog.DialogFeedRepository;
import no.nav.pto.veilarbportefolje.util.DateUtils;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.internal.AuthorizationUtils.isBasicAuthAuthorized;

@Slf4j
public class ResetDialogFeedServlet extends HttpServlet {

    private DialogFeedRepository dialogFeedRepository;

    @Inject
    public ResetDialogFeedServlet(DialogFeedRepository dialogFeedRepository) {
        this.dialogFeedRepository = dialogFeedRepository;
    }

    @Override
    @SneakyThrows
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        Timestamp fromTimestamp = Optional.ofNullable(req.getParameter("fromId"))
                .map(DateUtils::getTimestampFromSimpleISODate)
                .orElseThrow(() -> new WebApplicationException("Feil i datoformatering, bruk formatet: YYYY-MM-DD / 2020-12-31", Response.Status.BAD_REQUEST));

        if (isBasicAuthAuthorized(req)) {
            dialogFeedRepository.updateDialogFeedTimestamp(fromTimestamp);
            resp.getWriter().write(String.format("Stilte dialog-feeeden tilbake til tidspunkt: %s", fromTimestamp.toString()));
            resp.setStatus(200);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
