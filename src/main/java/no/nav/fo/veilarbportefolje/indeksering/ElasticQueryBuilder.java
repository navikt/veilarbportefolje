package no.nav.fo.veilarbportefolje.indeksering;

import no.nav.fo.veilarbportefolje.domene.AktivitetFiltervalg;
import no.nav.fo.veilarbportefolje.domene.Brukerstatus;
import no.nav.fo.veilarbportefolje.domene.Filtervalg;
import no.nav.fo.veilarbportefolje.provider.rest.ValideringsRegler;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbportefolje.domene.AktivitetFiltervalg.JA;
import static no.nav.fo.veilarbportefolje.domene.AktivitetFiltervalg.NEI;
import static no.nav.fo.veilarbportefolje.indeksering.SolrUtils.TILTAK;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.search.aggregations.AggregationBuilders.filter;
import static org.elasticsearch.search.aggregations.AggregationBuilders.filters;
import static org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregator.KeyedFilter;
import static org.elasticsearch.search.sort.ScriptSortBuilder.ScriptSortType.NUMBER;
import static org.elasticsearch.search.sort.ScriptSortBuilder.ScriptSortType.STRING;
import static org.elasticsearch.search.sort.SortMode.MIN;

public class ElasticQueryBuilder {

    static void leggTilManuelleFilter(BoolQueryBuilder queryBuilder, Filtervalg filtervalg) {

        if (!filtervalg.alder.isEmpty()) {
            BoolQueryBuilder subQuery = boolQuery();
            filtervalg.alder.forEach(alder -> byggAlderQuery(alder, subQuery));
            queryBuilder.must(subQuery);
        }

        List<Integer> fodseldagIMndQuery = filtervalg.fodselsdagIMnd.stream().map(Integer::parseInt).collect(toList());

        byggManuellFilter(fodseldagIMndQuery, queryBuilder, "fodselsdag_i_mnd");
        byggManuellFilter(filtervalg.kjonn, queryBuilder, "kjonn");
        byggManuellFilter(filtervalg.innsatsgruppe, queryBuilder, "kvalifiseringsgruppekode");
        byggManuellFilter(filtervalg.hovedmal, queryBuilder, "hovedmaalkode");
        byggManuellFilter(filtervalg.formidlingsgruppe, queryBuilder, "formidlingsgruppekode");
        byggManuellFilter(filtervalg.servicegruppe, queryBuilder, "kvalifiseringsgruppekode");
        byggManuellFilter(filtervalg.veiledere, queryBuilder, "veileder_id");
        byggManuellFilter(filtervalg.manuellBrukerStatus, queryBuilder, "manuell_bruker");
        byggManuellFilter(filtervalg.tiltakstyper, queryBuilder, "tiltak");
        byggManuellFilter(filtervalg.rettighetsgruppe, queryBuilder, "rettighetsgruppekode");

        if (filtervalg.harYtelsefilter()) {

            BoolQueryBuilder subQuery = boolQuery();
            filtervalg.ytelse.underytelser.forEach(
                    ytelse -> queryBuilder.must(subQuery.should(matchQuery("ytelse", ytelse.name())))
            );

        }

        if (filtervalg.harAktivitetFilter()) {
            byggAktivitetFilterQuery(filtervalg, queryBuilder);
        }

        if (filtervalg.harNavnEllerFnrQuery()) {
            String query = filtervalg.navnEllerFnrQuery.trim().toLowerCase();
            if (isNumeric(query)) {
                queryBuilder.must(termQuery("fnr", query));
            } else {
                queryBuilder.must(termQuery("fullt_navn", query));
            }
        }
    }

    static List<BoolQueryBuilder> byggAktivitetFilterQuery(Filtervalg filtervalg, BoolQueryBuilder queryBuilder) {
        return filtervalg.aktiviteter.entrySet().stream()
                .map(
                        entry -> {
                            String navnPaaAktivitet = entry.getKey();
                            AktivitetFiltervalg valg = entry.getValue();

                            if (JA.equals(valg)) {
                                return queryBuilder.filter(matchQuery("aktiviteter", navnPaaAktivitet));
                            } else if (NEI.equals(valg) && TILTAK.equals(navnPaaAktivitet)) {
                                return queryBuilder.filter(boolQuery().mustNot(existsQuery("tiltak")));
                            } else if (NEI.equals(valg)) {
                                return queryBuilder.filter(boolQuery().mustNot(matchQuery("aktiviteter", navnPaaAktivitet)));
                            } else {
                                return queryBuilder;
                            }
                        }
                ).collect(toList());
    }

    static SearchSourceBuilder sorterQueryParametere(String sortOrder, String sortField, SearchSourceBuilder searchSourceBuilder, Filtervalg filtervalg) {
        SortOrder order = "ascending".equals(sortOrder) ? SortOrder.ASC : SortOrder.DESC;

        if ("ikke_satt".equals(sortField)) {
            searchSourceBuilder.sort("person_id", SortOrder.ASC);
            return searchSourceBuilder;
        }

        switch (sortField) {
            case "valgteaktiviteter":
                sorterValgteAktiviteter(filtervalg, searchSourceBuilder, order);
                break;
            case "iavtaltaktivitet":
                FieldSortBuilder builder = new FieldSortBuilder("aktivitet_utlopsdatoer")
                        .order(order)
                        .sortMode(MIN);

                searchSourceBuilder.sort(builder);
                break;
            case "fodselsnummer":
                searchSourceBuilder.sort("fnr.raw", order);
                break;
            case "utlopteaktiviteter":
                searchSourceBuilder.sort("nyesteutlopteaktivitet", order);
                break;
            case "arbeidslistefrist":
                searchSourceBuilder.sort("arbeidsliste_frist", order);
                break;
            case "aaprettighetsperiode":
                sorterAapRettighetsPeriode(searchSourceBuilder, order);
                break;
            default:
                defaultSort(sortField, searchSourceBuilder, order);
        }
        return searchSourceBuilder;
    }

    private static void defaultSort(String sortField, SearchSourceBuilder searchSourceBuilder, SortOrder order) {
        if (ValideringsRegler.sortFields.contains(sortField)) {
            searchSourceBuilder.sort(sortField, order);
        } else {
            throw new IllegalStateException();
        }
    }

    static SearchSourceBuilder sorterPaaNyForEnhet(SearchSourceBuilder builder, List<String> veilederePaaEnhet) {
        Script script = new Script(byggVeilederPaaEnhetScript(veilederePaaEnhet));
        ScriptSortBuilder scriptBuilder = new ScriptSortBuilder(script, STRING);
        builder.sort(scriptBuilder);
        return builder;
    }

    static SearchSourceBuilder sorterAapRettighetsPeriode(SearchSourceBuilder builder, SortOrder order) {
        Script script = new Script("Math.max(doc.aapmaxtiduke.value, doc.aapunntakukerigjen.value)");
        ScriptSortBuilder scriptBuilder = new ScriptSortBuilder(script, NUMBER);
        scriptBuilder.order(order);
        builder.sort(scriptBuilder);
        return builder;
    }

    static SearchSourceBuilder sorterValgteAktiviteter(Filtervalg filtervalg, SearchSourceBuilder builder, SortOrder order) {

        String datoFelter = filtervalg.aktiviteter.entrySet().stream()
                .filter(entry -> JA.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .map(aktivitet -> format("doc.aktivitet_%s_utlopsdato.date.getMillisOfDay()", aktivitet.toLowerCase()))
                .collect(joining(","));

        Script script = new Script(format("[%s].stream().mapToInt(Integer::intValue).min().orElse(Integer.MAX_VALUE)", datoFelter));

        ScriptSortBuilder scriptSortBuilder = new ScriptSortBuilder(script, NUMBER);
        scriptSortBuilder.order(order);
        builder.sort(scriptSortBuilder);
        return builder;
    }

    static QueryBuilder leggTilFerdigFilter(Brukerstatus brukerStatus, List<String> veiledereMedTilgangTilEnhet) {

        QueryBuilder queryBuilder;
        switch (brukerStatus) {
            case NYE_BRUKERE:
                queryBuilder = matchQuery("ny_for_enhet", true);
                break;
            case UFORDELTE_BRUKERE:
                queryBuilder = byggUfordeltBrukereQuery(veiledereMedTilgangTilEnhet);
                break;
            case TRENGER_VURDERING:
                queryBuilder = matchQuery("trenger_vurdering", true);
                break;
            case INAKTIVE_BRUKERE:
                queryBuilder = matchQuery("formidlingsgruppekode", "ISERV");
                break;
            case VENTER_PA_SVAR_FRA_NAV:
                queryBuilder = existsQuery("venterpasvarfranav");
                break;
            case VENTER_PA_SVAR_FRA_BRUKER:
                queryBuilder = existsQuery("venterpasvarfrabruker");
                break;
            case I_AVTALT_AKTIVITET:
                queryBuilder = existsQuery("aktiviteter");
                break;
            case IKKE_I_AVTALT_AKTIVITET:
                queryBuilder = boolQuery().mustNot(existsQuery("aktiviteter"));
                break;
            case UTLOPTE_AKTIVITETER:
                queryBuilder = existsQuery("nyesteutlopteaktivitet");
                break;
            case MIN_ARBEIDSLISTE:
                queryBuilder = matchQuery("arbeidsliste_aktiv", true);
                break;
            case NYE_BRUKERE_FOR_VEILEDER:
                queryBuilder = matchQuery("ny_for_veileder", true);
                break;
            case ER_SYKMELDT_MED_ARBEIDSGIVER:
                queryBuilder = boolQuery()
                        .must(matchQuery("formidlingsgruppekode", "IARBS"))
                        .mustNot(matchQuery("kvalifiseringsgruppekode", "BATT"))
                        .mustNot(matchQuery("kvalifiseringsgruppekode", "BFORM"))
                        .mustNot(matchQuery("kvalifiseringsgruppekode", "IKVAL"))
                        .mustNot(matchQuery("kvalifiseringsgruppekode", "VURDU"))
                        .mustNot(matchQuery("kvalifiseringsgruppekode", "OPPFI"))
                        .mustNot(matchQuery("kvalifiseringsgruppekode", "VARIG"));
                break;

            default:
                throw new IllegalStateException();

        }
        return queryBuilder;
    }

    // Brukere med veileder uten tilgang til denne enheten ansees som ufordelte brukere
    static BoolQueryBuilder byggUfordeltBrukereQuery(List<String> veiledereMedTilgangTilEnhet) {
        BoolQueryBuilder boolQuery = boolQuery();
        veiledereMedTilgangTilEnhet.forEach(id -> boolQuery.mustNot(matchQuery("veileder_id", id)));
        return boolQuery;
    }

    static<T> BoolQueryBuilder byggManuellFilter (List<T> filtervalgsListe, BoolQueryBuilder queryBuilder, String matchQueryString) {
        if (!filtervalgsListe.isEmpty()) {
            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
            filtervalgsListe.forEach(filtervalg -> boolQueryBuilder.should(matchQuery(matchQueryString, filtervalg)));
            return queryBuilder.filter(boolQueryBuilder);
        }

        return queryBuilder;
    }

    //
//  Eksempel:
//
//  Vi vil hente ut alle mellom 20-24
//  Kari har fodselsdato 1994-05-21
//  Datoen i dag er 2019-02-15
//
//  Da må vi filtrere på:
//
//  1999-02-15 >= fodselsdato > 1993-12-31
//
//  fodselsdato må være *mindre eller lik* datoen i dag for 20 år siden, altså før 1999-02-15.
//
//  fodselsdato må være *høyere* enn nyttårsaften for *25* år siden, altså etter 1993-12-31, fordi da fanger
//  vi opp personer som Kari som er 24 år og 8 måneder gammel.
//
//  Ja det gjør vondt i hodet å tenke på dette.
//
    static void byggAlderQuery(String alder, BoolQueryBuilder queryBuilder) {

        if ("19-og-under".equals(alder)) {
            queryBuilder.should(
                    rangeQuery("fodselsdato")
                            .lte("now")
                            .gt("now-20y-1d")
            );
        } else {

            String[] fraTilAlder = alder.split("-");
            int fraAlder = parseInt(fraTilAlder[0]);
            int tilAlder = parseInt(fraTilAlder[1]);

            queryBuilder.should(
                    rangeQuery("fodselsdato")
                            .lte(format("now-%sy/d", fraAlder))
                            .gt(format("now-%sy-1d", tilAlder + 1))
            );
        }
    }

    static SearchSourceBuilder byggArbeidslisteQuery(String enhetId, String veilederId) {
        return new SearchSourceBuilder().query(
                boolQuery()
                        .must(termQuery("enhet_id", enhetId))
                        .must(termQuery("veileder_id", veilederId))
                        .must(termQuery("arbeidsliste_aktiv", true))
        );
    }

    static SearchSourceBuilder byggPortefoljestorrelserQuery(String enhetId) {
        String name = "portefoljestorrelser";

        return new SearchSourceBuilder()
                .size(0)
                .aggregation(
                        filter(name, termQuery("enhet_id", enhetId))
                                .subAggregation(AggregationBuilders
                                        .terms(name)
                                        .field("veileder_id")
                                        .size(9999)
                                        .order(BucketOrder.key(true))
                                )
                );
    }


    static SearchSourceBuilder byggStatusTallForEnhetQuery(String enhetId, List<String> veiledereMedTilgangTilEnhet) {
        BoolQueryBuilder enhetQuery = boolQuery().must(termQuery("enhet_id", enhetId));
        return byggStatusTallQuery(enhetQuery, veiledereMedTilgangTilEnhet);
    }

    static SearchSourceBuilder byggStatusTallForVeilederQuery(String enhetId, String veilederId, List<String> veiledereMedTilgangTilEnhet) {
        BoolQueryBuilder veilederOgEnhetQuery = boolQuery()
                .must(termQuery("enhet_id", enhetId))
                .must(termQuery("veileder_id", veilederId));

        return byggStatusTallQuery(veilederOgEnhetQuery, veiledereMedTilgangTilEnhet);
    }

    private static SearchSourceBuilder byggStatusTallQuery(BoolQueryBuilder filtrereVeilederOgEnhet, List<String> veiledereMedTilgangTilEnhet) {


        return new SearchSourceBuilder()
                .size(0)
                .aggregation(
                        filters(
                                "statustall",
                                erSykmeldtMedArbeidsgiver(filtrereVeilederOgEnhet),
                                mustExistFilter(filtrereVeilederOgEnhet, "iavtaltAktivitet", "aktiviteter"),
                                ikkeIavtaltAktivitet(filtrereVeilederOgEnhet),
                                inaktiveBrukere(filtrereVeilederOgEnhet),
                                mustBeTrueFilter(filtrereVeilederOgEnhet, "minArbeidsliste", "arbeidsliste_aktiv"),
                                mustBeTrueFilter(filtrereVeilederOgEnhet, "nyeBrukere", "ny_for_enhet"),
                                mustBeTrueFilter(filtrereVeilederOgEnhet, "nyeBrukereForVeileder", "ny_for_veileder"),
                                totalt(filtrereVeilederOgEnhet),
                                mustBeTrueFilter(filtrereVeilederOgEnhet, "trengerVurdering", "trenger_vurdering"),
                                mustExistFilter(filtrereVeilederOgEnhet, "venterPaSvarFraNAV", "venterpasvarfranav"),
                                mustExistFilter(filtrereVeilederOgEnhet, "venterPaSvarFraBruker", "venterpasvarfrabruker"),
                                ufordelteBrukere(filtrereVeilederOgEnhet, veiledereMedTilgangTilEnhet),
                                mustExistFilter(filtrereVeilederOgEnhet, "utlopteAktiviteter", "nyesteutlopteaktivitet")
                        ));
    }

    private static KeyedFilter ufordelteBrukere(BoolQueryBuilder filtrereVeilederOgEnhet, List<String> veiledereMedTilgangTilEnhet) {
        return new KeyedFilter(
                "ufordelteBrukere",
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .should(byggUfordeltBrukereQuery(veiledereMedTilgangTilEnhet))
                        .should(boolQuery().mustNot(existsQuery("veileder_id")))
        );
    }

    private static KeyedFilter totalt(BoolQueryBuilder filtrereVeilederOgEnhet) {
        return new KeyedFilter(
                "totalt",
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
        );
    }

    private static KeyedFilter mustBeTrueFilter(BoolQueryBuilder filtrereVeilederOgEnhet, String minArbeidsliste, String arbeidsliste_aktiv) {
        return new KeyedFilter(
                minArbeidsliste,
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .must(termQuery(arbeidsliste_aktiv, true))

        );
    }

    private static KeyedFilter inaktiveBrukere(BoolQueryBuilder filtrereVeilederOgEnhet) {
        return new KeyedFilter(
                "inaktiveBrukere",
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .must(matchQuery("formidlingsgruppekode", "ISERV"))

        );
    }

    private static KeyedFilter ikkeIavtaltAktivitet(BoolQueryBuilder filtrereVeilederOgEnhet) {
        return new KeyedFilter(
                "ikkeIavtaltAktivitet",
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .mustNot(existsQuery("aktiviteter"))

        );
    }

    private static KeyedFilter mustExistFilter(BoolQueryBuilder filtrereVeilederOgEnhet, String key, String value) {
        return new KeyedFilter(
                key,
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .must(existsQuery(value))
        );
    }

    private static KeyedFilter erSykmeldtMedArbeidsgiver(BoolQueryBuilder filtrereVeilederOgEnhet) {
        return new KeyedFilter(
                "erSykmeldtMedArbeidsgiver",
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .must(matchQuery("formidlingsgruppekode", "IARBS"))
                        .mustNot(matchQuery("kvalifiseringsgruppekode", "BATT"))
                        .mustNot(matchQuery("kvalifiseringsgruppekode", "BFORM"))
                        .mustNot(matchQuery("kvalifiseringsgruppekode", "IKVAL"))
                        .mustNot(matchQuery("kvalifiseringsgruppekode", "VURDU"))
                        .mustNot(matchQuery("kvalifiseringsgruppekode", "OPPFI"))
                        .mustNot(matchQuery("kvalifiseringsgruppekode", "VARIG"))
        );
    }

    public static String byggVeilederPaaEnhetScript(List<String> veilederePaaEnhet) {
        String veiledere = veilederePaaEnhet.stream()
                .map(id -> format("\"%s\"", id))
                .collect(joining(","));

        String medKlammer = format("%s%s%s", "[", veiledere, "]");
        return format("%s.contains(doc.veileder_id.value)", medKlammer);
    }
}

