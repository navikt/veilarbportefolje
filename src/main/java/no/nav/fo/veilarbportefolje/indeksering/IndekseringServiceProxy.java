package no.nav.fo.veilarbportefolje.indeksering;

import no.nav.fo.veilarbportefolje.domene.*;
import no.nav.fo.veilarbportefolje.util.MetricsUtils;
import no.nav.sbl.featuretoggle.unleash.UnleashService;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class IndekseringServiceProxy implements IndekseringService {

    private SolrService solrService;

    private ElasticIndexer elasticIndexer;

    private UnleashService featureToggle;

    @Inject
    public IndekseringServiceProxy(SolrService solrService, ElasticIndexer elasticIndexer, UnleashService featureToggle) {
        this.solrService = solrService;
        this.elasticIndexer = elasticIndexer;
        this.featureToggle = featureToggle;
    }

    @Override
    public void hovedindeksering() {
        MetricsUtils.timed("solr.hovedindeksering", solrService::hovedindeksering);

        if (skrivDataTilElasticsearchIsEnabled()) {
            MetricsUtils.timed("es.hovedindeksering", elasticIndexer::hovedindeksering);
        }
    }

    @Override
    public void deltaindeksering() {
        MetricsUtils.timed("solr.deltaindeksering", solrService::deltaindeksering);

        if (skrivDataTilElasticsearchIsEnabled()) {
            MetricsUtils.timed("es.deltaindeksering", elasticIndexer::deltaindeksering);
        }
    }

    @Override
    public BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg, Integer fra, Integer antall) {

        if (hentDataFraElasticSearchIsEnabled()) {
            return elasticIndexer.hentBrukere(enhetId, veilederIdent, sortOrder, sortField, filtervalg, fra, antall);
        } else {
            return solrService.hentBrukere(enhetId, veilederIdent, sortOrder, sortField, filtervalg, fra, antall);
        }

    }

    @Override
    public BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg) {
        if (hentDataFraElasticSearchIsEnabled()) {
            return elasticIndexer.hentBrukere(enhetId, veilederIdent, sortOrder, sortField, filtervalg);
        } else {
            return solrService.hentBrukere(enhetId, veilederIdent, sortOrder, sortField, filtervalg);
        }
    }

    @Override
    public StatusTall hentStatusTallForPortefolje(String enhet) {
        if (hentDataFraElasticSearchIsEnabled()) {
            return elasticIndexer.hentStatusTallForPortefolje(enhet);
        } else {
            return solrService.hentStatusTallForPortefolje(enhet);
        }
    }

    @Override
    public FacetResults hentPortefoljestorrelser(String enhetId) {
        if (hentDataFraElasticSearchIsEnabled()) {
            return elasticIndexer.hentPortefoljestorrelser(enhetId);
        } else {
            return solrService.hentPortefoljestorrelser(enhetId);
        }
    }

    @Override
    public StatusTall hentStatusTallForVeileder(String enhet, String veilederIdent) {
        if (hentDataFraElasticSearchIsEnabled()) {
            return elasticIndexer.hentStatusTallForVeileder(enhet, veilederIdent);
        } else {
            return solrService.hentStatusTallForVeileder(enhet, veilederIdent);
        }
    }

    @Override
    public List<Bruker> hentBrukereMedArbeidsliste(VeilederId veilederId, String enhet) {
        if (hentDataFraElasticSearchIsEnabled()) {
            return elasticIndexer.hentBrukereMedArbeidsliste(veilederId, enhet);
        } else {
            return solrService.hentBrukereMedArbeidsliste(veilederId, enhet);
        }
    }

    @Override
    public void indekserAsynkront(AktoerId aktoerId) {
        solrService.indekserAsynkront(aktoerId);

        if (skrivDataTilElasticsearchIsEnabled()) {
            elasticIndexer.indekserAsynkront(aktoerId);
        }
    }

    @Override
    public void indekserBrukere(List<PersonId> personIds) {
        solrService.indekserBrukere(personIds);

        if (skrivDataTilElasticsearchIsEnabled()) {
            elasticIndexer.indekserBrukere(personIds);
        }
    }

    private boolean skrivDataTilElasticsearchIsEnabled() {
        return true;
//        return featureToggle.isEnabled("veilarbportefolje.elasticsearch");
    }
    private boolean hentDataFraElasticSearchIsEnabled() {
        return true;
//        return featureToggle.isEnabled("veilarbportefolje.hent_data_fra_es");
    }

}
