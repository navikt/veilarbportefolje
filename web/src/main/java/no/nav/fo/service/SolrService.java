package no.nav.fo.service;

import io.vavr.control.Either;
import io.vavr.control.Try;
import no.nav.fo.domene.*;
import org.apache.solr.client.solrj.response.UpdateResponse;

import java.util.List;
import java.util.Optional;

public interface SolrService {
    void hovedindeksering();

    void deltaindeksering();

    Try<UpdateResponse> commit();

    List<Bruker> hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg);

    Either<Throwable, List<Bruker>> query(String query);

    void slettBruker(String fnr);

    void slettBruker(PersonId personId);

    void slettBruker(Fnr fnr);

    void indekserBrukerdata(PersonId personId);

    void indekserBrukerdata(AktoerId aktoerId);

    StatusTall hentStatusTallForPortefolje(String enhet);

    FacetResults hentPortefoljestorrelser(String enhetId);

    StatusTall hentStatusTallForVeileder(String enhet, String veilederIdent);

    Try<List<Bruker>> hentBrukereMedArbeidsliste(VeilederId veilederId, String enhet);

    String byggQueryString(String enhetId, Optional<String> veilederIdent);

}
