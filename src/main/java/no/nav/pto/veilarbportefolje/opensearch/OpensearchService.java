package no.nav.pto.veilarbportefolje.opensearch;

import lombok.SneakyThrows;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.domene.Bruker;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.FacetResults;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.domene.StatusTall;
import no.nav.pto.veilarbportefolje.opensearch.domene.Bucket;
import no.nav.pto.veilarbportefolje.opensearch.domene.OpensearchResponse;
import no.nav.pto.veilarbportefolje.opensearch.domene.Hit;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljestorrelserResponse;
import no.nav.pto.veilarbportefolje.opensearch.domene.StatustallResponse;
import no.nav.pto.veilarbportefolje.opensearch.domene.StatustallResponse.StatustallAggregation.StatustallFilter.StatustallBuckets;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.util.VedtakstottePilotRequest;
import org.apache.commons.lang3.StringUtils;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchQueryBuilder.byggArbeidslisteQuery;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchQueryBuilder.byggPortefoljestorrelserQuery;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchQueryBuilder.byggStatusTallForEnhetQuery;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchQueryBuilder.byggStatusTallForVeilederQuery;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchQueryBuilder.leggTilFerdigFilter;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchQueryBuilder.leggTilManuelleFilter;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchQueryBuilder.sorterPaaNyForEnhet;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchQueryBuilder.sorterQueryParametere;
import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.matchQuery;
import static org.opensearch.index.query.QueryBuilders.termQuery;

@Service
public class OpensearchService {
    private final RestHighLevelClient restHighLevelClient;
    private final VeilarbVeilederClient veilarbVeilederClient;
    private final VedtakstottePilotRequest vedtakstottePilotRequest;
    private final IndexName indexName;
    private final UnleashService unleashService;

    @Autowired
    public OpensearchService(RestHighLevelClient restHighLevelClient, VeilarbVeilederClient veilarbVeilederClient, IndexName indexName, VedtakstottePilotRequest vedtakstottePilotRequest, UnleashService unleashService) {
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

        OpensearchResponse response = search(searchSourceBuilder, indexName.getValue(), OpensearchResponse.class);
        int totalHits = response.hits().getTotal().getValue();

        List<Bruker> brukere = response.hits().getHits().stream()
                .map(Hit::get_source)
                .map(oppfolgingsBruker -> setNyForEnhet(oppfolgingsBruker, veiledereMedTilgangTilEnhet))
                .map(oppfolgingsBruker -> mapOppfolgingsBrukerTilBruker(oppfolgingsBruker, filtervalg, enhetId))
                .collect(toList());

        return new BrukereMedAntall(totalHits, brukere);
    }

    public List<Bruker> hentBrukereMedArbeidsliste(String veilederId, String enhetId) {

        SearchSourceBuilder request = byggArbeidslisteQuery(enhetId, veilederId);

        OpensearchResponse response = search(request, indexName.getValue(), OpensearchResponse.class);

        return response.hits().getHits().stream()
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
