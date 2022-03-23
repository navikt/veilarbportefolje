package no.nav.pto.veilarbportefolje.opensearch;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg;
import no.nav.pto.veilarbportefolje.domene.Brukerstatus;
import no.nav.pto.veilarbportefolje.domene.CVjobbprofil;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.RangeQueryBuilder;
import org.opensearch.script.Script;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.BucketOrder;
import org.opensearch.search.aggregations.bucket.filter.FiltersAggregator;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.FieldSortBuilder;
import org.opensearch.search.sort.ScriptSortBuilder;
import org.opensearch.search.sort.SortOrder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg.JA;
import static no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg.NEI;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.existsQuery;
import static org.opensearch.index.query.QueryBuilders.matchQuery;
import static org.opensearch.index.query.QueryBuilders.rangeQuery;
import static org.opensearch.index.query.QueryBuilders.termQuery;
import static org.opensearch.search.aggregations.AggregationBuilders.filter;
import static org.opensearch.search.aggregations.AggregationBuilders.filters;
import static org.opensearch.search.sort.ScriptSortBuilder.ScriptSortType.STRING;
import static org.opensearch.search.sort.SortMode.MIN;

@Slf4j
public class OpensearchQueryBuilder {

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
        byggManuellFilter(filtervalg.utdanning, queryBuilder, "utdanning");
        byggManuellFilter(filtervalg.utdanningBestatt, queryBuilder, "utdanning_bestatt");
        byggManuellFilter(filtervalg.utdanningGodkjent, queryBuilder, "utdanning_godkjent");
        byggManuellFilter(filtervalg.arbeidslisteKategori, queryBuilder, "arbeidsliste_kategori");
        byggManuellFilter(filtervalg.aktiviteterForenklet, queryBuilder, "aktiviteter");
        byggManuellFilter(filtervalg.alleAktiviteter, queryBuilder, "alleAktiviteter");

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
            if (filtervalg.cvJobbprofil.equals(CVjobbprofil.HAR_DELT_CV)) {
                queryBuilder.must(matchQuery("har_delt_cv", true));
                queryBuilder.must(matchQuery("cv_eksistere", true));
            } else {
                BoolQueryBuilder orQuery = boolQuery();
                orQuery.should(matchQuery("har_delt_cv", false));
                orQuery.should(matchQuery("cv_eksistere", false));
                queryBuilder.must(orQuery);
            }
        }


        if (filtervalg.harAktivitetFilter()) {
            byggAktivitetFilterQuery(filtervalg, queryBuilder);
        }

        if (filtervalg.harUlesteEndringerFilter()) {
            byggUlestEndringsFilter(filtervalg.sisteEndringKategori, queryBuilder);
        }

        if (filtervalg.harSisteEndringFilter()) {
            byggSisteEndringFilter(filtervalg.sisteEndringKategori, queryBuilder);
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

    private static void byggUlestEndringsFilter(List<String> sisteEndringKategori, BoolQueryBuilder queryBuilder) {
        if (sisteEndringKategori != null && sisteEndringKategori.size() > 1) {
            log.error("Det ble filtrert på flere ulike siste endringer (ulest): {}", sisteEndringKategori.size());
            throw new IllegalStateException("Filtrering på flere siste_endringer er ikke tilatt.");
        }
        List<String> relvanteKategorier = sisteEndringKategori;
        if (sisteEndringKategori == null || sisteEndringKategori.isEmpty()) {
            relvanteKategorier = (Arrays.stream(SisteEndringsKategori.values()).map(SisteEndringsKategori::name)).collect(toList());
        }

        BoolQueryBuilder orQuery = boolQuery();
        relvanteKategorier.forEach(kategori -> orQuery.should(
                QueryBuilders.matchQuery("siste_endringer." + kategori + ".er_sett", "N")
        ));
        queryBuilder.must(orQuery);
    }

    private static void byggSisteEndringFilter(List<String> sisteEndringKategori, BoolQueryBuilder queryBuilder) {
        if (sisteEndringKategori.size() == 0){
            return;
        }
        if (sisteEndringKategori.size() != 1) {
            log.error("Det ble filtrert på flere ulike siste endringer: {}", sisteEndringKategori.size());
            throw new IllegalStateException("Filtrering på flere siste_endringer er ikke tilatt.");
        }
        queryBuilder.must(existsQuery("siste_endringer." + sisteEndringKategori.get(0)));
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

    static SearchSourceBuilder sorterQueryParametere(String sortOrder, String sortField, SearchSourceBuilder searchSourceBuilder, Filtervalg filtervalg, boolean brukAvIkkeAvtalteAktiviteter) {
        SortOrder order = "ascending".equals(sortOrder) ? SortOrder.ASC : SortOrder.DESC;

        if ("ikke_satt".equals(sortField)) {
            searchSourceBuilder.sort("aktoer_id", SortOrder.ASC);
            return searchSourceBuilder;
        }

        switch (sortField) {
            case "valgteaktiviteter" -> sorterValgteAktiviteter(filtervalg, searchSourceBuilder, order);
            case "moterMedNAVIdag" -> {
                if (brukAvIkkeAvtalteAktiviteter) {
                    searchSourceBuilder.sort("alle_aktiviteter_mote_startdato", order);
                } else {
                    searchSourceBuilder.sort("aktivitet_mote_startdato", order);
                }
            }
            case "motestatus" -> searchSourceBuilder.sort("aktivitet_mote_startdato", order);
            case "iavtaltaktivitet" -> {
                FieldSortBuilder builder = new FieldSortBuilder("aktivitet_utlopsdatoer")
                        .order(order)
                        .sortMode(MIN);
                searchSourceBuilder.sort(builder);
            }
            case "iaktivitet" -> {
                FieldSortBuilder builder = new FieldSortBuilder("alle_aktiviteter_utlopsdatoer")
                        .order(order)
                        .sortMode(MIN);
                searchSourceBuilder.sort(builder);
            }
            case "fodselsnummer" -> searchSourceBuilder.sort("fnr.raw", order);
            case "utlopteaktiviteter" -> searchSourceBuilder.sort("nyesteutlopteaktivitet", order);
            case "arbeidslistefrist" -> searchSourceBuilder.sort("arbeidsliste_frist", order);
            case "aaprettighetsperiode" -> sorterAapRettighetsPeriode(searchSourceBuilder, order);
            case "vedtakstatus" -> searchSourceBuilder.sort("vedtak_status", order);
            case "arbeidslistekategori" -> searchSourceBuilder.sort("arbeidsliste_kategori", order);
            case "siste_endring_tidspunkt" -> sorterSisteEndringTidspunkt(searchSourceBuilder, order, filtervalg);
            case "arbeidsliste_overskrift" -> sorterArbeidslisteOverskrift(searchSourceBuilder, order);
            default -> defaultSort(sortField, searchSourceBuilder, order);
        }
        addSecondarySort(searchSourceBuilder);
        return searchSourceBuilder;
    }

    private static void defaultSort(String sortField, SearchSourceBuilder searchSourceBuilder, SortOrder order) {
        if (ValideringsRegler.sortFields.contains(sortField)) {
            searchSourceBuilder.sort(sortField, order);
        } else {
            throw new IllegalStateException();
        }
    }

    static void sorterSisteEndringTidspunkt(SearchSourceBuilder builder, SortOrder order, Filtervalg filtervalg) {
        if (filtervalg.sisteEndringKategori.size() == 0){
            return;
        }
        if (filtervalg.sisteEndringKategori.size() != 1) {
            log.error("Det ble sortert på flere ulike siste endringer: {}", filtervalg.sisteEndringKategori.size());
            throw new IllegalStateException("Filtrering på flere siste_endringer er ikke tilatt.");
        }
        String expresion = "doc['siste_endringer." + filtervalg.sisteEndringKategori.get(0) + ".tidspunkt']?.value.toInstant().toEpochMilli()";

        Script script = new Script(expresion);
        ScriptSortBuilder scriptBuilder = new ScriptSortBuilder(script, ScriptSortBuilder.ScriptSortType.NUMBER);
        scriptBuilder.order(order);
        builder.sort(scriptBuilder);
    }

    static void sorterArbeidslisteOverskrift(SearchSourceBuilder searchSourceBuilder, SortOrder order) {
        searchSourceBuilder.sort("arbeidsliste_tittel_sortering", order);
        searchSourceBuilder.sort("arbeidsliste_tittel_lengde", order);
    }

    static SearchSourceBuilder sorterPaaNyForEnhet(SearchSourceBuilder builder, List<String> veilederePaaEnhet) {
        Script script = new Script(byggVeilederPaaEnhetScript(veilederePaaEnhet));
        ScriptSortBuilder scriptBuilder = new ScriptSortBuilder(script, STRING);
        builder.sort(scriptBuilder);
        return builder;
    }

    static SearchSourceBuilder sorterAapRettighetsPeriode(SearchSourceBuilder builder, SortOrder order) {
        Script script = new Script("Math.max((doc.aapmaxtiduke.size() != 0) ? doc.aapmaxtiduke.value : 0, (doc.aapunntakukerigjen.size() != 0) ? doc.aapunntakukerigjen.value : 0)");
        ScriptSortBuilder scriptBuilder = new ScriptSortBuilder(script, ScriptSortBuilder.ScriptSortType.NUMBER);
        scriptBuilder.order(order);
        builder.sort(scriptBuilder);
        return builder;
    }

    static SearchSourceBuilder sorterValgteAktiviteter(Filtervalg filtervalg, SearchSourceBuilder builder, SortOrder order) {
        List<String> sorterings_aktiviter;
        if (filtervalg.harAktiviteterForenklet()) {
            sorterings_aktiviter = filtervalg.aktiviteterForenklet;
        } else {
            sorterings_aktiviter = filtervalg.aktiviteter.entrySet().stream()
                    .filter(entry -> JA.equals(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(toList());
        }

        if (sorterings_aktiviter.isEmpty()) {
            return builder;
        }
        StringJoiner script = new StringJoiner("", "List l = new ArrayList(); ", "return l.stream().sorted().findFirst().get();");
        sorterings_aktiviter.forEach(aktivitet -> script.add(format("l.add(doc['aktivitet_%s_utlopsdato']?.value.toInstant().toEpochMilli()); ", aktivitet.toLowerCase())));
        ScriptSortBuilder scriptBuilder = new ScriptSortBuilder(new Script(script.toString()), ScriptSortBuilder.ScriptSortType.NUMBER);
        scriptBuilder.order(order);
        builder.sort(scriptBuilder);

        return builder;
    }

    static QueryBuilder leggTilFerdigFilter(Brukerstatus brukerStatus, List<String> veiledereMedTilgangTilEnhet, boolean erVedtakstottePilotPa, boolean brukIkkeAvtalteAktiviteter) {
        QueryBuilder queryBuilder;
        switch (brukerStatus) {
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
            case I_AKTIVITET:
                queryBuilder = existsQuery("alleAktiviteter");
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
                queryBuilder = byggMoteMedNavIdag(brukIkkeAvtalteAktiviteter);
                break;
            case ER_SYKMELDT_MED_ARBEIDSGIVER:
                queryBuilder = byggErSykmeldtMedArbeidsgiverFilter(erVedtakstottePilotPa);
                break;
            case UNDER_VURDERING:
                if (erVedtakstottePilotPa) {
                    queryBuilder = existsQuery("vedtak_status");
                } else {
                    throw new IllegalStateException();
                }
                break;
            default:
                throw new IllegalStateException();

        }
        return queryBuilder;
    }

    private static RangeQueryBuilder byggMoteMedNavIdag(boolean inkluderIkkeAvtalte){
        LocalDate localDate = LocalDate.now();
        String moteFelt = inkluderIkkeAvtalte ? "alle_aktiviteter_mote_startdato" : "aktivitet_mote_startdato";
        return rangeQuery(moteFelt)
                .gte(toIsoUTC(localDate.atStartOfDay()))
                .lt(toIsoUTC(localDate.plusDays(1).atStartOfDay()));
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
                        .must(termQuery("oppfolging", true))
                        .must(termQuery("enhet_id", enhetId))
                        .must(termQuery("veileder_id", veilederId))
                        .must(termQuery("arbeidsliste_aktiv", true))
        );
    }


    static SearchSourceBuilder byggPortefoljestorrelserQuery(String enhetId) {
        String name = "portefoljestorrelser";

        return new SearchSourceBuilder()
                .size(0)
                .query(termQuery("oppfolging", true))
                .aggregation(
                        filter(
                                name,
                                termQuery("enhet_id", enhetId))
                                .subAggregation(AggregationBuilders
                                        .terms(name)
                                        .field("veileder_id")
                                        .size(9999)
                                        .order(BucketOrder.key(true))
                                )
                );
    }

    static SearchSourceBuilder byggStatusTallForEnhetQuery(String enhetId, List<String> veiledereMedTilgangTilEnhet, boolean vedtakstottePilotErPa, boolean inkluderIkkeAvtalteAktiviteter) {
        BoolQueryBuilder enhetQuery = boolQuery()
                .must(termQuery("oppfolging", true))
                .must(termQuery("enhet_id", enhetId));

        return byggStatusTallQuery(enhetQuery, veiledereMedTilgangTilEnhet, vedtakstottePilotErPa, inkluderIkkeAvtalteAktiviteter);
    }

    static SearchSourceBuilder byggStatusTallForVeilederQuery(String enhetId, String veilederId, List<String> veiledereMedTilgangTilEnhet, boolean vedtakstottePilotErPa, boolean inkluderIkkeAvtalteAktiviteter) {
        BoolQueryBuilder veilederOgEnhetQuery = boolQuery()
                .must(termQuery("oppfolging", true))
                .must(termQuery("enhet_id", enhetId))
                .must(termQuery("veileder_id", veilederId));

        return byggStatusTallQuery(veilederOgEnhetQuery, veiledereMedTilgangTilEnhet, vedtakstottePilotErPa, inkluderIkkeAvtalteAktiviteter);
    }

    private static SearchSourceBuilder byggStatusTallQuery(BoolQueryBuilder filtrereVeilederOgEnhet, List<String> veiledereMedTilgangTilEnhet, boolean vedtakstottePilotErPa, boolean inkluderIkkeAvtalteAktiviteter) {
        return new SearchSourceBuilder()
                .size(0)
                .aggregation(
                        filters(
                                "statustall",
                                erSykemeldtMedArbeidsgiverFilter(filtrereVeilederOgEnhet, vedtakstottePilotErPa),
                                mustExistFilter(filtrereVeilederOgEnhet, "iavtaltAktivitet", "aktiviteter"),
                                mustExistFilter(filtrereVeilederOgEnhet, "iAktivitet", "alleAktiviteter"),
                                ikkeIavtaltAktivitet(filtrereVeilederOgEnhet),
                                inaktiveBrukere(filtrereVeilederOgEnhet),
                                mustBeTrueFilter(filtrereVeilederOgEnhet, "minArbeidsliste", "arbeidsliste_aktiv"),
                                mustBeTrueFilter(filtrereVeilederOgEnhet, "nyeBrukereForVeileder", "ny_for_veileder"),
                                totalt(filtrereVeilederOgEnhet),
                                trengerVurderingFilter(filtrereVeilederOgEnhet, vedtakstottePilotErPa),
                                mustExistFilter(filtrereVeilederOgEnhet, "venterPaSvarFraNAV", "venterpasvarfranav"),
                                mustExistFilter(filtrereVeilederOgEnhet, "venterPaSvarFraBruker", "venterpasvarfrabruker"),
                                ufordelteBrukere(filtrereVeilederOgEnhet, veiledereMedTilgangTilEnhet),
                                mustExistFilter(filtrereVeilederOgEnhet, "utlopteAktiviteter", "nyesteutlopteaktivitet"),
                                moterMedNavIdag(filtrereVeilederOgEnhet, inkluderIkkeAvtalteAktiviteter),
                                alleMoterMedNavIdag(filtrereVeilederOgEnhet),
                                mustExistFilter(filtrereVeilederOgEnhet, "underVurdering", "vedtak_status"),
                                mustMatchQuery(filtrereVeilederOgEnhet, "minArbeidslisteBla", "arbeidsliste_kategori", Arbeidsliste.Kategori.BLA.name()),
                                mustMatchQuery(filtrereVeilederOgEnhet, "minArbeidslisteLilla", "arbeidsliste_kategori", Arbeidsliste.Kategori.LILLA.name()),
                                mustMatchQuery(filtrereVeilederOgEnhet, "minArbeidslisteGronn", "arbeidsliste_kategori", Arbeidsliste.Kategori.GRONN.name()),
                                mustMatchQuery(filtrereVeilederOgEnhet, "minArbeidslisteGul", "arbeidsliste_kategori", Arbeidsliste.Kategori.GUL.name())
                        ));
    }

    private static FiltersAggregator.KeyedFilter trengerVurderingFilter(BoolQueryBuilder filtrereVeilederOgEnhet, boolean vedtakstottePilotErPa) {
        BoolQueryBuilder boolQueryBuilder = boolQuery()
                .must(filtrereVeilederOgEnhet)
                .must(termQuery("trenger_vurdering", true));

        if (vedtakstottePilotErPa) {
            boolQueryBuilder.mustNot(existsQuery("vedtak_status"));
        }

        return new FiltersAggregator.KeyedFilter("trengerVurdering", boolQueryBuilder);
    }

    private static FiltersAggregator.KeyedFilter erSykemeldtMedArbeidsgiverFilter(BoolQueryBuilder filtrereVeilederOgEnhet, boolean vedtakstottePilotErPa) {
        BoolQueryBuilder boolQueryBuilder = boolQuery()
                .must(filtrereVeilederOgEnhet)
                .must(termQuery("er_sykmeldt_med_arbeidsgiver", true));

        if (vedtakstottePilotErPa) {
            boolQueryBuilder.mustNot(existsQuery("vedtak_status"));
        }

        return new FiltersAggregator.KeyedFilter("erSykmeldtMedArbeidsgiver", boolQueryBuilder);
    }

    private static FiltersAggregator.KeyedFilter alleMoterMedNavIdag(BoolQueryBuilder filtrereVeilederOgEnhet) {
        LocalDate localDate = LocalDate.now();
        return new FiltersAggregator.KeyedFilter(
                "alleMoterMedNAVIdag",
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .must(rangeQuery("alle_aktiviteter_mote_startdato")
                                .gte(toIsoUTC(localDate.atStartOfDay()))
                                .lt(toIsoUTC(localDate.plusDays(1).atStartOfDay())))
        );
    }

    private static FiltersAggregator.KeyedFilter moterMedNavIdag(BoolQueryBuilder filtrereVeilederOgEnhet, boolean inkluderIkkeAvtalteAktiviteter) {
        return new FiltersAggregator.KeyedFilter(
                "moterMedNAVIdag",
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .must(byggMoteMedNavIdag(inkluderIkkeAvtalteAktiviteter))
        );
    }

    private static FiltersAggregator.KeyedFilter ufordelteBrukere(BoolQueryBuilder filtrereVeilederOgEnhet, List<String> veiledereMedTilgangTilEnhet) {
        return new FiltersAggregator.KeyedFilter(
                "ufordelteBrukere",
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .must(byggUfordeltBrukereQuery(veiledereMedTilgangTilEnhet))
        );
    }

    private static FiltersAggregator.KeyedFilter totalt(BoolQueryBuilder filtrereVeilederOgEnhet) {
        return new FiltersAggregator.KeyedFilter(
                "totalt",
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
        );
    }

    private static FiltersAggregator.KeyedFilter mustBeTrueFilter(BoolQueryBuilder filtrereVeilederOgEnhet, String minArbeidsliste, String arbeidsliste_aktiv) {
        return new FiltersAggregator.KeyedFilter(
                minArbeidsliste,
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .must(termQuery(arbeidsliste_aktiv, true))

        );
    }

    private static FiltersAggregator.KeyedFilter inaktiveBrukere(BoolQueryBuilder filtrereVeilederOgEnhet) {
        return new FiltersAggregator.KeyedFilter(
                "inaktiveBrukere",
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .must(matchQuery("formidlingsgruppekode", "ISERV"))

        );
    }

    private static FiltersAggregator.KeyedFilter mustMatchQuery(BoolQueryBuilder filtrereVeilederOgEnhet, String key, String matchQuery, String value) {
        return new FiltersAggregator.KeyedFilter(
                key,
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .must(matchQuery(matchQuery, value))
        );
    }

    private static FiltersAggregator.KeyedFilter ikkeIavtaltAktivitet(BoolQueryBuilder filtrereVeilederOgEnhet) {
        return new FiltersAggregator.KeyedFilter(
                "ikkeIavtaltAktivitet",
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .mustNot(existsQuery("aktiviteter"))

        );
    }

    private static FiltersAggregator.KeyedFilter mustExistFilter(BoolQueryBuilder filtrereVeilederOgEnhet, String key, String value) {
        return new FiltersAggregator.KeyedFilter(
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

        String veilederListe = format("[%s]", veiledere);
        return format("(doc.veileder_id.size() != 0 && %s.contains(doc.veileder_id.value)).toString()", veilederListe);
    }

    private static void addSecondarySort(SearchSourceBuilder searchSourceBuilder) {
        searchSourceBuilder.sort("aktoer_id", SortOrder.ASC);
    }
}

