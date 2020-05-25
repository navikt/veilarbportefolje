package no.nav.pto.veilarbportefolje.internal;

import lombok.SneakyThrows;
import no.nav.common.utils.CollectionUtils;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.profilering.ProfileringRepository;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class PopulerProfileringFraArenaServlet extends HttpServlet {
    private BrukerRepository brukerRepository;
    private ProfileringRepository profileringRepository;

    public PopulerProfileringFraArenaServlet(BrukerRepository brukerRepository, ProfileringRepository profileringRepository) {
        this.brukerRepository = brukerRepository;
        this.profileringRepository = profileringRepository;
    }

    @Override
    @SneakyThrows
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        if (AuthorizationUtils.isBasicAuthAuthorized(req)) {
            List<OppfolgingsBruker> brukareUtenVedtakFraArena =  brukerRepository.hentBrukereUtenProfilering();
            CollectionUtils.partition(brukareUtenVedtakFraArena, 1000).forEach(partion -> {
                partion.forEach(oppfolgingsBruker -> profileringRepository.insertProfileringFraArena(oppfolgingsBruker));
            });
            resp.setStatus(200);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
