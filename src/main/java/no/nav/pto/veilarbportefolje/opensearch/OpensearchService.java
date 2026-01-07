package no.nav.pto.veilarbportefolje.opensearch;

import io.getunleash.DefaultUnleash;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.BrukerinnsynTilganger;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg;
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModell;
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModellMapper;
import no.nav.pto.veilarbportefolje.opensearch.domene.*;
import no.nav.pto.veilarbportefolje.opensearch.domene.StatustallResponse.StatustallAggregation.StatustallFilter.StatustallBuckets;
import org.apache.commons.lang3.StringUtils;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static java.lang.Integer.parseInt;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.opensearch.BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ;
import static no.nav.pto.veilarbportefolje.opensearch.BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ;
import static org.opensearch.index.query.QueryBuilders.*;

@Service
@RequiredArgsConstructor
public class OpensearchService {
    private final RestHighLevelClient restHighLevelClient;
    private final VeilarbVeilederClient veilarbVeilederClient;
    private final IndexName indexName;
    private final DefaultUnleash defaultUnleash;
    private final AuthService authService;
    private final OpensearchFilterQueryBuilder filterQueryBuilder = new OpensearchFilterQueryBuilder();
    private final OpensearchSortQueryBuilder sortQueryBuilder = new OpensearchSortQueryBuilder();

    public BrukereMedAntall hentBrukere(
            String enhetId,
            Optional<String> veilederIdent,
            Sorteringsrekkefolge sorteringsrekkefolge,
            Sorteringsfelt sorteringsfelt,
            Filtervalg filtervalg,
            Integer fra,
            Integer antall
    ) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        int from = Optional.ofNullable(fra).orElse(0);
        int size = Optional.ofNullable(antall).orElse(5000);

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
            filtervalg.getFerdigfilterListe().forEach(
                    filter -> boolQuery.filter(filterQueryBuilder.leggTilFerdigFilter(filter, veiledereMedTilgangTilEnhet))
            );

            filterQueryBuilder.leggTilManuelleFilter(boolQuery, filtervalg);
        }

        if (filtervalg.harBarnUnder18AarFilter()) {
            if (filtervalg.getBarnUnder18Aar() != null && !filtervalg.getBarnUnder18AarAlder().isEmpty()) {
                String[] fraTilAlder = filtervalg.getBarnUnder18AarAlder().getFirst().split("-");
                int fraAlder = parseInt(fraTilAlder[0]);
                int tilAlder = parseInt(fraTilAlder[1]);
                filterQueryBuilder.leggTilBarnAlderFilter(boolQuery, authService.harVeilederTilgangTilKode6(), authService.harVeilederTilgangTilKode7(), fraAlder, tilAlder);
            } else if (filtervalg.getBarnUnder18Aar() != null && !filtervalg.getBarnUnder18Aar().isEmpty()) {
                filterQueryBuilder.leggTilBarnFilter(filtervalg, boolQuery, authService.harVeilederTilgangTilKode6(), authService.harVeilederTilgangTilKode7());
            }
        }

        searchSourceBuilder.query(boolQuery);

        if (FeatureToggle.brukFilterForBrukerinnsynTilganger(defaultUnleash)) {
            filterQueryBuilder.leggTilBrukerinnsynTilgangFilter(boolQuery, authService.hentVeilederBrukerInnsynTilganger(), BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ);
        }

        sortQueryBuilder.sorterQueryParametere(sorteringsrekkefolge, sorteringsfelt, searchSourceBuilder, filtervalg, authService.hentVeilederBrukerInnsynTilganger());

        OpensearchResponse response = search(searchSourceBuilder, indexName.getValue(), OpensearchResponse.class);
        int totalHits = response.hits().getTotal().getValue();

        List<PortefoljebrukerFrontendModell> brukere = response.hits().getHits().stream()
                .map(Hit::get_source)
                .map(oppfolgingsBruker -> mapPortefoljebrukerFraOpensearchModellTilFrontendModell(oppfolgingsBruker, veiledereMedTilgangTilEnhet, filtervalg))
                .collect(toList());
        return new BrukereMedAntall(totalHits, brukere);
    }

    public Statustall hentStatustallForVeilederPortefolje(String veilederId, String enhetId) {
        BoolQueryBuilder veilederOgEnhetQuery = boolQuery()
                .must(termQuery("oppfolging", true))
                .must(termQuery("enhet_id", enhetId))
                .must(termQuery("veileder_id", veilederId));

        if (FeatureToggle.brukFilterForBrukerinnsynTilganger(defaultUnleash)) {
            filterQueryBuilder.leggTilBrukerinnsynTilgangFilter(veilederOgEnhetQuery, authService.hentVeilederBrukerInnsynTilganger(), BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ);
        }

        SearchSourceBuilder request = filterQueryBuilder.byggStatustallQuery(veilederOgEnhetQuery, emptyList());

        StatustallResponse response = search(request, indexName.getValue(), StatustallResponse.class);
        StatustallBuckets buckets = response.getAggregations().getFilters().getBuckets();
        return new Statustall(buckets);
    }

    public Statustall hentStatusTallForEnhetPortefolje(String enhetId, BrukerinnsynTilgangFilterType brukerinnsynTilgangFilterType) {
        BrukerinnsynTilganger brukerInnsynTilganger = authService.hentVeilederBrukerInnsynTilganger();

        if ((brukerInnsynTilganger.harAlle() && BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ == brukerinnsynTilgangFilterType) ||
                (!FeatureToggle.brukFilterForBrukerinnsynTilganger(defaultUnleash) && BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ == brukerinnsynTilgangFilterType)) {
            return new Statustall();
        }

        List<String> veilederPaaEnhet = veilarbVeilederClient.hentVeilederePaaEnhet(EnhetId.of(enhetId));

        BoolQueryBuilder enhetQuery = boolQuery()
                .must(termQuery("oppfolging", true))
                .must(termQuery("enhet_id", enhetId));

        if (FeatureToggle.brukFilterForBrukerinnsynTilganger(defaultUnleash)) {
            filterQueryBuilder.leggTilBrukerinnsynTilgangFilter(enhetQuery, brukerInnsynTilganger, brukerinnsynTilgangFilterType);
        }

        SearchSourceBuilder request = filterQueryBuilder.byggStatustallQuery(enhetQuery, veilederPaaEnhet);

        StatustallResponse response = search(request, indexName.getValue(), StatustallResponse.class);
        StatustallBuckets buckets = response.getAggregations().getFilters().getBuckets();
        return new Statustall(buckets);
    }

    public FacetResults hentPortefoljestorrelser(String enhetId) {
        SearchSourceBuilder request = filterQueryBuilder.byggPortefoljestorrelserQuery(enhetId);
        PortefoljestorrelserResponse response = search(request, indexName.getValue(), PortefoljestorrelserResponse.class);
        List<Bucket> buckets = response.getAggregations().getFilter().getSterms().getBuckets();
        return new FacetResults(buckets);
    }

    @SneakyThrows
    public <T> T search(SearchSourceBuilder searchSourceBuilder, String indexAlias, Class<T> clazz) {
        SearchRequest request = new SearchRequest()
                .indices(indexAlias)
                .source(searchSourceBuilder);

        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        return JsonUtils.fromJson(response.toString(), clazz);
    }

    private PortefoljebrukerFrontendModell mapPortefoljebrukerFraOpensearchModellTilFrontendModell(
            PortefoljebrukerOpensearchModell brukerOpensearchModell,
            List<String> aktiveVeilederePaEnhet,
            Filtervalg filtervalg
    ) {
        return PortefoljebrukerFrontendModellMapper.INSTANCE.toPortefoljebrukerFrontendModell(
                brukerOpensearchModell,
                erUfordelt(brukerOpensearchModell, aktiveVeilederePaEnhet),
                filtervalg
        );
    }


    private boolean erUfordelt(PortefoljebrukerOpensearchModell brukerOpensearchModell, List<String> veiledereMedTilgangTilEnhet) {
        boolean harVeilederPaaSammeEnhet = brukerOpensearchModell.getVeileder_id() != null && veiledereMedTilgangTilEnhet.contains(brukerOpensearchModell.getVeileder_id());
        return !harVeilederPaaSammeEnhet;
    }
}
