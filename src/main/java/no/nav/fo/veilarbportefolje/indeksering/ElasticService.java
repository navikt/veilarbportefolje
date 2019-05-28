package no.nav.fo.veilarbportefolje.indeksering;

import lombok.SneakyThrows;
import no.nav.fo.veilarbportefolje.domene.*;
import no.nav.fo.veilarbportefolje.indeksering.domene.*;
import no.nav.fo.veilarbportefolje.indeksering.domene.StatustallResponse.StatustallAggregation.StatustallFilter.StatustallBuckets;
import no.nav.fo.veilarbportefolje.service.PepClient;
import no.nav.fo.veilarbportefolje.service.VeilederService;
import no.nav.json.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.sort;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbportefolje.indeksering.ElasticQueryBuilder.*;
import static org.elasticsearch.index.query.QueryBuilders.*;

@Component
public class ElasticService {
    RestHighLevelClient client;
    PepClient pepClient;
    VeilederService veilederService;

    public ElasticService(RestHighLevelClient client, PepClient pepClient, VeilederService veilederService) {
        this.client = client;
        this.pepClient = pepClient;
        this.veilederService = veilederService;
    }

    public BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg, Integer fra, Integer antall, String indexAlias) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        int from = Optional.ofNullable(fra).orElse(0);
        int size = Optional.ofNullable(antall).orElse(9999);
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(size);

        BoolQueryBuilder boolQuery = boolQuery();
        boolQuery.must(matchQuery("enhet_id", enhetId));

        boolean kallesFraMinOversikt = veilederIdent.isPresent() && StringUtils.isNotBlank(veilederIdent.get());
        if (kallesFraMinOversikt) {
            boolQuery.filter(termQuery("veileder_id", veilederIdent.get()));
        }

        List<String> veiledereMedTilgangTilEnhet = veilederService.getIdenter(enhetId).stream()
                .map(VeilederId::getVeilederId)
                .collect(toList());

        if (filtervalg.harAktiveFilter()) {

            filtervalg.ferdigfilterListe.forEach(
                    filter -> boolQuery.filter(leggTilFerdigFilter(filter, veiledereMedTilgangTilEnhet))
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

        ElasticSearchResponse response = search(searchSourceBuilder, indexAlias, ElasticSearchResponse.class);
        int totalHits = response.getHits().getTotal();

        List<Bruker> brukere = response.getHits().getHits().stream()
                .map(Hit::get_source)
                .map(oppfolgingsBruker -> setNyForEnhet(oppfolgingsBruker, veiledereMedTilgangTilEnhet))
                .map(Bruker::of)
                .collect(toList());

        return new BrukereMedAntall(totalHits, brukere);
    }

    public List<Bruker> hentBrukereMedArbeidsliste(String veilederId, String enhetId, String indexAlias) {
        SearchSourceBuilder request = byggArbeidslisteQuery(enhetId, veilederId);
        ElasticSearchResponse response = search(request, indexAlias, ElasticSearchResponse.class);

        return response.getHits().getHits().stream()
                .map(hit -> Bruker.of(hit.get_source()))
                .collect(toList());
    }

    public StatusTall hentStatusTallForVeileder(String veilederId, String enhetId, String indexAlias) {
        SearchSourceBuilder request = byggStatusTallForVeilederQuery(enhetId, veilederId, emptyList());
        StatustallResponse response = search(request, indexAlias, StatustallResponse.class);
        StatustallBuckets buckets = response.getAggregations().getFilters().getBuckets();
        return new StatusTall(buckets);
    }

    public StatusTall hentStatusTallForEnhet(String enhetId, String indexAlias) {
        List<String> veilederPaaEnhet = veilederService.getIdenter(enhetId).stream()
                .map(VeilederId::toString)
                .collect(toList());

        SearchSourceBuilder request = byggStatusTallForEnhetQuery(enhetId, veilederPaaEnhet);
        StatustallResponse response = search(request, indexAlias, StatustallResponse.class);
        StatustallBuckets buckets = response.getAggregations().getFilters().getBuckets();
        return new StatusTall(buckets);
    }

    public FacetResults hentPortefoljestorrelser(String enhetId, String indexAlias) {
        SearchSourceBuilder request = byggPortefoljestorrelserQuery(enhetId);
        PortefoljestorrelserResponse response = search(request, indexAlias, PortefoljestorrelserResponse.class);
        List<Bucket> buckets = response.getAggregations().getFilter().getSterms().getBuckets();
        return new FacetResults(buckets);
    }

    @SneakyThrows
    private <T> T search(SearchSourceBuilder searchSourceBuilder, String indexAlias, Class<T> clazz) {
        SearchRequest request = new SearchRequest()
                .indices(indexAlias)
                .source(searchSourceBuilder);

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        return JsonUtils.fromJson(response.toString(), clazz);
    }

    private OppfolgingsBruker setNyForEnhet(OppfolgingsBruker oppfolgingsBruker, List<String> veiledereMedTilgangTilEnhet) {
        boolean harVeilederPaaSammeEnhet = veiledereMedTilgangTilEnhet.contains(oppfolgingsBruker.getVeileder_id());
        return oppfolgingsBruker.setNy_for_enhet(!harVeilederPaaSammeEnhet);
    }
}
