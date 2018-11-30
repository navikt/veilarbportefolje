package no.nav.fo.veilarbportefolje.indeksering;

import no.nav.fo.veilarbportefolje.domene.*;

import java.util.List;
import java.util.Optional;

public class ElasticSearchService implements IndekseringService{
    @Override
    public void hovedindeksering() {

    }

    @Override
    public void deltaindeksering() {

    }

    @Override
    public BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg, Integer fra, Integer antall) {
        return null;
    }

    @Override
    public BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg) {
        return null;
    }

    @Override
    public void slettBruker(String fnr) {

    }

    @Override
    public void slettBruker(PersonId personid) {

    }

    @Override
    public void indekserBrukerdata(PersonId personId) {

    }

    @Override
    public void commit() {

    }

    @Override
    public StatusTall hentStatusTallForPortefolje(String enhet) {
        return null;
    }

    @Override
    public FacetResults hentPortefoljestorrelser(String enhetId) {
        return null;
    }

    @Override
    public StatusTall hentStatusTallForVeileder(String enhet, String veilederIdent) {
        return null;
    }

    @Override
    public List<Bruker> hentBrukereMedArbeidsliste(VeilederId veilederId, String enhet) {
        return null;
    }

    @Override
    public void indekserAsynkront(AktoerId aktoerId) {

    }

    @Override
    public void indekserBrukere(List<PersonId> personIds) {

    }
}
