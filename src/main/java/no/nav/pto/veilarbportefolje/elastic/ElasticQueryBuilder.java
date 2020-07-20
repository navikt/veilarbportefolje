package no.nav.pto.veilarbportefolje.elastic;

import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.domene.CVjobbprofil;
import no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg;
import no.nav.pto.veilarbportefolje.domene.Brukerstatus;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg.JA;
import static no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg.NEI;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.apache.commons.lang3.StringUtils.truncate;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.search.aggregations.AggregationBuilders.filter;
import static org.elasticsearch.search.aggregations.AggregationBuilders.filters;
import static org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregator.KeyedFilter;
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
        byggManuellFilter(filtervalg.innsatsgruppe, queryBuilder, "kvalifiseringsgruppekode");
        byggManuellFilter(filtervalg.hovedmal, queryBuilder, "hovedmaalkode");
        byggManuellFilter(filtervalg.formidlingsgruppe, queryBuilder, "formidlingsgruppekode");
        byggManuellFilter(filtervalg.servicegruppe, queryBuilder, "kvalifiseringsgruppekode");
        byggManuellFilter(filtervalg.veiledere, queryBuilder, "veileder_id");
        byggManuellFilter(filtervalg.manuellBrukerStatus, queryBuilder, "manuell_bruker");
        byggManuellFilter(filtervalg.tiltakstyper, queryBuilder, "tiltak");
        byggManuellFilter(filtervalg.rettighetsgruppe, queryBuilder, "rettighetsgruppekode");
        byggManuellFilter(filtervalg.registreringstype, queryBuilder, "brukers_situasjon");

        byggManuellFilter(filtervalg.arbeidslisteKategori, queryBuilder, "arbeidsliste_kategori");

        if (filtervalg.harYtelsefilter()) {
            BoolQueryBuilder subQuery = boolQuery();
            filtervalg.ytelse.underytelser.forEach(
                    ytelse -> queryBuilder.must(subQuery.should(matchQuery("ytelse", ytelse.name())))
            );
        }

        if (filtervalg.harKjonnfilter()) {
            BoolQueryBuilder subQuery = boolQuery();
            queryBuilder.must(subQuery.should(matchQuery("kjonn", filtervalg.kjonn.name())));
        }

        if (filtervalg.harCvFilter()) {
            queryBuilder.filter(matchQuery("har_delt_cv", filtervalg.cvJobbprofil.equals(CVjobbprofil.HAR_DELT_CV)));
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
                            } else if (NEI.equals(valg) && "TILTAK".equals(navnPaaAktivitet)) {
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
            case "moterMedNAVIdag":
                searchSourceBuilder.sort("aktivitet_mote_startdato", order);
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
            case "vedtakstatus":
                searchSourceBuilder.sort("vedtak_status", order);
                break;
            case "arbeidslistekategori":
                searchSourceBuilder.sort("arbeidsliste_kategori", order);
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
        ScriptSortBuilder scriptBuilder = new ScriptSortBuilder(script, ScriptSortBuilder.ScriptSortType.NUMBER);
        scriptBuilder.order(order);
        builder.sort(scriptBuilder);
        return builder;
    }

    static SearchSourceBuilder sorterValgteAktiviteter(Filtervalg filtervalg, SearchSourceBuilder builder, SortOrder order) {
        filtervalg.aktiviteter.entrySet().stream()
                .filter(entry -> JA.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .map(aktivitet -> format("aktivitet_%s_utlopsdato", aktivitet.toLowerCase()))
                .forEach(aktivitet -> builder.sort(aktivitet, order));

        return builder;
    }

    static QueryBuilder leggTilFerdigFilter(Brukerstatus brukerStatus, List<String> veiledereMedTilgangTilEnhet, boolean erVedtakstottePilotPa) {
        LocalDate localDate = LocalDate.now();

        QueryBuilder queryBuilder;
        switch (brukerStatus) {
            case NYE_BRUKERE:
                queryBuilder = matchQuery("ny_for_enhet", true);
                break;
            case UFORDELTE_BRUKERE:
                queryBuilder = byggUfordeltBrukereQuery(veiledereMedTilgangTilEnhet);
                break;
            case TRENGER_VURDERING:
                queryBuilder = byggTrengerVurderingFilter(erVedtakstottePilotPa);
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
            case MOTER_IDAG:
                queryBuilder = rangeQuery("aktivitet_mote_startdato")
                        .gte(toIsoUTC(localDate.atStartOfDay()))
                        .lt(toIsoUTC(localDate.plusDays(1).atStartOfDay()));
                break;
            case ER_SYKMELDT_MED_ARBEIDSGIVER:
                queryBuilder = byggErSykmeldtMedArbeidsgiverFilter(erVedtakstottePilotPa);
                break;
            case UNDER_VURDERING:
                if (erVedtakstottePilotPa) {
                    queryBuilder = existsQuery("vedtak_status");
                    break;
                }
            case PERMITTERTE_ETTER_NIENDE_MARS:
                queryBuilder = byggPermittertFilter();
                break;
            case IKKE_PERMITTERTE_ETTER_NIENDE_MARS:
                queryBuilder = byggIkkePermittertFilter();
                break;
            default:
                throw new IllegalStateException();

        }
        return queryBuilder;
    }

    // Brukere med veileder uten tilgang til denne enheten ansees som ufordelte brukere
    static QueryBuilder byggTrengerVurderingFilter(boolean erVedtakstottePilotPa) {
        if (erVedtakstottePilotPa) {
            return boolQuery()
                    .must(matchQuery("trenger_vurdering", true))
                    .mustNot(existsQuery("vedtak_status"));
        }

        return matchQuery("trenger_vurdering", true);

    }

    static QueryBuilder byggErSykmeldtMedArbeidsgiverFilter(boolean erVedtakstottePilotPa) {
        if (erVedtakstottePilotPa) {
            return boolQuery()
                    .must(matchQuery("er_sykmeldt_med_arbeidsgiver", true))
                    .mustNot(existsQuery("vedtak_status"));
        }

        return matchQuery("er_sykmeldt_med_arbeidsgiver", true);

    }


    // Brukere med veileder uten tilgang til denne enheten ansees som ufordelte brukere
    static BoolQueryBuilder byggUfordeltBrukereQuery(List<String> veiledereMedTilgangTilEnhet) {
        BoolQueryBuilder boolQuery = boolQuery();
        veiledereMedTilgangTilEnhet.forEach(id -> boolQuery.mustNot(matchQuery("veileder_id", id)));
        return boolQuery;
    }

    static <T> BoolQueryBuilder byggManuellFilter(List<T> filtervalgsListe, BoolQueryBuilder queryBuilder, String matchQueryString) {
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


    static SearchSourceBuilder byggStatusTallForEnhetQuery(String enhetId, List<String> veiledereMedTilgangTilEnhet, boolean vedtakstottePilotErPa) {
        BoolQueryBuilder enhetQuery = boolQuery().must(termQuery("enhet_id", enhetId));
        return byggStatusTallQuery(enhetQuery, veiledereMedTilgangTilEnhet, vedtakstottePilotErPa);
    }

    static SearchSourceBuilder byggStatusTallForVeilederQuery(String enhetId, String veilederId, List<String> veiledereMedTilgangTilEnhet, boolean vedtakstottePilotErPa) {
        BoolQueryBuilder veilederOgEnhetQuery = boolQuery()
                .must(termQuery("enhet_id", enhetId))
                .must(termQuery("veileder_id", veilederId));

        return byggStatusTallQuery(veilederOgEnhetQuery, veiledereMedTilgangTilEnhet, vedtakstottePilotErPa);
    }

    static BoolQueryBuilder byggIkkePermittertFilter() {
        return boolQuery()
                .must(boolQuery()
                        .should(boolQuery().mustNot(matchQuery("brukers_situasjon", "ER_PERMITTERT")))
                        .should(boolQuery().mustNot(rangeQuery("oppfolging_startdato")
                                .gte(toIsoUTC(LocalDate.of(2020, 3, 10).atStartOfDay())))));

    }

    static BoolQueryBuilder byggPermittertFilter() {
        return boolQuery()
                .must(matchQuery("brukers_situasjon", "ER_PERMITTERT"))
                .must(rangeQuery("oppfolging_startdato")
                        .gte(toIsoUTC(LocalDate.of(2020, 3, 10).atStartOfDay())));
    }

    private static SearchSourceBuilder byggStatusTallQuery(BoolQueryBuilder filtrereVeilederOgEnhet, List<String> veiledereMedTilgangTilEnhet, boolean vedtakstottePilotErPa) {
        return new SearchSourceBuilder()
                .size(0)
                .aggregation(
                        filters(
                                "statustall",
                                erSykemeldtMedArbeidsgiverFilter(filtrereVeilederOgEnhet, vedtakstottePilotErPa),
                                mustExistFilter(filtrereVeilederOgEnhet, "iavtaltAktivitet", "aktiviteter"),
                                ikkeIavtaltAktivitet(filtrereVeilederOgEnhet),
                                inaktiveBrukere(filtrereVeilederOgEnhet),
                                mustBeTrueFilter(filtrereVeilederOgEnhet, "minArbeidsliste", "arbeidsliste_aktiv"),
                                mustBeTrueFilter(filtrereVeilederOgEnhet, "nyeBrukere", "ny_for_enhet"),
                                mustBeTrueFilter(filtrereVeilederOgEnhet, "nyeBrukereForVeileder", "ny_for_veileder"),
                                totalt(filtrereVeilederOgEnhet),
                                trengerVurderingFilter(filtrereVeilederOgEnhet, vedtakstottePilotErPa),
                                mustExistFilter(filtrereVeilederOgEnhet, "venterPaSvarFraNAV", "venterpasvarfranav"),
                                mustExistFilter(filtrereVeilederOgEnhet, "venterPaSvarFraBruker", "venterpasvarfrabruker"),
                                ufordelteBrukere(filtrereVeilederOgEnhet, veiledereMedTilgangTilEnhet),
                                mustExistFilter(filtrereVeilederOgEnhet, "utlopteAktiviteter", "nyesteutlopteaktivitet"),
                                moterMedNavIdag(filtrereVeilederOgEnhet),
                                mustExistFilter(filtrereVeilederOgEnhet, "underVurdering", "vedtak_status"),
                                permitterteEtterNiendeMarsStatusTall(filtrereVeilederOgEnhet),
                                ikkePermitterteEtterNiendeMarsStatusTall(filtrereVeilederOgEnhet),
                                mustMatchQuery(filtrereVeilederOgEnhet, "minArbeidslisteBla", "arbeidsliste_kategori", Arbeidsliste.Kategori.BLA.name()),
                                mustMatchQuery(filtrereVeilederOgEnhet, "minArbeidslisteLilla", "arbeidsliste_kategori", Arbeidsliste.Kategori.LILLA.name()),
                                mustMatchQuery(filtrereVeilederOgEnhet, "minArbeidslisteGronn", "arbeidsliste_kategori", Arbeidsliste.Kategori.GRONN.name()),
                                mustMatchQuery(filtrereVeilederOgEnhet, "minArbeidslisteGul", "arbeidsliste_kategori", Arbeidsliste.Kategori.GUL.name())
                        ));
    }

    private static KeyedFilter trengerVurderingFilter(BoolQueryBuilder filtrereVeilederOgEnhet, boolean vedtakstottePilotErPa) {
        BoolQueryBuilder boolQueryBuilder = boolQuery()
                .must(filtrereVeilederOgEnhet)
                .must(termQuery("trenger_vurdering", true));

        if (vedtakstottePilotErPa) {
            boolQueryBuilder.mustNot(existsQuery("vedtak_status"));
        }

        return new KeyedFilter("trengerVurdering", boolQueryBuilder);
    }

    private static KeyedFilter erSykemeldtMedArbeidsgiverFilter(BoolQueryBuilder filtrereVeilederOgEnhet, boolean vedtakstottePilotErPa) {
        BoolQueryBuilder boolQueryBuilder = boolQuery()
                .must(filtrereVeilederOgEnhet)
                .must(termQuery("er_sykmeldt_med_arbeidsgiver", true));

        if (vedtakstottePilotErPa) {
            boolQueryBuilder.mustNot(existsQuery("vedtak_status"));
        }

        return new KeyedFilter("erSykmeldtMedArbeidsgiver", boolQueryBuilder);
    }

    private static KeyedFilter permitterteEtterNiendeMarsStatusTall(BoolQueryBuilder filtrereVeilederOgEnhet) {
        return new KeyedFilter("permitterteEtterNiendeMars", byggPermittertFilter().must(filtrereVeilederOgEnhet));
    }

    private static KeyedFilter ikkePermitterteEtterNiendeMarsStatusTall(BoolQueryBuilder filtrereVeilederOgEnhet) {
        return new KeyedFilter("ikkePermitterteEtterNiendeMars", byggIkkePermittertFilter().must(filtrereVeilederOgEnhet));
    }

    private static KeyedFilter moterMedNavIdag(BoolQueryBuilder filtrereVeilederOgEnhet) {
        LocalDate localDate = LocalDate.now();
        return new KeyedFilter(
                "moterMedNAVIdag",
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .should(rangeQuery("aktivitet_mote_startdato")
                                .gte(toIsoUTC(localDate.atStartOfDay()))
                                .lt(toIsoUTC(localDate.plusDays(1).atStartOfDay())))
        );
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

    private static KeyedFilter mustMatchQuery(BoolQueryBuilder filtrereVeilederOgEnhet, String key, String matchQuery, String value) {
        return new KeyedFilter(
                key,
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .must(matchQuery(matchQuery, value))
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

    public static String byggVeilederPaaEnhetScript(List<String> veilederePaaEnhet) {
        String veiledere = veilederePaaEnhet.stream()
                .map(id -> format("\"%s\"", id))
                .collect(joining(","));

        String medKlammer = format("%s%s%s", "[", veiledere, "]");
        return format("%s.contains(doc.veileder_id.value)", medKlammer);
    }
}

