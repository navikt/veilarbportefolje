package no.nav.fo.service;

import io.vavr.control.Either;
import io.vavr.control.Try;
import no.nav.fo.domene.*;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

import java.util.List;
import java.util.Optional;

public interface SolrService {
    void hovedindeksering();

    void deltaindeksering();

    Try<UpdateResponse> commit();

    BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg, Integer fra, Integer antall);
    BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg);

    Either<Throwable, List<Bruker>> query(String query);

    void slettBruker(String fnr);

    void slettBruker(PersonId personId);

    void slettBrukere(List<PersonId> personId);

    void indekserBrukerdata(PersonId personId);

    void indekserBrukerdata(AktoerId aktoerId);

    void indekserDokumenter(List<SolrInputDocument> personIds);

    StatusTall hentStatusTallForPortefolje(String enhet);

    FacetResults hentPortefoljestorrelser(String enhetId);

    StatusTall hentStatusTallForVeileder(String enhet, String veilederIdent);

    Try<List<Bruker>> hentBrukereMedArbeidsliste(VeilederId veilederId, String enhet);

    String byggQueryString(String enhetId, Optional<String> veilederIdent);

    void indekserAsynkront(AktoerId aktoerId);

    void indekserBrukere(List<PersonId> personIds);

}
