package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.Credentials;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.util.AuthorizationUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@WebServlet(
        name = "SlettAktivitet",
        description = "Sletter aktivitet fra databasen",
        urlPatterns = {"/internal/slett_aktivitet"}
)
public class SlettAktivitetServlet extends HttpServlet {

    private final AktivitetDAO database;
    private final ElasticIndexer elasticIndexer;
    private Credentials serviceUserCredentials;

    @Autowired
    public SlettAktivitetServlet(AktivitetDAO database, ElasticIndexer elasticIndexer, Credentials serviceUserCredentials) {
        this.database = database;
        this.elasticIndexer = elasticIndexer;
        this.serviceUserCredentials = serviceUserCredentials;
    }

    @Override
    @SneakyThrows
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        if (AuthorizationUtils.isBasicAuthAuthorized(req, serviceUserCredentials)) {

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
