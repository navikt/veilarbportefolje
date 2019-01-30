package no.nav.fo.veilarbportefolje.indeksering;

import io.micrometer.core.instrument.LongTaskTimer;
import no.nav.fo.veilarbportefolje.domene.*;
import no.nav.metrics.MetricsFactory;
import no.nav.sbl.featuretoggle.unleash.UnleashService;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class IndekseringServiceProxy implements IndekseringService {

    private final LongTaskTimer solrTimerHoved;
    private final LongTaskTimer solrTimerDelta;

    private final LongTaskTimer esTimerHoved;
    private final LongTaskTimer esTimerDelta;

    private SolrService solrService;

    private ElasticSearchService elasticSearchService;

    private UnleashService featureToggle;

    @Inject
    public IndekseringServiceProxy(SolrService solrService, ElasticSearchService elasticSearchService, UnleashService featureToggle) {
        this.solrService = solrService;
        this.elasticSearchService = elasticSearchService;
        this.featureToggle = featureToggle;

        solrTimerHoved = LongTaskTimer.builder("solr_hovedindeksering").register(MetricsFactory.getMeterRegistry());
        solrTimerDelta = LongTaskTimer.builder("solr_deltaindeksering").register(MetricsFactory.getMeterRegistry());

        esTimerHoved = LongTaskTimer.builder("es_hovedindeksering").register(MetricsFactory.getMeterRegistry());
        esTimerDelta = LongTaskTimer.builder("es_deltaindeksering").register(MetricsFactory.getMeterRegistry());
    }

    @Override
    public void hovedindeksering() {

        solrTimerHoved.record(solrService::hovedindeksering);

        if (elasticSearchIsEnabled()) {
            esTimerHoved.record(elasticSearchService::hovedindeksering);
        }
    }

    @Override
    public void deltaindeksering() {
        solrTimerDelta.record(solrService::deltaindeksering);

        if (elasticSearchIsEnabled()) {
            esTimerDelta.record(elasticSearchService::deltaindeksering);
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

        if (elasticSearchIsEnabled()) {
            elasticSearchService.indekserBrukere(personIds);
        }
    }

    private boolean elasticSearchIsEnabled() {
        return featureToggle.isEnabled("veilarbportefolje.elasticsearch");
    }
}
