package no.nav.pto.veilarbportefolje.elastic;

import lombok.SneakyThrows;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.elastic.domene.*;
import no.nav.pto.veilarbportefolje.elastic.domene.StatustallResponse.StatustallAggregation.StatustallFilter.StatustallBuckets;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.elastic.ElasticQueryBuilder.*;
import static org.elasticsearch.index.query.QueryBuilders.*;

public class ElasticService {
    RestHighLevelClient restHighLevelClient;
    VeilarbVeilederClient veilarbVeilederClient;
    UnleashService unleashService;
    IndexName indexName;


    public ElasticService(RestHighLevelClient restHighLevelClient, VeilarbVeilederClient veilarbVeilederClient, UnleashService unleashService, IndexName indexName) {
        this.restHighLevelClient = restHighLevelClient;
        this.veilarbVeilederClient = veilarbVeilederClient;
        this.unleashService = unleashService;
        this.indexName = indexName;
    }

    public BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg, Integer fra, Integer antall) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        int from = Optional.ofNullable(fra).orElse(0);
        int size = Optional.ofNullable(antall).orElse(9999);

        searchSourceBuilder.from(from);
        searchSourceBuilder.size(size);

        BoolQueryBuilder boolQuery = boolQuery();
        boolQuery.must(matchQuery("enhet_id", enhetId));

        boolQuery.must(matchQuery("oppfolging", true));

        boolean kallesFraMinOversikt = veilederIdent.isPresent() && StringUtils.isNotBlank(veilederIdent.get());
        if (kallesFraMinOversikt) {
            boolQuery.filter(termQuery("veileder_id", veilederIdent.get()));
        }

        List<String> veiledereMedTilgangTilEnhet = veilarbVeilederClient.hentVeilederePaaEnhet(enhetId);

        if (filtervalg.harAktiveFilter()) {
            boolean erVedtakstottePilotPa = erVedtakstottePilotPa();
            filtervalg.ferdigfilterListe.forEach(
                    filter -> boolQuery.filter(leggTilFerdigFilter(filter, veiledereMedTilgangTilEnhet, erVedtakstottePilotPa))
            );

            leggTilManuelleFilter(boolQuery, filtervalg);
        }

        searchSourceBuilder.query(boolQuery);

        if (kallesFraMinOversikt) {
            searchSourceBuilder.sort("ny_for_veileder", SortOrder.DESC);
        } else {
            sorterPaaNyForEnhet(searchSourceBuilder, veiledereMedTilgangTilEnhet);
        }

        sorterQueryParametere(sortOrder, sortField, searchSourceBuilder, filtervalg);

        ElasticSearchResponse response = search(searchSourceBuilder, indexName.getValue(), ElasticSearchResponse.class);
        int totalHits = response.getHits().getTotal();

        List<Bruker> brukere = response.getHits().getHits().stream().map(Hit::get_source)
                        .map(oppfolgingsBruker -> setNyForEnhet(oppfolgingsBruker, veiledereMedTilgangTilEnhet))
                        .map(oppfolgingsBruker -> mapOppfolgingsBrukerTilBruker(oppfolgingsBruker, filtervalg.sisteEndringKategori))
                        .collect(toList());

        return new BrukereMedAntall(totalHits, brukere);
    }

    public List<Bruker> hentBrukereMedArbeidsliste(String veilederId, String enhetId) {

        SearchSourceBuilder request = byggArbeidslisteQuery(enhetId, veilederId);

        ElasticSearchResponse response = search(request, indexName.getValue(), ElasticSearchResponse.class);

        return response.getHits().getHits().stream()
                .map(hit -> Bruker.of(hit.get_source(), erVedtakstottePilotPa()))
                .collect(toList());
    }

    public StatusTall hentStatusTallForVeileder(String veilederId, String enhetId) {
        boolean vedtakstottePilotErPa = this.erVedtakstottePilotPa();

        SearchSourceBuilder request =
                byggStatusTallForVeilederQuery(enhetId, veilederId, emptyList(), vedtakstottePilotErPa);

        StatustallResponse response = search(request, indexName.getValue(), StatustallResponse.class);
        StatustallBuckets buckets = response.getAggregations().getFilters().getBuckets();
        return new StatusTall(buckets, vedtakstottePilotErPa);
    }

    public StatusTall hentStatusTallForEnhet(String enhetId) {
        List<String> veilederPaaEnhet = veilarbVeilederClient.hentVeilederePaaEnhet(enhetId);

        boolean vedtakstottePilotErPa = this.erVedtakstottePilotPa();

        SearchSourceBuilder request =
                byggStatusTallForEnhetQuery(enhetId, veilederPaaEnhet, vedtakstottePilotErPa);

        StatustallResponse response = search(request, indexName.getValue(), StatustallResponse.class);
        StatustallBuckets buckets = response.getAggregations().getFilters().getBuckets();
        return new StatusTall(buckets, vedtakstottePilotErPa);
    }

    public FacetResults hentPortefoljestorrelser(String enhetId) {
        SearchSourceBuilder request = byggPortefoljestorrelserQuery(enhetId);
        PortefoljestorrelserResponse response = search(request, indexName.getValue(), PortefoljestorrelserResponse.class);
        List<Bucket> buckets = response.getAggregations().getFilter().getSterms().getBuckets();
        return new FacetResults(buckets);
    }

    @SneakyThrows
    private <T> T search(SearchSourceBuilder searchSourceBuilder, String indexAlias, Class<T> clazz) {
        SearchRequest request = new SearchRequest()
                .indices(indexAlias)
                .source(searchSourceBuilder);

        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        return JsonUtils.fromJson(response.toString(), clazz);
    }

    private Bruker mapOppfolgingsBrukerTilBruker(OppfolgingsBruker oppfolgingsBruker, List<String> sisteEndringKategori) {
        if(sisteEndringKategori == null || sisteEndringKategori.isEmpty()) {
            return Bruker.of(oppfolgingsBruker, erVedtakstottePilotPa());
        }
        oppfolgingsBruker.kalkulerSisteEndring(sisteEndringKategori);
        return Bruker.of(oppfolgingsBruker, erVedtakstottePilotPa());
    }


    private OppfolgingsBruker setNyForEnhet(OppfolgingsBruker oppfolgingsBruker, List<String> veiledereMedTilgangTilEnhet) {
        boolean harVeilederPaaSammeEnhet = oppfolgingsBruker.getVeileder_id() != null && veiledereMedTilgangTilEnhet.contains(oppfolgingsBruker.getVeileder_id());
        return oppfolgingsBruker.setNy_for_enhet(!harVeilederPaaSammeEnhet);
    }


    private boolean erVedtakstottePilotPa() {
        return unleashService.isEnabled(FeatureToggle.VEDTAKSTOTTE_PILOT);
    }

}
