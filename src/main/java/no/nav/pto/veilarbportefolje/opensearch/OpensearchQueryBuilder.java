package no.nav.pto.veilarbportefolje.opensearch;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.JobbSituasjonBeskrivelse;
import no.nav.pto.veilarbportefolje.auth.BrukerinnsynTilganger;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.domene.filtervalg.UtdanningBestattSvar;
import no.nav.pto.veilarbportefolje.domene.filtervalg.UtdanningGodkjentSvar;
import no.nav.pto.veilarbportefolje.domene.filtervalg.UtdanningSvar;
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriVerdi;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.persononinfo.domene.Adressebeskyttelse;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.NestedQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
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
import static no.nav.pto.veilarbportefolje.arbeidssoeker.v2.ArbeidssoekerMapperKt.inkludereSituasjonerFraBadeVeilarbregistreringOgArbeidssoekerregistrering;
import static no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg.JA;
import static no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg.NEI;
import static no.nav.pto.veilarbportefolje.opensearch.domene.StatustallResponse.StatustallAggregationKey;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.opensearch.index.query.QueryBuilders.*;
import static org.opensearch.search.aggregations.AggregationBuilders.filter;
import static org.opensearch.search.aggregations.AggregationBuilders.filters;
import static org.opensearch.search.sort.ScriptSortBuilder.ScriptSortType.STRING;
import static org.opensearch.search.sort.SortMode.MIN;

@Slf4j
public class OpensearchQueryBuilder {

    static String byggVeilederPaaEnhetScript(List<String> veilederePaaEnhet) {
        String veiledere = veilederePaaEnhet.stream()
                .map(id -> format("\"%s\"", id))
                .collect(joining(","));

        String veilederListe = format("[%s]", veiledere);
        return format("(doc.veileder_id.size() != 0 && %s.contains(doc.veileder_id.value)).toString()", veilederListe);
    }

    static BoolQueryBuilder leggTilBrukerinnsynTilgangFilter(BoolQueryBuilder boolQuery, BrukerinnsynTilganger brukerInnsynTilganger, BrukerinnsynTilgangFilterType filterType) {
        return switch (filterType) {
            case BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ -> {
                if (!brukerInnsynTilganger.tilgangTilAdressebeskyttelseStrengtFortrolig()) {
                    boolQuery.mustNot(matchQuery("diskresjonskode", Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode));
                }

                if (!brukerInnsynTilganger.tilgangTilAdressebeskyttelseFortrolig()) {
                    boolQuery.mustNot(matchQuery("diskresjonskode", Adressebeskyttelse.FORTROLIG.diskresjonskode));
                }

                if (!brukerInnsynTilganger.tilgangTilSkjerming()) {
                    boolQuery.mustNot(matchQuery("egen_ansatt", true));
                }

                yield boolQuery;
            }

            case BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ -> {
                BoolQueryBuilder shouldBoolQuery = boolQuery();

                if (!brukerInnsynTilganger.tilgangTilAdressebeskyttelseStrengtFortrolig()) {
                    shouldBoolQuery.should(matchQuery("diskresjonskode", Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode));
                }

                if (!brukerInnsynTilganger.tilgangTilAdressebeskyttelseFortrolig()) {
                    shouldBoolQuery.should(matchQuery("diskresjonskode", Adressebeskyttelse.FORTROLIG.diskresjonskode));
                }

                if (!brukerInnsynTilganger.tilgangTilSkjerming()) {
                    shouldBoolQuery.should(matchQuery("egen_ansatt", true));
                }

                boolQuery.must(shouldBoolQuery);

                yield boolQuery;
            }
        };
    }


    static void leggTilBarnFilter(Filtervalg filtervalg, BoolQueryBuilder boolQuery, Boolean harTilgangKode6, Boolean harTilgangKode7) {
        Boolean tilgangTil6og7 = harTilgangKode6 && harTilgangKode7;
        Boolean tilgangTilKun6 = harTilgangKode6 && !harTilgangKode7;
        Boolean tilgangTil7 = !harTilgangKode6 && harTilgangKode7;

        filtervalg.barnUnder18Aar.forEach(
                harBarnUnder18Aar -> {
                    switch (harBarnUnder18Aar) {
                        case HAR_BARN_UNDER_18_AAR -> {
                            filterForBarnUnder18(boolQuery, tilgangTil6og7, tilgangTilKun6, tilgangTil7);
                        }
                        default -> throw new IllegalStateException("Ingen barn under 18 aar funnet");
                    }
                });
    }

    static void leggTilBarnAlderFilter(BoolQueryBuilder boolQuery, Boolean harTilgangKode6, Boolean harTilgangKode7, int fraAlder, int tilAlder) {
        Boolean tilgangTil6og7 = harTilgangKode6 && harTilgangKode7;
        Boolean tilgangTilKun6 = harTilgangKode6 && !harTilgangKode7;
        Boolean tilgangTil7 = !harTilgangKode6 && harTilgangKode7;

        BoolQueryBuilder barnUnder18aarQueryBuilder = new BoolQueryBuilder();
        if (tilgangTil6og7) {
            barnUnder18aarQueryBuilder
                    .must(existsQuery("barn_under_18_aar"))
                    .must(rangeQuery("barn_under_18_aar.alder").gte(fraAlder).lte(tilAlder));
        } else if (tilgangTilKun6) {
            barnUnder18aarQueryBuilder
                    .must(boolQuery()
                            .should(matchQuery("barn_under_18_aar.diskresjonskode", "-1"))
                            .should(matchQuery("barn_under_18_aar.diskresjonskode", "6"))
                            .should(matchQuery("barn_under_18_aar.diskresjonskode", "19")))
                    .must(rangeQuery("barn_under_18_aar.alder").gte(fraAlder).lte(tilAlder));
        } else if (tilgangTil7) {
            barnUnder18aarQueryBuilder
                    .must(boolQuery()
                            .should(matchQuery("barn_under_18_aar.diskresjonskode", "-1"))
                            .should(matchQuery("barn_under_18_aar.diskresjonskode", "7")))
                    .must(rangeQuery("barn_under_18_aar.alder").gte(fraAlder).lte(tilAlder));
        } else {
            barnUnder18aarQueryBuilder
                    .must(matchQuery("barn_under_18_aar.diskresjonskode", "-1"))
                    .must(rangeQuery("barn_under_18_aar.alder").gte(fraAlder).lte(tilAlder));
        }
        NestedQueryBuilder barnUnder18AarNested = nestedQuery("barn_under_18_aar", barnUnder18aarQueryBuilder, ScoreMode.Avg);
        boolQuery.must(barnUnder18AarNested);
    }

    private static void filterForBarnUnder18(BoolQueryBuilder boolQuery, Boolean tilgangTil6og7, Boolean tilgangTilKun6, Boolean tilgangTil7) {
        BoolQueryBuilder barnUnder18aarQueryBuilder = new BoolQueryBuilder();
        if (tilgangTil6og7) {
            barnUnder18aarQueryBuilder.must(existsQuery("barn_under_18_aar"));
        } else if (tilgangTilKun6) {
            barnUnder18aarQueryBuilder.must(boolQuery()
                    .should(matchQuery("barn_under_18_aar.diskresjonskode", "-1"))
                    .should(matchQuery("barn_under_18_aar.diskresjonskode", "6"))
                    .should(matchQuery("barn_under_18_aar.diskresjonskode", "19")));
        } else if (tilgangTil7) {
            barnUnder18aarQueryBuilder.must(boolQuery()
                    .should(matchQuery("barn_under_18_aar.diskresjonskode", "-1"))
                    .should(matchQuery("barn_under_18_aar.diskresjonskode", "7")));
        } else {
            barnUnder18aarQueryBuilder.must(matchQuery("barn_under_18_aar.diskresjonskode", "-1"));
        }
        NestedQueryBuilder barnUnder18Aar = nestedQuery("barn_under_18_aar", barnUnder18aarQueryBuilder, ScoreMode.Avg);
        boolQuery.must(barnUnder18Aar);
    }


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
        byggManuellFilter(filtervalg.aktiviteterForenklet, queryBuilder, "aktiviteter");
        byggManuellFilter(filtervalg.alleAktiviteter, queryBuilder, "alleAktiviteter");
        byggManuellFilter(filtervalg.innsatsgruppeGjeldendeVedtak14a, queryBuilder, "gjeldendeVedtak14a.innsatsgruppe");
        byggManuellFilter(filtervalg.hovedmalGjeldendeVedtak14a, queryBuilder, "gjeldendeVedtak14a.hovedmal");

        if (filtervalg.harYtelsefilter()) {
            BoolQueryBuilder subQuery = boolQuery();
            filtervalg.ytelse.underytelser.forEach(
                    ytelse -> queryBuilder.must(subQuery.should(matchQuery("ytelse", ytelse.name())))
            );
        }

        if (filtervalg.harKjonnfilter()) {
            queryBuilder.must(matchQuery("kjonn", filtervalg.kjonn.name()));
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

        if (filtervalg.harStillingFraNavFilter()) {
            filtervalg.stillingFraNavFilter.forEach(
                    stillingFraNAVFilter -> {
                        switch (stillingFraNAVFilter) {
                            case CV_KAN_DELES_STATUS_JA ->
                                    queryBuilder.must(matchQuery("neste_cv_kan_deles_status", "JA"));
                            default -> throw new IllegalStateException("Stilling fra Nav ikke funnet");
                        }
                    });
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

        if (filtervalg.harFoedelandFilter()) {
            BoolQueryBuilder subQuery = boolQuery();
            filtervalg.getFoedeland().forEach(
                    foedeLand -> queryBuilder.must(subQuery.should(matchQuery("foedeland", foedeLand)))
            );
        }
        if (filtervalg.harLandgruppeFilter()) {
            BoolQueryBuilder subQuery = boolQuery();
            BoolQueryBuilder subQueryUnkjent = boolQuery();
            filtervalg.getLandgruppe().forEach(
                    landGruppe -> {
                        String landgruppeCode = landGruppe.replace("LANDGRUPPE_", "");
                        if (landgruppeCode.equalsIgnoreCase("UKJENT")) {
                            subQueryUnkjent.mustNot(existsQuery("landgruppe"));
                            subQuery.should(subQueryUnkjent);
                        } else {
                            subQuery.should(matchQuery("landgruppe", landgruppeCode));
                        }
                    }
            );
            queryBuilder.must(subQuery);
        }
        if (filtervalg.harFargeKategoriFilter()) {
            BoolQueryBuilder subQuery = boolQuery();
            BoolQueryBuilder subQueryUnkjent = boolQuery();
            filtervalg.getFargekategorier().forEach(
                    fargeKategori -> {
                        if (FargekategoriVerdi.INGEN_KATEGORI.name().equals(fargeKategori)) {
                            subQueryUnkjent.mustNot(existsQuery("fargekategori"));
                            subQuery.should(subQueryUnkjent);
                        } else {
                            subQuery.should(matchQuery("fargekategori", fargeKategori));
                        }
                    }
            );
            queryBuilder.must(subQuery);
        }
        if (filtervalg.harTalespraaktolkFilter() || filtervalg.harTegnspraakFilter()) {
            BoolQueryBuilder tolkBehovSubquery = boolQuery();
            BoolQueryBuilder tolkBehovTale = boolQuery();
            BoolQueryBuilder tolkBehovTegn = boolQuery();

            if (filtervalg.harTalespraaktolkFilter()) {
                tolkBehovSubquery
                        .should(tolkBehovTale.must(existsQuery("talespraaktolk")))
                        .must(tolkBehovTale.mustNot(matchQuery("talespraaktolk", "")));
            }
            if (filtervalg.harTegnspraakFilter()) {
                tolkBehovSubquery
                        .should(tolkBehovTegn.must(existsQuery("tegnspraaktolk")))
                        .should(tolkBehovTegn.mustNot(matchQuery("tegnspraaktolk", "")));
            }
            queryBuilder.must(tolkBehovSubquery);
        }
        if (filtervalg.harTolkbehovSpraakFilter()) {
            boolean tolkbehovSelected = false;
            BoolQueryBuilder tolkBehovSubquery = boolQuery();

            if (filtervalg.harTalespraaktolkFilter()) {
                filtervalg.getTolkBehovSpraak().forEach(
                        tolkbehovSpraak -> tolkBehovSubquery.should(matchQuery("talespraaktolk", tolkbehovSpraak))
                );
                tolkbehovSelected = true;
            }
            if (filtervalg.harTegnspraakFilter()) {
                filtervalg.getTolkBehovSpraak().forEach(tolkbehovSpraak ->
                        tolkBehovSubquery.should(matchQuery("tegnspraaktolk", tolkbehovSpraak))
                );
                tolkbehovSelected = true;
            }

            if (!tolkbehovSelected) {
                filtervalg.getTolkBehovSpraak().forEach(
                        tolkbehovSpraak -> tolkBehovSubquery.should(matchQuery("talespraaktolk", tolkbehovSpraak))
                );
                filtervalg.getTolkBehovSpraak().forEach(tolkbehovSpraak ->
                        tolkBehovSubquery.should(matchQuery("tegnspraaktolk", tolkbehovSpraak))
                );
            }
            queryBuilder.must(tolkBehovSubquery);
        }

        if (filtervalg.harBostedFilter()) {
            BoolQueryBuilder bostedSubquery = boolQuery();
            filtervalg.getGeografiskBosted().forEach(geografiskBosted -> {
                        bostedSubquery.should(matchQuery("kommunenummer", geografiskBosted));
                        bostedSubquery.should(matchQuery("bydelsnummer", geografiskBosted));
                    }
            );
            queryBuilder.must(bostedSubquery);
        }

        if (filtervalg.harAvvik14aVedtakFilter()) {
            BoolQueryBuilder avvik14aVedtakSubQuery = boolQuery();
            filtervalg.avvik14aVedtak.forEach(avvik14aVedtak ->
                    avvik14aVedtakSubQuery.should(matchQuery("avvik14aVedtak", avvik14aVedtak))
            );
            queryBuilder.must(avvik14aVedtakSubQuery);
        }

        if (filtervalg.harEnsligeForsorgereFilter() && filtervalg.getEnsligeForsorgere().contains(EnsligeForsorgere.OVERGANGSSTONAD)) {
            queryBuilder.must(existsQuery("enslige_forsorgere_overgangsstonad"));
        }

        if (filtervalg.harDinSituasjonSvar()) {
            BoolQueryBuilder brukerensSituasjonSubQuery = boolQuery();
            filtervalg.registreringstype.forEach(jobbSituasjonBeskrivelse -> {
                if (jobbSituasjonBeskrivelse == JobbSituasjonBeskrivelse.INGEN_DATA) {
                    brukerensSituasjonSubQuery.should(boolQuery().mustNot(existsQuery("brukers_situasjoner")));
                } else {
                    inkludereSituasjonerFraBadeVeilarbregistreringOgArbeidssoekerregistrering(jobbSituasjonBeskrivelse).forEach(sokerOrd ->
                            brukerensSituasjonSubQuery.should(matchQuery("brukers_situasjoner", sokerOrd))
                    );
                }

            });
            queryBuilder.must(brukerensSituasjonSubQuery);
        }

        if (filtervalg.harUtdanningSvar()) {
            BoolQueryBuilder brukerensUtdanningSubQuery = boolQuery();
            filtervalg.utdanning.forEach(utdanningSvar -> {
                if (utdanningSvar == UtdanningSvar.INGEN_DATA) {
                    brukerensUtdanningSubQuery.should(boolQuery().mustNot(existsQuery("utdanning")));
                    brukerensUtdanningSubQuery.should(matchQuery("utdanning", "INGEN_SVAR"));
                } else {
                    brukerensUtdanningSubQuery.should(matchQuery("utdanning", utdanningSvar));
                }

            });
            queryBuilder.must(brukerensUtdanningSubQuery);
        }

        if (filtervalg.harUtdanningBestattSvar()) {
            BoolQueryBuilder brukerensUtdanningSubQuery = boolQuery();
            filtervalg.utdanningBestatt.forEach(utdanningSvar -> {
                if (utdanningSvar == UtdanningBestattSvar.INGEN_DATA) {
                    brukerensUtdanningSubQuery.should(boolQuery().mustNot(existsQuery("utdanning_bestatt")));
                    brukerensUtdanningSubQuery.should(matchQuery("utdanning_bestatt", "INGEN_SVAR"));
                } else {
                    brukerensUtdanningSubQuery.should(matchQuery("utdanning_bestatt", utdanningSvar));
                }

            });
            queryBuilder.must(brukerensUtdanningSubQuery);
        }

        if (filtervalg.harUtdanningGodkjentSvar()) {
            BoolQueryBuilder brukerensUtdanningSubQuery = boolQuery();
            filtervalg.utdanningGodkjent.forEach(utdanningSvar -> {
                if (utdanningSvar == UtdanningGodkjentSvar.INGEN_DATA) {
                    brukerensUtdanningSubQuery.should(boolQuery().mustNot(existsQuery("utdanning_godkjent")));
                    brukerensUtdanningSubQuery.should(matchQuery("utdanning_godkjent", "INGEN_SVAR"));
                } else {
                    brukerensUtdanningSubQuery.should(matchQuery("utdanning_godkjent", utdanningSvar));
                }

            });
            queryBuilder.must(brukerensUtdanningSubQuery);
        }

        if (filtervalg.harGjeldendeVedtak14aFilter()) {
            List<GjeldendeVedtak14aFilter> valgteGjeldendeVedtak14aFilter = filtervalg.gjeldendeVedtak14a.stream().map(GjeldendeVedtak14aFilter::valueOf).toList();
            BoolQueryBuilder subQuery = boolQuery();

            if (valgteGjeldendeVedtak14aFilter.contains(GjeldendeVedtak14aFilter.HAR_14A_VEDTAK)
                && !valgteGjeldendeVedtak14aFilter.contains(GjeldendeVedtak14aFilter.HAR_IKKE_14A_VEDTAK)) {
                subQuery.must(existsQuery("gjeldendeVedtak14a"));
                queryBuilder.must(subQuery);
            } else if (valgteGjeldendeVedtak14aFilter.contains(GjeldendeVedtak14aFilter.HAR_IKKE_14A_VEDTAK)
                       && !valgteGjeldendeVedtak14aFilter.contains(GjeldendeVedtak14aFilter.HAR_14A_VEDTAK)) {
                subQuery.mustNot(existsQuery("gjeldendeVedtak14a"));
                queryBuilder.must(subQuery);
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

    /**
     * Tar i mot en {@link SearchSourceBuilder} og utvider denne med sorteringsqueryer basert på valgt sorteringsrekkefølge,
     * sorteringsfelt og valgte filter.
     * <p>
     * Merk: {@link Sorteringsfelt} er en enum som representerer lovlige sorteringsfelter slik frontend har definert dem.
     * Disse mappes til felter i OpenSearch, dvs. for enkelte verdier av {@link Sorteringsfelt} kan det være at feltet
     * det faktisk sorteres på heter noe annet i OpenSearch. Siden {@link OppfolgingsBruker} er "fasiten" for hvilke felter
     * som er lov å sortere på i OpenSearch er det viktig at vi ikke legger til nye sorteringsfelter i {@link Sorteringsfelt}
     * uten å sørge for at disse også er tilgjengelige i {@link OppfolgingsBruker}.
     */
    static SearchSourceBuilder sorterQueryParametere(
            Sorteringsrekkefolge sorteringsrekkefolge,
            Sorteringsfelt sorteringsfelt,
            SearchSourceBuilder searchSourceBuilder,
            Filtervalg filtervalg,
            BrukerinnsynTilganger brukerinnsynTilganger
    ) {
        SortOrder sorteringsrekkefolgeOpenSearch = Sorteringsrekkefolge.STIGENDE.equals(sorteringsrekkefolge) ? SortOrder.ASC : SortOrder.DESC;

        // Vi må assigne til en ny variabel for at kompilatoren sin exhaustiveness-check skal slå inn.
        // Dette er strengt tatt ikke nødvendig da vi bare kunne returnert searchSourceBuilder direkte, men da ville vi
        // mistet exhaustiveness-checken.
        SearchSourceBuilder hovedsortering = switch (sorteringsfelt) {
            case IKKE_SATT -> {
                brukStandardsorteringBasertPaValgteFilter(filtervalg, searchSourceBuilder);
                yield searchSourceBuilder;
            }
            case VALGTE_AKTIVITETER -> {
                sorterValgteAktiviteter(filtervalg, searchSourceBuilder, sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case MOTER_MED_NAV_IDAG -> {
                searchSourceBuilder.sort("alle_aktiviteter_mote_startdato", sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case MOTESTATUS -> {
                searchSourceBuilder.sort("aktivitet_mote_startdato", sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case I_AVTALT_AKTIVITET -> {
                FieldSortBuilder builder = new FieldSortBuilder("aktivitet_utlopsdatoer")
                        .order(sorteringsrekkefolgeOpenSearch)
                        .sortMode(MIN);
                searchSourceBuilder.sort(builder);
                yield searchSourceBuilder;
            }
            case FODSELSNUMMER -> {
                searchSourceBuilder.sort("fnr.raw", sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case UTLOPTE_AKTIVITETER -> {
                searchSourceBuilder.sort("nyesteutlopteaktivitet", sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case AAP_TYPE -> {
                searchSourceBuilder.sort("ytelse", sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case AAP_VURDERINGSFRIST -> {
                sorterAapVurderingsfrist(searchSourceBuilder, sorteringsrekkefolgeOpenSearch, filtervalg);
                yield searchSourceBuilder;
            }
            case AAP_RETTIGHETSPERIODE -> {
                sorterAapRettighetsPeriode(searchSourceBuilder, sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case GJELDENDE_VEDTAK_14A_INNSATSGRUPPE -> {
                searchSourceBuilder.sort("gjeldendeVedtak14a.innsatsgruppe", sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case GJELDENDE_VEDTAK_14A_HOVEDMAL -> {
                searchSourceBuilder.sort("gjeldendeVedtak14a.hovedmal", sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case GJELDENDE_VEDTAK_14A_VEDTAKSDATO -> {
                sorterGjeldendeVedtak14aVedtaksdato(searchSourceBuilder, sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case UTKAST_14A_STATUS -> {
                searchSourceBuilder.sort("utkast_14a_status", sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case SISTE_ENDRING_DATO -> {
                sorterSisteEndringTidspunkt(searchSourceBuilder, sorteringsrekkefolgeOpenSearch, filtervalg);
                yield searchSourceBuilder;
            }
            case FODELAND -> {
                sorterFodeland(searchSourceBuilder, sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case STATSBORGERSKAP -> {
                sorterStatsborgerskap(searchSourceBuilder, sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case STATSBORGERSKAP_GYLDIG_FRA -> {
                sorterStatsborgerskapGyldigFra(searchSourceBuilder, sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case TOLKESPRAK -> {
                sorterTolkeSpraak(filtervalg, searchSourceBuilder, sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case TOLKEBEHOV_SIST_OPPDATERT -> {
                searchSourceBuilder.sort("tolkBehovSistOppdatert", sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case ENSLIGE_FORSORGERE_UTLOP_YTELSE -> {
                sorterEnsligeForsorgereUtlopsDato(searchSourceBuilder, sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case ENSLIGE_FORSORGERE_VEDTAKSPERIODETYPE -> {
                sorterEnsligeForsorgereVedtaksPeriode(searchSourceBuilder, sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case ENSLIGE_FORSORGERE_AKTIVITETSPLIKT -> {
                sorterEnsligeForsorgereAktivitetsPlikt(searchSourceBuilder, sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case ENSLIGE_FORSORGERE_OM_BARNET -> {
                sorterEnsligeForsorgereOmBarnet(searchSourceBuilder, sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case BARN_UNDER_18_AR -> {
                sorterBarnUnder18(searchSourceBuilder, sorteringsrekkefolgeOpenSearch, brukerinnsynTilganger, filtervalg);
                yield searchSourceBuilder;
            }
            case BRUKERS_SITUASJON_SIST_ENDRET -> {
                searchSourceBuilder.sort("brukers_situasjon_sist_endret", sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case UTDANNING_OG_SITUASJON_SIST_ENDRET -> {
                searchSourceBuilder.sort("utdanning_og_situasjon_sist_endret", sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case HUSKELAPP_FRIST -> {
                sorterHuskelappFrist(searchSourceBuilder, sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case HUSKELAPP -> {
                sorterHuskelappEksistere(searchSourceBuilder, sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case HUSKELAPP_KOMMENTAR -> {
                searchSourceBuilder.sort("huskelapp.kommentar", sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case FARGEKATEGORI -> {
                searchSourceBuilder.sort("fargekategori", sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case TILTAKSHENDELSE_DATO_OPPRETTET -> {
                sorterTiltakshendelseOpprettetDato(searchSourceBuilder, sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case TILTAKSHENDELSE_TEKST -> {
                searchSourceBuilder.sort("tiltakshendelse.tekst", sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            case UTGATT_VARSEL_DATO -> {
                sorterUtgattVarselHendelseDato(searchSourceBuilder, sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
            // Vi har eksplisitt latt være å definere en "default" case i switch-en for å tvinge oss selv til å håndtere
            // alle sorteringsfeltene (exhaustivness check som gjøres av kompilatoren). Så i praksis er dette default-tilfellet.
            case ETTERNAVN, CV_SVARFRIST, AAP_MAXTID_UKE, AAP_UNNTAK_UKER_IGJEN, VENTER_PA_SVAR_FRA_NAV,
                 VENTER_PA_SVAR_FRA_BRUKER, STARTDATO_FOR_AVTALT_AKTIVITET, NESTE_STARTDATO_FOR_AVTALT_AKTIVITET,
                 FORRIGE_DATO_FOR_AVTALT_AKTIVITET, UTKAST_14A_STATUS_ENDRET, UTKAST_14A_ANSVARLIG_VEILEDER,
                 BOSTED_KOMMUNE, BOSTED_BYDEL, BOSTED_SIST_OPPDATERT, OPPFOLGING_STARTET, UTLOPSDATO, VEILEDER_IDENT,
                 DAGPENGER_UTLOP_UKE, DAGPENGER_PERM_UTLOP_UKE -> {
                searchSourceBuilder.sort(sorteringsfelt.sorteringsverdi, sorteringsrekkefolgeOpenSearch);
                yield searchSourceBuilder;
            }
        };
        addSecondarySort(hovedsortering);
        return hovedsortering;
    }

    private static void brukStandardsorteringBasertPaValgteFilter(Filtervalg filtervalg, SearchSourceBuilder searchSourceBuilder) {
        boolean filtrertPaTiltakshendelse = filtervalg.ferdigfilterListe != null && filtervalg.ferdigfilterListe.contains(Brukerstatus.TILTAKSHENDELSER);
        boolean filtrertPaUtgatteVarsel = filtervalg.ferdigfilterListe != null && filtervalg.ferdigfilterListe.contains(Brukerstatus.UTGATTE_VARSEL);
        boolean filtrertPaEtGjeldendeVedtak14aFilter = filtervalg.gjeldendeVedtak14a.contains("HAR_14A_VEDTAK") ||
                                                       (filtervalg.innsatsgruppeGjeldendeVedtak14a != null && !filtervalg.innsatsgruppeGjeldendeVedtak14a.isEmpty()) ||
                                                       (filtervalg.hovedmalGjeldendeVedtak14a != null && !filtervalg.hovedmalGjeldendeVedtak14a.isEmpty());

        if (filtrertPaTiltakshendelse) {
            sorterTiltakshendelseOpprettetDato(searchSourceBuilder, SortOrder.ASC);
        } else if (filtrertPaUtgatteVarsel) {
            sorterUtgattVarselHendelseDato(searchSourceBuilder, SortOrder.ASC);
        } else if (filtrertPaEtGjeldendeVedtak14aFilter) {
            sorterGjeldendeVedtak14aVedtaksdato(searchSourceBuilder, SortOrder.ASC);
        } else {
            searchSourceBuilder.sort("aktoer_id", SortOrder.ASC);
        }
    }

    static void sorterSisteEndringTidspunkt(SearchSourceBuilder builder, SortOrder order, Filtervalg filtervalg) {
        if (filtervalg.sisteEndringKategori.size() == 0) {
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

    static void sorterFodeland(SearchSourceBuilder searchSourceBuilder, SortOrder order) {
        searchSourceBuilder.sort("foedelandFulltNavn", order);
    }

    static void sorterStatsborgerskap(SearchSourceBuilder searchSourceBuilder, SortOrder order) {
        searchSourceBuilder.sort("hovedStatsborgerskap.statsborgerskap", order);
    }

    static void sorterStatsborgerskapGyldigFra(SearchSourceBuilder searchSourceBuilder, SortOrder order) {
        searchSourceBuilder.sort("hovedStatsborgerskap.gyldigFra", order);
    }

    static void sorterTiltakshendelseOpprettetDato(SearchSourceBuilder searchSourceBuilder, SortOrder order) {
        searchSourceBuilder.sort("tiltakshendelse.opprettet", order);
    }

    static void sorterUtgattVarselHendelseDato(SearchSourceBuilder searchSourceBuilder, SortOrder order) {
        searchSourceBuilder.sort("utgatt_varsel.dato", order);
    }

    static void sorterGjeldendeVedtak14aVedtaksdato(SearchSourceBuilder searchSourceBuilder, SortOrder order) {
        searchSourceBuilder.sort("gjeldendeVedtak14a.fattetDato", order);
    }

    static void sorterTolkeSpraak(Filtervalg filtervalg, SearchSourceBuilder searchSourceBuilder, SortOrder order) {
        if (filtervalg.harTalespraaktolkFilter()) {
            searchSourceBuilder.sort("talespraaktolk", order);
        }
        if (filtervalg.harTegnspraakFilter()) {
            searchSourceBuilder.sort("tegnspraaktolk", order);
        }
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

    static void sorterAapVurderingsfrist(SearchSourceBuilder builder, SortOrder order, Filtervalg filtervalg) {
        String expression = "";
        if (filtervalg.harYtelsefilter() && filtervalg.ytelse.equals(YtelseFilter.AAP)) {
            expression = """
                    if (doc.containsKey('aapunntakukerigjen') && !doc['aapunntakukerigjen'].empty && doc['aapunntakukerigjen'].value != 0) {
                        return doc['utlopsdato'].value.toInstant().toEpochMilli();
                    }
                    else if (doc.containsKey('aapordinerutlopsdato') && !doc['aapordinerutlopsdato'].empty) {
                        return doc['aapordinerutlopsdato'].value.toInstant().toEpochMilli();
                    }
                    else if (doc.containsKey('aapmaxtiduke')) {
                        // Legger til 01.01.2050 i millis for å sortere bak de som har dato
                        return 2524653462000.0 + doc['aapmaxtiduke'].value;
                    }
                    else {
                       return 0;
                    }
                    """;
        } else if (filtervalg.harYtelsefilter() && filtervalg.ytelse.equals(YtelseFilter.AAP_MAXTID)) {
            expression = """
                    if (doc.containsKey('aapordinerutlopsdato') && !doc['aapordinerutlopsdato'].empty) {
                        return doc['aapordinerutlopsdato'].value.toInstant().toEpochMilli();
                    }
                    else if (doc.containsKey('aapmaxtiduke') && !doc['aapmaxtiduke'].empty) {
                        // Legger til 01.01.2050 i millis for å sortere bak de som har dato
                        return 2524653462000.0 + doc['aapmaxtiduke'].value;
                    }
                    else {
                        return 0;
                    }
                    """;
        } else if (filtervalg.harYtelsefilter() && filtervalg.ytelse.equals(YtelseFilter.AAP_UNNTAK)) {
            expression = """
                    if (doc.containsKey('utlopsdato') && !doc['utlopsdato'].empty) {
                        return doc['utlopsdato'].value.toInstant().toEpochMilli();
                    }
                    else {
                        return 0;
                    }
                    """;

        }

        if (!expression.isEmpty()) {
            Script script = new Script(expression);
            ScriptSortBuilder scriptBuilder = new ScriptSortBuilder(script, ScriptSortBuilder.ScriptSortType.NUMBER);
            scriptBuilder.order(order);
            builder.sort(scriptBuilder);
        }
    }

    static SearchSourceBuilder sorterValgteAktiviteter(Filtervalg filtervalg, SearchSourceBuilder builder, SortOrder order) {
        List<String> sorterings_aktiviter;
        if (filtervalg.harAktiviteterForenklet()) {
            sorterings_aktiviter = filtervalg.aktiviteterForenklet;
        } else if (!filtervalg.tiltakstyper.isEmpty()) {
            sorterings_aktiviter = List.of("TILTAK");
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

    private static void sorterHuskelappFrist(SearchSourceBuilder builder, SortOrder order) {
        String expresion = """
                if (doc.containsKey('huskelapp.frist') && !doc['huskelapp.frist'].empty) {
                    return doc['huskelapp.frist'].value.toInstant().toEpochMilli();
                }
                else {
                    return -1;
                }
                """;
        Script script = new Script(expresion);
        ScriptSortBuilder scriptBuilder = new ScriptSortBuilder(script, ScriptSortBuilder.ScriptSortType.NUMBER);
        scriptBuilder.order(order);
        builder.sort(scriptBuilder);
    }

    private static void sorterHuskelappEksistere(SearchSourceBuilder builder, SortOrder order) {
        String expresion = """
                // Når man sorterer huskelapp-kolonnen (ikon-knappen), skal de som har frist sorteres først, med nærmeste utløpsdato øverst
                if (!doc['huskelapp.frist'].empty) {
                    // Trekker fra (fra DateUtils.java) FAR_IN_THE_FUTURE_DATE = "3017-10-07T00:00:00Z" i millis
                    return doc['huskelapp.frist'].value.toInstant().toEpochMilli() - 33064243200000.0;
                }
                else if (!doc['huskelapp.kommentar'].empty) {
                    return 0;
                }
                else {
                    // Returnerer 3017.10.07 + 1 i millis
                    return 33064243200001.0;
                }
                """;
        Script script = new Script(expresion);
        ScriptSortBuilder scriptBuilder = new ScriptSortBuilder(script, ScriptSortBuilder.ScriptSortType.NUMBER);
        scriptBuilder.order(order);
        builder.sort(scriptBuilder);
    }

    static QueryBuilder leggTilFerdigFilter(Brukerstatus brukerStatus, List<String> veiledereMedTilgangTilEnhet) {
        QueryBuilder queryBuilder;
        switch (brukerStatus) {
            case UFORDELTE_BRUKERE:
                queryBuilder = byggUfordeltBrukereQuery(veiledereMedTilgangTilEnhet);
                break;
            case TRENGER_OPPFOLGINGSVEDTAK:
                queryBuilder = byggTrengerOppfolgingsvedtakFilter();
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
            case MINE_HUSKELAPPER:
                queryBuilder = existsQuery("huskelapp");
                break;
            case NYE_BRUKERE_FOR_VEILEDER:
                queryBuilder = matchQuery("ny_for_veileder", true);
                break;
            case MOTER_IDAG:
                queryBuilder = byggMoteMedNavIdag();
                break;
            case ER_SYKMELDT_MED_ARBEIDSGIVER:
                queryBuilder = byggErSykmeldtMedArbeidsgiverFilter();
                break;
            case UNDER_VURDERING:
                queryBuilder = existsQuery("utkast_14a_status");
                break;
            case TILTAKSHENDELSER:
                queryBuilder = existsQuery("tiltakshendelse");
                break;
            case UTGATTE_VARSEL:
                queryBuilder = existsQuery("utgatt_varsel");
                break;
            default:
                throw new IllegalStateException();

        }
        return queryBuilder;
    }

    static QueryBuilder byggTrengerOppfolgingsvedtakFilter() {
        return boolQuery()
                .mustNot(existsQuery("gjeldendeVedtak14a"));
    }

    static QueryBuilder byggErSykmeldtMedArbeidsgiverFilter() {
        return boolQuery()
                .must(matchQuery("er_sykmeldt_med_arbeidsgiver", true))
                .mustNot(existsQuery("utkast_14a_status"));
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

    static SearchSourceBuilder byggStatustallQuery(BoolQueryBuilder filtrereVeilederOgEnhet, List<String> veiledereMedTilgangTilEnhet) {
        FiltersAggregator.KeyedFilter[] filtre = new FiltersAggregator.KeyedFilter[]{
                erSykemeldtMedArbeidsgiverFilter(filtrereVeilederOgEnhet),
                mustExistFilter(filtrereVeilederOgEnhet, StatustallAggregationKey.I_AVTALT_AKTIVITET.key, "aktiviteter"),
                ikkeIavtaltAktivitet(filtrereVeilederOgEnhet),
                inaktiveBrukere(filtrereVeilederOgEnhet),
                mustBeTrueFilter(filtrereVeilederOgEnhet, StatustallAggregationKey.NYE_BRUKERE_FOR_VEILEDER.key, "ny_for_veileder"),
                totalt(filtrereVeilederOgEnhet),
                mustNotExistFilter(filtrereVeilederOgEnhet, StatustallAggregationKey.TRENGER_OPPFOLGINGSVEDTAK.key, "gjeldendeVedtak14a"),
                mustExistFilter(filtrereVeilederOgEnhet, StatustallAggregationKey.VENTER_PA_SVAR_FRA_NAV.key, "venterpasvarfranav"),
                mustExistFilter(filtrereVeilederOgEnhet, StatustallAggregationKey.VENTER_PA_SVAR_FRA_BRUKER.key, "venterpasvarfrabruker"),
                ufordelteBrukere(filtrereVeilederOgEnhet, veiledereMedTilgangTilEnhet),
                mustExistFilter(filtrereVeilederOgEnhet, StatustallAggregationKey.UTLOPTE_AKTIVITETER.key, "nyesteutlopteaktivitet"),
                moterMedNavIdag(filtrereVeilederOgEnhet),
                mustExistFilter(filtrereVeilederOgEnhet, StatustallAggregationKey.UNDER_VURDERING.key, "utkast_14a_status"),
                mustMatchQuery(filtrereVeilederOgEnhet, StatustallAggregationKey.FARGEKATEGORI_A.key, "fargekategori", FargekategoriVerdi.FARGEKATEGORI_A.name()),
                mustMatchQuery(filtrereVeilederOgEnhet, StatustallAggregationKey.FARGEKATEGORI_B.key, "fargekategori", FargekategoriVerdi.FARGEKATEGORI_B.name()),
                mustMatchQuery(filtrereVeilederOgEnhet, StatustallAggregationKey.FARGEKATEGORI_C.key, "fargekategori", FargekategoriVerdi.FARGEKATEGORI_C.name()),
                mustMatchQuery(filtrereVeilederOgEnhet, StatustallAggregationKey.FARGEKATEGORI_D.key, "fargekategori", FargekategoriVerdi.FARGEKATEGORI_D.name()),
                mustMatchQuery(filtrereVeilederOgEnhet, StatustallAggregationKey.FARGEKATEGORI_E.key, "fargekategori", FargekategoriVerdi.FARGEKATEGORI_E.name()),
                mustMatchQuery(filtrereVeilederOgEnhet, StatustallAggregationKey.FARGEKATEGORI_F.key, "fargekategori", FargekategoriVerdi.FARGEKATEGORI_F.name()),
                mustNotExistFilter(filtrereVeilederOgEnhet, StatustallAggregationKey.FARGEKATEGORI_INGEN_KATEGORI.key, "fargekategori"),
                mustExistFilter(filtrereVeilederOgEnhet, StatustallAggregationKey.MINE_HUSKELAPPER.key, "huskelapp"),
                mustExistFilter(filtrereVeilederOgEnhet, StatustallAggregationKey.TILTAKSHENDELSER.key, "tiltakshendelse"),
                mustExistFilter(filtrereVeilederOgEnhet, StatustallAggregationKey.UTGATTE_VARSEL.key, "utgatt_varsel")
        };

        return new SearchSourceBuilder()
                .size(0)
                .aggregation(filters("statustall", filtre));
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
                matchQuery("siste_endringer." + kategori + ".er_sett", "N")
        ));
        queryBuilder.must(orQuery);
    }

    private static void byggSisteEndringFilter(List<String> sisteEndringKategori, BoolQueryBuilder queryBuilder) {
        if (sisteEndringKategori.size() == 0) {
            return;
        }
        if (sisteEndringKategori.size() != 1) {
            log.error("Det ble filtrert på flere ulike siste endringer: {}", sisteEndringKategori.size());
            throw new IllegalStateException("Filtrering på flere siste_endringer er ikke tilatt.");
        }
        queryBuilder.must(existsQuery("siste_endringer." + sisteEndringKategori.get(0)));
    }

    private static void sorterEnsligeForsorgereUtlopsDato(SearchSourceBuilder builder, SortOrder order) {
        String expresion = """
                if (doc.containsKey('enslige_forsorgere_overgangsstonad.utlopsDato') && !doc['enslige_forsorgere_overgangsstonad.utlopsDato'].empty) {
                    return doc['enslige_forsorgere_overgangsstonad.utlopsDato'].value.toInstant().toEpochMilli();
                }
                else {
                  return 0;
                }
                """;
        Script script = new Script(expresion);
        ScriptSortBuilder scriptBuilder = new ScriptSortBuilder(script, ScriptSortBuilder.ScriptSortType.NUMBER);
        scriptBuilder.order(order);
        builder.sort(scriptBuilder);

    }

    private static void sorterEnsligeForsorgereOmBarnet(SearchSourceBuilder builder, SortOrder order) {
        String expresion = """
                if (doc.containsKey('enslige_forsorgere_overgangsstonad.yngsteBarnsFødselsdato') && !doc['enslige_forsorgere_overgangsstonad.yngsteBarnsFødselsdato'].empty) {
                    return doc['enslige_forsorgere_overgangsstonad.yngsteBarnsFødselsdato'].value.toInstant().toEpochMilli();
                }
                else {
                    return 0;
                }
                """;
        Script script = new Script(expresion);
        ScriptSortBuilder scriptBuilder = new ScriptSortBuilder(script, ScriptSortBuilder.ScriptSortType.NUMBER);
        scriptBuilder.order(order);
        builder.sort(scriptBuilder);
    }

    private static void sorterBarnUnder18(SearchSourceBuilder searchSourceBuilder, SortOrder order, BrukerinnsynTilganger brukerinnsynTilganger, Filtervalg filtervalg) {
        Boolean harTilgangKode6 = brukerinnsynTilganger.tilgangTilAdressebeskyttelseStrengtFortrolig();
        Boolean harTilgangKode7 = brukerinnsynTilganger.tilgangTilAdressebeskyttelseFortrolig();
        Boolean hartilgangKode6og7 = harTilgangKode6 && harTilgangKode7;

        Boolean harBaretilgangKode6 = harTilgangKode6 && !harTilgangKode7;
        Boolean harBaretilgangKode7 = !harTilgangKode6 && harTilgangKode7;


        if (filtervalg.harBarnUnder18AarFilter()) {
            String expressionTilgang6og7 = """
                    params._source.barn_under_18_aar.size()
                    """;

            String expression6 = """
                    int count = 0;
                    for (item in params._source.barn_under_18_aar) {
                        if ((item.diskresjonskode == null) || (item.diskresjonskode == '6') || (item.diskresjonskode == '19')){ count = count + 1; }
                    }
                    return count;
                    """;

            String expression7 = """
                    int count = 0;
                    for (item in params._source.barn_under_18_aar) {
                        if ((item.diskresjonskode == null) || (item.diskresjonskode == '7')){ count = count + 1; }
                    }
                    return count;
                    """;

            String expressionIngen = """
                    int count = 0;
                    for (item in params._source.barn_under_18_aar) {
                        if (item.diskresjonskode == null){ count = count + 1; }
                    }
                    return count;
                    """;

            String expressionToUse = "";
            if (hartilgangKode6og7) {
                expressionToUse = expressionTilgang6og7;
            } else if (harBaretilgangKode6) {
                expressionToUse = expression6;
            } else if (harBaretilgangKode7) {
                expressionToUse = expression7;
            } else {
                expressionToUse = expressionIngen;
            }

            Script script = new Script(expressionToUse);
            ScriptSortBuilder scriptBuilder = new ScriptSortBuilder(script, ScriptSortBuilder.ScriptSortType.NUMBER);
            scriptBuilder.order(order);
            searchSourceBuilder.sort(scriptBuilder);
        }
    }

    private static void sorterEnsligeForsorgereVedtaksPeriode(SearchSourceBuilder builder, SortOrder
            order) {
        builder.sort("enslige_forsorgere_overgangsstonad.vedtaksPeriodetype", order);
    }

    private static void sorterEnsligeForsorgereAktivitetsPlikt(SearchSourceBuilder builder, SortOrder order) {
        builder.sort("enslige_forsorgere_overgangsstonad.harAktivitetsplikt", order);
    }

    private static RangeQueryBuilder byggMoteMedNavIdag() {
        LocalDate localDate = LocalDate.now();
        return rangeQuery("alle_aktiviteter_mote_startdato")
                .gte(toIsoUTC(localDate.atStartOfDay()))
                .lt(toIsoUTC(localDate.plusDays(1).atStartOfDay()));
    }

    private static FiltersAggregator.KeyedFilter erSykemeldtMedArbeidsgiverFilter(BoolQueryBuilder filtrereVeilederOgEnhet) {
        BoolQueryBuilder boolQueryBuilder = boolQuery()
                .must(filtrereVeilederOgEnhet)
                .must(termQuery("er_sykmeldt_med_arbeidsgiver", true))
                .mustNot(existsQuery("utkast_14a_status"));

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

    private static FiltersAggregator.KeyedFilter moterMedNavIdag(BoolQueryBuilder filtrereVeilederOgEnhet) {
        return new FiltersAggregator.KeyedFilter(
                StatustallAggregationKey.MOTER_MED_NAV_I_DAG.key,
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .must(byggMoteMedNavIdag())
        );
    }

    private static FiltersAggregator.KeyedFilter ufordelteBrukere(BoolQueryBuilder filtrereVeilederOgEnhet, List<String> veiledereMedTilgangTilEnhet) {
        return new FiltersAggregator.KeyedFilter(
                StatustallAggregationKey.UFORDELTE_BRUKERE.key,
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .must(byggUfordeltBrukereQuery(veiledereMedTilgangTilEnhet))
        );
    }

    private static FiltersAggregator.KeyedFilter totalt(BoolQueryBuilder filtrereVeilederOgEnhet) {
        return new FiltersAggregator.KeyedFilter(
                StatustallAggregationKey.TOTALT.key,
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
        );
    }

    private static FiltersAggregator.KeyedFilter mustBeTrueFilter(BoolQueryBuilder filtrereVeilederOgEnhet, String key, String term) {
        return new FiltersAggregator.KeyedFilter(
                key,
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .must(termQuery(term, true))

        );
    }

    private static FiltersAggregator.KeyedFilter inaktiveBrukere(BoolQueryBuilder filtrereVeilederOgEnhet) {
        return new FiltersAggregator.KeyedFilter(
                StatustallAggregationKey.INAKTIVE_BRUKERE.key,
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
                StatustallAggregationKey.IKKE_I_AVTALT_AKTIVITET.key,
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .mustNot(existsQuery("aktiviteter"))

        );
    }

    private static FiltersAggregator.KeyedFilter mustExistFilter(BoolQueryBuilder filtrereVeilederOgEnhet, String key, String term) {
        return new FiltersAggregator.KeyedFilter(
                key,
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .must(existsQuery(term))
        );
    }

    private static FiltersAggregator.KeyedFilter mustNotExistFilter(BoolQueryBuilder filtrereVeilederOgEnhet, String key, String term) {
        return new FiltersAggregator.KeyedFilter(
                key,
                boolQuery()
                        .must(filtrereVeilederOgEnhet)
                        .mustNot(existsQuery(term))
        );
    }

    private static void addSecondarySort(SearchSourceBuilder searchSourceBuilder) {
        searchSourceBuilder.sort("aktoer_id", SortOrder.ASC);
    }
}

