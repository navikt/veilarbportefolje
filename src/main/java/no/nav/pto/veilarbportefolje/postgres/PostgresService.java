package no.nav.pto.veilarbportefolje.postgres;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.util.VedtakstottePilotRequest;

import java.util.List;
import java.util.Optional;


@RequiredArgsConstructor
public class PostgresService {
    private final VedtakstottePilotRequest vedtakstottePilotRequest;

    public BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg, Integer fra, Integer antall) {
        return null;
    }

    public List<Bruker> hentBrukereMedArbeidsliste(String veilederId, String enhetId) {
        return null;
    }

    public StatusTall hentStatusTallForVeileder(String veilederId, String enhetId) {
        boolean vedtakstottePilotErPa = this.erVedtakstottePilotPa(EnhetId.of(enhetId));
        return null;
    }

    public StatusTall hentStatusTallForEnhet(String enhetId) {
       return null;
    }

    public FacetResults hentPortefoljestorrelser(String enhetId) {
         return null;
    }

    private boolean erVedtakstottePilotPa(EnhetId enhetId) {
        return vedtakstottePilotRequest.erVedtakstottePilotPa(enhetId);
    }
}
