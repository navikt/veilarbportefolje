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
import no.nav.pto.veilarbportefolje.opensearch.domene.*;
import no.nav.pto.veilarbportefolje.opensearch.domene.Avvik14aStatistikkResponse.Avvik14aStatistikkAggregation.Avvik14aStatistikkFilter.Avvik14aStatistikkBuckets;
import no.nav.pto.veilarbportefolje.opensearch.domene.StatustallResponse.StatustallAggregation.StatustallFilter.StatustallBuckets;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.avvik14aVedtak.Avvik14aVedtak;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtaksstotteClient;
import org.apache.commons.lang3.StringUtils;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.search.aggregations.bucket.filter.FiltersAggregator;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static java.lang.Integer.parseInt;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.opensearch.BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ;
import static no.nav.pto.veilarbportefolje.opensearch.BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchQueryBuilder.*;
import static org.opensearch.index.query.QueryBuilders.*;
import static org.opensearch.search.aggregations.AggregationBuilders.filters;

@Service
@RequiredArgsConstructor
public class OpensearchService {
    private final RestHighLevelClient restHighLevelClient;
    private final VeilarbVeilederClient veilarbVeilederClient;
    private final VedtaksstotteClient vedtaksstotteClient;
    private final IndexName indexName;
    private final DefaultUnleash defaultUnleash;
    private final AuthService authService;

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
            filtervalg.ferdigfilterListe.forEach(
                    filter -> boolQuery.filter(leggTilFerdigFilter(filter, veiledereMedTilgangTilEnhet))
            );

            leggTilManuelleFilter(boolQuery, filtervalg);
        }

        if (filtervalg.harBarnUnder18AarFilter()) {
            if (filtervalg.barnUnder18AarAlder != null && !filtervalg.barnUnder18AarAlder.isEmpty()) {
                String[] fraTilAlder = filtervalg.barnUnder18AarAlder.getFirst().split("-");
                int fraAlder = parseInt(fraTilAlder[0]);
                int tilAlder = parseInt(fraTilAlder[1]);
                leggTilBarnAlderFilter(boolQuery, authService.harVeilederTilgangTilKode6(), authService.harVeilederTilgangTilKode7(), fraAlder, tilAlder);
            } else if (filtervalg.barnUnder18Aar != null && !filtervalg.barnUnder18Aar.isEmpty()) {
                leggTilBarnFilter(filtervalg, boolQuery, authService.harVeilederTilgangTilKode6(), authService.harVeilederTilgangTilKode7());
            }
        }

        searchSourceBuilder.query(boolQuery);

        if (FeatureToggle.brukFilterForBrukerinnsynTilganger(defaultUnleash)) {
            leggTilBrukerinnsynTilgangFilter(boolQuery, authService.hentVeilederBrukerInnsynTilganger(), BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ);
        }

        sorterQueryParametere(sorteringsrekkefolge, sorteringsfelt, searchSourceBuilder, filtervalg, authService.hentVeilederBrukerInnsynTilganger());

        OpensearchResponse response = search(searchSourceBuilder, indexName.getValue(), OpensearchResponse.class);
        int totalHits = response.hits().getTotal().getValue();

        List<Bruker> brukere = response.hits().getHits().stream()
                .map(Hit::get_source)
                .map(oppfolgingsBruker -> mapOppfolgingsBrukerTilBruker(oppfolgingsBruker, veiledereMedTilgangTilEnhet, filtervalg, enhetId))
                .collect(toList());

        return new BrukereMedAntall(totalHits, brukere);
    }

    public Statustall hentStatustallForVeilederPortefolje(String veilederId, String enhetId) {
        BoolQueryBuilder veilederOgEnhetQuery = boolQuery()
                .must(termQuery("oppfolging", true))
                .must(termQuery("enhet_id", enhetId))
                .must(termQuery("veileder_id", veilederId));

        if (FeatureToggle.brukFilterForBrukerinnsynTilganger(defaultUnleash)) {
            leggTilBrukerinnsynTilgangFilter(veilederOgEnhetQuery, authService.hentVeilederBrukerInnsynTilganger(), BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ);
        }

        SearchSourceBuilder request = byggStatustallQuery(veilederOgEnhetQuery, emptyList());

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
            leggTilBrukerinnsynTilgangFilter(enhetQuery, brukerInnsynTilganger, brukerinnsynTilgangFilterType);
        }

        SearchSourceBuilder request = byggStatustallQuery(enhetQuery, veilederPaaEnhet);

        StatustallResponse response = search(request, indexName.getValue(), StatustallResponse.class);
        StatustallBuckets buckets = response.getAggregations().getFilters().getBuckets();
        return new Statustall(buckets);
    }

    public FacetResults hentPortefoljestorrelser(String enhetId) {
        SearchSourceBuilder request = byggPortefoljestorrelserQuery(enhetId);
        PortefoljestorrelserResponse response = search(request, indexName.getValue(), PortefoljestorrelserResponse.class);
        List<Bucket> buckets = response.getAggregations().getFilter().getSterms().getBuckets();
        return new FacetResults(buckets);
    }

    public Avvik14aStatistikk hentAvvik14aStatistikk() {
        FiltersAggregator.KeyedFilter[] filtre = new FiltersAggregator.KeyedFilter[]{
                new FiltersAggregator.KeyedFilter(
                        "innsatsgruppeUlik",
                        boolQuery()
                                .must(matchQuery("avvik14aVedtak", Avvik14aVedtak.INNSATSGRUPPE_ULIK))
                ),
                new FiltersAggregator.KeyedFilter(
                        "hovedmaalUlik",
                        boolQuery()
                                .must(matchQuery("avvik14aVedtak", Avvik14aVedtak.HOVEDMAAL_ULIK))
                ),
                new FiltersAggregator.KeyedFilter(
                        "innsatsgruppeOgHovedmaalUlik",
                        boolQuery()
                                .must(matchQuery("avvik14aVedtak", Avvik14aVedtak.INNSATSGRUPPE_OG_HOVEDMAAL_ULIK))
                ),
                new FiltersAggregator.KeyedFilter(
                        "innsatsgruppeManglerINyKilde",
                        boolQuery()
                                .must(matchQuery("avvik14aVedtak", Avvik14aVedtak.INNSATSGRUPPE_MANGLER_I_NY_KILDE))
                )
        };

        SearchSourceBuilder request = new SearchSourceBuilder()
                .size(0)
                .aggregation(filters("avvik14astatistikk", filtre));

        Avvik14aStatistikkResponse response = search(request, indexName.getValue(), Avvik14aStatistikkResponse.class);
        Avvik14aStatistikkBuckets buckets = response.getAggregations().getFilters().getBuckets();
        return Avvik14aStatistikk.of(buckets);
    }

    @SneakyThrows
    public <T> T search(SearchSourceBuilder searchSourceBuilder, String indexAlias, Class<T> clazz) {
        SearchRequest request = new SearchRequest()
                .indices(indexAlias)
                .source(searchSourceBuilder);

        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        return JsonUtils.fromJson(response.toString(), clazz);
    }

    private Bruker mapOppfolgingsBrukerTilBruker(OppfolgingsBruker oppfolgingsBruker, List<String> aktiveVeilederePaEnhet, Filtervalg filtervalg, String enhetId) {
        Bruker bruker = Bruker.of(oppfolgingsBruker, erUfordelt(oppfolgingsBruker, aktiveVeilederePaEnhet), erVedtakstottePilotPa(EnhetId.of(enhetId)));

        if (filtervalg.harAktiviteterForenklet()) {
            bruker.kalkulerNesteUtlopsdatoAvValgtAktivitetFornklet(filtervalg.aktiviteterForenklet);
        }
        if (filtervalg.harAlleAktiviteterFilter()) {
            bruker.leggTilUtlopsdatoForAktiviteter(filtervalg.alleAktiviteter);
        }
        if (filtervalg.harAktivitetFilter()) {
            bruker.kalkulerNesteUtlopsdatoAvValgtAktivitetAvansert(filtervalg.aktiviteter);
        }
        if (filtervalg.harSisteEndringFilter()) {
            bruker.kalkulerSisteEndring(oppfolgingsBruker.getSiste_endringer(), filtervalg.sisteEndringKategori);
        }

        return bruker;
    }


    private boolean erUfordelt(OppfolgingsBruker oppfolgingsBruker, List<String> veiledereMedTilgangTilEnhet) {
        boolean harVeilederPaaSammeEnhet = oppfolgingsBruker.getVeileder_id() != null && veiledereMedTilgangTilEnhet.contains(oppfolgingsBruker.getVeileder_id());
        return !harVeilederPaaSammeEnhet;
    }

    private boolean erVedtakstottePilotPa(EnhetId enhetId) {
        return vedtaksstotteClient.erVedtakstottePilotPa(enhetId);
    }
}
