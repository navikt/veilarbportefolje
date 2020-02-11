package no.nav.pto.veilarbportefolje.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetDAO;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class SlettAktivitetServlet extends HttpServlet {

    private AktivitetDAO database;
    private ElasticIndexer elasticIndexer;

    public SlettAktivitetServlet(AktivitetDAO database, ElasticIndexer elasticIndexer) {
        this.database = database;
        this.elasticIndexer = elasticIndexer;
    }

    @Override
    @SneakyThrows
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        if (AuthorizationUtils.isBasicAuthAuthorized(req)) {

            String aktivitetId = req.getParameter("aktivitetId");
            AktoerId aktoerId = database.getAktoerId(aktivitetId);

            database.deleteById(aktivitetId);
            elasticIndexer.indekser(aktoerId);

            resp.getWriter().write(String.format("Slettet aktivitet %s for bruker %s", aktivitetId, aktoerId));
            resp.setStatus(200);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
