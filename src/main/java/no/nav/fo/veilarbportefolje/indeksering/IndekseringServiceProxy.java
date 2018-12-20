package no.nav.fo.veilarbportefolje.indeksering;

import no.nav.fo.veilarbportefolje.domene.*;
import no.nav.fo.veilarbportefolje.util.MetricsUtils;
import no.nav.sbl.featuretoggle.unleash.UnleashService;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class IndekseringServiceProxy implements IndekseringService {

    private SolrService solrService;

    private ElasticSearchService elasticSearchService;

    private UnleashService featureToggle;

    @Inject
    public IndekseringServiceProxy(SolrService solrService, ElasticSearchService elasticSearchService, UnleashService featureToggle) {
        this.solrService = solrService;
        this.elasticSearchService = elasticSearchService;
        this.featureToggle = featureToggle;
    }

    @Override
    public void hovedindeksering() {
        MetricsUtils.timed("solr.hovedindeksering", solrService::hovedindeksering);

        if (elasticSearchIsEnabled()) {
            MetricsUtils.timed("es.hovedindeksering", elasticSearchService::hovedindeksering);
        }
    }

    @Override
    public void deltaindeksering() {
        solrService.deltaindeksering();

        if (elasticSearchIsEnabled()) {
            elasticSearchService.deltaindeksering();
        }
    }

    @Override
    public BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg, Integer fra, Integer antall) {
        return solrService.hentBrukere(enhetId, veilederIdent, sortOrder, sortField, filtervalg, fra, antall);
    }

    @Override
    public BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg) {
        return solrService.hentBrukere(enhetId, veilederIdent, sortOrder, sortField, filtervalg);
    }

    @Override
    public void slettBruker(String fnr) {
        solrService.slettBruker(fnr);

        if (elasticSearchIsEnabled()) {
            elasticSearchService.slettBruker(fnr);
        }
    }

    @Override
    public void indekserBrukerdata(PersonId personId) {
        solrService.indekserBrukerdata(personId);
    }

    @Override
    public StatusTall hentStatusTallForPortefolje(String enhet) {
        return solrService.hentStatusTallForPortefolje(enhet);
    }

    @Override
    public FacetResults hentPortefoljestorrelser(String enhetId) {
        return solrService.hentPortefoljestorrelser(enhetId);
    }

    @Override
    public StatusTall hentStatusTallForVeileder(String enhet, String veilederIdent) {
        return solrService.hentStatusTallForVeileder(enhet, veilederIdent);
    }

    @Override
    public List<Bruker> hentBrukereMedArbeidsliste(VeilederId veilederId, String enhet) {
        return solrService.hentBrukereMedArbeidsliste(veilederId, enhet);
    }

    @Override
    public void indekserAsynkront(AktoerId aktoerId) {
        solrService.indekserAsynkront(aktoerId);

        if (elasticSearchIsEnabled()) {
            elasticSearchService.indekserAsynkront(aktoerId);
        }
    }

    @Override
    public void indekserBrukere(List<PersonId> personIds) {
        solrService.indekserBrukere(personIds);
    }

    private boolean elasticSearchIsEnabled() {
        return featureToggle.isEnabled("veilarbportefolje.elasticsearch");
    }
}
