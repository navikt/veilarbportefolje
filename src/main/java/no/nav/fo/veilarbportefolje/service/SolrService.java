package no.nav.fo.veilarbportefolje.service;

import no.nav.fo.veilarbportefolje.domene.*;

import java.util.List;
import java.util.Optional;

public interface SolrService {
    void hovedindeksering();

    void deltaindeksering();

    BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg, Integer fra, Integer antall);

    BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg);

    void slettBruker(String fnr);

    void slettBruker(PersonId personid);

    void indekserBrukerdata(PersonId personId);

    void commit();

    StatusTall hentStatusTallForPortefolje(String enhet);

    FacetResults hentPortefoljestorrelser(String enhetId);

    StatusTall hentStatusTallForVeileder(String enhet, String veilederIdent);

    List<Bruker> hentBrukereMedArbeidsliste(VeilederId veilederId, String enhet);

    void indekserAsynkront(AktoerId aktoerId);

    void indekserBrukere(List<PersonId> personIds);

}
