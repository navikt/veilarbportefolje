package no.nav.fo.veilarbportefolje.indeksering;

import no.nav.fo.veilarbportefolje.domene.*;

import java.util.List;
import java.util.Optional;

public interface IndekseringService {
    void hovedindeksering();

    void deltaindeksering();

    BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg, Integer fra, Integer antall);

    BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg);

    StatusTall hentStatusTallForPortefolje(String enhet);

    FacetResults hentPortefoljestorrelser(String enhetId);

    StatusTall hentStatusTallForVeileder(String enhet, String veilederIdent);

    List<Bruker> hentBrukereMedArbeidsliste(VeilederId veilederId, String enhet);

    void indekserAsynkront(AktoerId aktoerId);

    void indekserBrukere(List<PersonId> personIds);

}
