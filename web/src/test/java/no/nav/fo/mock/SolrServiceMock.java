package no.nav.fo.mock;

import io.vavr.control.Either;
import io.vavr.control.Try;
import no.nav.fo.domene.*;
import no.nav.fo.service.SolrService;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

public class SolrServiceMock implements SolrService {
    @Override
    public void hovedindeksering() {
    }

    @Override
    public void deltaindeksering() {

    }

    @Override
    public Try<UpdateResponse> commit() {
        return Try.success(new UpdateResponse());
    }

    @Override
    public BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg, Integer fra, Integer antall) {
        return new BrukereMedAntall(0, emptyList());
    }

    @Override
    public BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg) {
        return new BrukereMedAntall(0, emptyList());
    }

    @Override
    public Either<Throwable, List<Bruker>> query(String query) {
        return Either.right(emptyList());
    }

    @Override
    public void slettBruker(String fnr) {

    }

    @Override
    public void slettBruker(PersonId personId) {

    }

    @Override
    public void slettBrukere(List<PersonId> personId) {

    }

    @Override
    public void indekserBrukerdata(PersonId personId) {

    }

    @Override
    public void indekserBrukerdata(AktoerId aktoerId) {

    }

    @Override
    public void indekserDokumenter(List<SolrInputDocument> personIds) {

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
    public Try<List<Bruker>> hentBrukereMedArbeidsliste(VeilederId veilederId, String enhet) {
        return null;
    }

    @Override
    public String byggQueryString(String enhetId, Optional<String> veilederIdent) {
        return null;
    }

    @Override
    public void populerIndeksForPersonids(List<PersonId> personIds) {

    }
}
