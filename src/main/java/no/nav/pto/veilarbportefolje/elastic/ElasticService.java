package no.nav.pto.veilarbportefolje.elastic;

import lombok.SneakyThrows;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.elastic.domene.*;
import no.nav.pto.veilarbportefolje.elastic.domene.StatustallResponse.StatustallAggregation.StatustallFilter.StatustallBuckets;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.util.VedtakstottePilotRequest;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.elastic.ElasticQueryBuilder.*;
import static org.elasticsearch.index.query.QueryBuilders.*;

public class ElasticService {
    private final RestHighLevelClient restHighLevelClient;
    private final VeilarbVeilederClient veilarbVeilederClient;
    private final VedtakstottePilotRequest vedtakstottePilotRequest;
    private final IndexName indexName;
    private final UnleashService unleashService;

    public ElasticService(RestHighLevelClient restHighLevelClient, VeilarbVeilederClient veilarbVeilederClient, IndexName indexName, VedtakstottePilotRequest vedtakstottePilotRequest, UnleashService unleashService) {
        this.restHighLevelClient = restHighLevelClient;
        this.veilarbVeilederClient = veilarbVeilederClient;
        this.indexName = indexName;
        this.vedtakstottePilotRequest = vedtakstottePilotRequest;
        this.unleashService = unleashService;
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

        List<String> veiledereMedTilgangTilEnhet = veilarbVeilederClient.hentVeilederePaaEnhet(EnhetId.of(enhetId));

        if (filtervalg.harAktiveFilter()) {
            boolean erVedtakstottePilotPa = erVedtakstottePilotPa(EnhetId.of(enhetId));
            filtervalg.ferdigfilterListe.forEach(
                    filter -> boolQuery.filter(leggTilFerdigFilter(filter, veiledereMedTilgangTilEnhet, erVedtakstottePilotPa))
            );

            leggTilManuelleFilter(boolQuery, filtervalg, unleashService);
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

        List<Bruker> brukere = response.getHits().getHits().stream()
                .map(Hit::get_source)
                .map(oppfolgingsBruker -> setNyForEnhet(oppfolgingsBruker, veiledereMedTilgangTilEnhet))
                .map(oppfolgingsBruker -> mapOppfolgingsBrukerTilBruker(oppfolgingsBruker, filtervalg, enhetId))
                .collect(toList());

        return new BrukereMedAntall(totalHits, brukere);
    }

    public List<Bruker> hentBrukereMedArbeidsliste(String veilederId, String enhetId) {

        SearchSourceBuilder request = byggArbeidslisteQuery(enhetId, veilederId);

        ElasticSearchResponse response = search(request, indexName.getValue(), ElasticSearchResponse.class);

        return response.getHits().getHits().stream()
                .map(hit -> Bruker.of(hit.get_source(), erVedtakstottePilotPa(EnhetId.of(enhetId))))
                .collect(toList());
    }

    public StatusTall hentStatusTallForVeileder(String veilederId, String enhetId) {
        boolean vedtakstottePilotErPa = this.erVedtakstottePilotPa(EnhetId.of(enhetId));

        SearchSourceBuilder request =
                byggStatusTallForVeilederQuery(enhetId, veilederId, emptyList(), vedtakstottePilotErPa);

        StatustallResponse response = search(request, indexName.getValue(), StatustallResponse.class);
        StatustallBuckets buckets = response.getAggregations().getFilters().getBuckets();
        return new StatusTall(buckets, vedtakstottePilotErPa);
    }

    public StatusTall hentStatusTallForEnhet(String enhetId) {
        List<String> veilederPaaEnhet = veilarbVeilederClient.hentVeilederePaaEnhet(EnhetId.of(enhetId));

        boolean vedtakstottePilotErPa = this.erVedtakstottePilotPa(EnhetId.of(enhetId));

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

    private Bruker mapOppfolgingsBrukerTilBruker(OppfolgingsBruker oppfolgingsBruker, Filtervalg filtervalg, String enhetId) {
        Bruker bruker = Bruker.of(oppfolgingsBruker, erVedtakstottePilotPa(EnhetId.of(enhetId)));

        if (filtervalg.harAktiviteterForenklet()) {
            bruker.kalkulerNesteUtlopsdatoAvValgtAktivitetFornklet(filtervalg.aktiviteterForenklet);
        }
        if (filtervalg.harAktivitetFilter()) {
            bruker.kalkulerNesteUtlopsdatoAvValgtAktivitetAvansert(filtervalg.aktiviteter);
        }
        if (filtervalg.harSisteEndringFilter()) {
            bruker.kalkulerSisteEndring(oppfolgingsBruker.getSiste_endringer(), filtervalg.sisteEndringKategori);
        }
        return bruker;
    }


    private OppfolgingsBruker setNyForEnhet(OppfolgingsBruker oppfolgingsBruker, List<String> veiledereMedTilgangTilEnhet) {
        boolean harVeilederPaaSammeEnhet = oppfolgingsBruker.getVeileder_id() != null && veiledereMedTilgangTilEnhet.contains(oppfolgingsBruker.getVeileder_id());
        return oppfolgingsBruker.setNy_for_enhet(!harVeilederPaaSammeEnhet);
    }


    private boolean erVedtakstottePilotPa(EnhetId enhetId) {
        return vedtakstottePilotRequest.erVedtakstottePilotPa(enhetId);
    }
}
