package no.nav.pto.veilarbportefolje.opensearch;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.BrukerInnsynTilganger;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.opensearch.domene.*;
import no.nav.pto.veilarbportefolje.opensearch.domene.StatustallResponse.StatustallAggregation.StatustallFilter.StatustallBuckets;
import no.nav.pto.veilarbportefolje.persononinfo.domene.Adressebeskyttelse;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtaksstotteClient;
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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchQueryBuilder.*;
import static org.opensearch.index.query.QueryBuilders.*;

@Service
@RequiredArgsConstructor
public class OpensearchService {
    private final RestHighLevelClient restHighLevelClient;
    private final VeilarbVeilederClient veilarbVeilederClient;
    private final VedtaksstotteClient vedtaksstotteClient;
    private final IndexName indexName;
    private final UnleashService unleashService;
    private final AuthService authService;

    public BrukereMedAntall hentBrukere(
            String enhetId,
            Optional<String> veilederIdent,
            String sortOrder,
            String sortField,
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
            boolean erVedtakstottePilotPa = erVedtakstottePilotPa(EnhetId.of(enhetId));
            filtervalg.ferdigfilterListe.forEach(
                    filter -> boolQuery.filter(leggTilFerdigFilter(filter, veiledereMedTilgangTilEnhet, erVedtakstottePilotPa))
            );

            leggTilManuelleFilter(boolQuery, filtervalg);
        }

        searchSourceBuilder.query(boolQuery);

        if (FeatureToggle.brukFilterForBrukerInnsynTilganger(unleashService)) {
            BrukerInnsynTilganger brukerInnsynTilganger = authService.hentVeilederBrukerInnsynTilganger();

            if (!brukerInnsynTilganger.tilgangTilAdressebeskyttelseStrengtFortrolig()) {
                boolQuery.mustNot(matchQuery("diskresjonskode", Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode));
            }

            if (!brukerInnsynTilganger.tilgangTilAdressebeskyttelseFortrolig()) {
                boolQuery.mustNot(matchQuery("diskresjonskode", Adressebeskyttelse.FORTROLIG.diskresjonskode));
            }

            if (!brukerInnsynTilganger.tilgangTilSkjerming()) {
                boolQuery.mustNot(matchQuery("egen_ansatt", true));
            }
        }

        sorterQueryParametere(sortOrder, sortField, searchSourceBuilder, filtervalg);

        OpensearchResponse response = search(searchSourceBuilder, indexName.getValue(), OpensearchResponse.class);
        int totalHits = response.hits().getTotal().getValue();

        List<Bruker> brukere = response.hits().getHits().stream()
                .map(Hit::get_source)
                .map(oppfolgingsBruker -> mapOppfolgingsBrukerTilBruker(oppfolgingsBruker, veiledereMedTilgangTilEnhet, filtervalg, enhetId))
                .collect(toList());

        return new BrukereMedAntall(totalHits, brukere);
    }

    public VeilederPortefoljeStatusTall hentStatusTallForVeileder(String veilederId, String enhetId) {
        boolean vedtakstottePilotErPa = this.erVedtakstottePilotPa(EnhetId.of(enhetId));

        SearchSourceBuilder request =
                byggStatusTallForVeilederQuery(enhetId, veilederId, emptyList(), vedtakstottePilotErPa);

        StatustallResponse response = search(request, indexName.getValue(), StatustallResponse.class);
        StatustallBuckets buckets = response.getAggregations().getFilters().getBuckets();
        return new VeilederPortefoljeStatusTall(buckets, vedtakstottePilotErPa);
    }

    public EnhetPortefoljeStatusTall hentStatusTallForEnhet(String enhetId) {
        List<String> veilederPaaEnhet = veilarbVeilederClient.hentVeilederePaaEnhet(EnhetId.of(enhetId));

        boolean vedtakstottePilotErPa = this.erVedtakstottePilotPa(EnhetId.of(enhetId));

        SearchSourceBuilder request =
                byggStatusTallForEnhetQuery(enhetId, veilederPaaEnhet, vedtakstottePilotErPa);

        StatustallResponse response = search(request, indexName.getValue(), StatustallResponse.class);
        StatustallBuckets buckets = response.getAggregations().getFilters().getBuckets();
        return new EnhetPortefoljeStatusTall(buckets, vedtakstottePilotErPa);
    }

    public FacetResults hentPortefoljestorrelser(String enhetId) {
        SearchSourceBuilder request = byggPortefoljestorrelserQuery(enhetId);
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
