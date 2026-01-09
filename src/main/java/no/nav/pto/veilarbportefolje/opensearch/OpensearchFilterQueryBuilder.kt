package no.nav.pto.veilarbportefolje.opensearch

import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.JobbSituasjonBeskrivelse
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.inkludereSituasjonerFraBadeVeilarbregistreringOgArbeidssoekerregistrering
import no.nav.pto.veilarbportefolje.auth.BrukerinnsynTilganger
import no.nav.pto.veilarbportefolje.domene.YtelseMapping
import no.nav.pto.veilarbportefolje.domene.filtervalg.*
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriVerdi
import no.nav.pto.veilarbportefolje.opensearch.domene.StatustallResponse.StatustallAggregationKey
import no.nav.pto.veilarbportefolje.persononinfo.domene.Adressebeskyttelse
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori
import no.nav.pto.veilarbportefolje.util.DateUtils
import org.apache.commons.lang3.StringUtils
import org.apache.lucene.search.join.ScoreMode
import org.opensearch.index.query.BoolQueryBuilder
import org.opensearch.index.query.QueryBuilder
import org.opensearch.index.query.QueryBuilders
import org.opensearch.index.query.RangeQueryBuilder
import org.opensearch.search.aggregations.AggregationBuilders
import org.opensearch.search.aggregations.BucketOrder
import org.opensearch.search.aggregations.bucket.filter.FiltersAggregator
import org.opensearch.search.builder.SearchSourceBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors

@Service
class OpensearchFilterQueryBuilder {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(OpensearchFilterQueryBuilder::class.java)
    }

    fun leggTilBrukerinnsynTilgangFilter(
        boolQuery: BoolQueryBuilder,
        brukerInnsynTilganger: BrukerinnsynTilganger,
        filterType: BrukerinnsynTilgangFilterType
    ): BoolQueryBuilder {
        return when (filterType) {
            BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ -> {
                if (!brukerInnsynTilganger.tilgangTilAdressebeskyttelseStrengtFortrolig) {
                    boolQuery.mustNot(
                        QueryBuilders.matchQuery(
                            "diskresjonskode",
                            Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode
                        )
                    )
                }

                if (!brukerInnsynTilganger.tilgangTilAdressebeskyttelseFortrolig) {
                    boolQuery.mustNot(
                        QueryBuilders.matchQuery(
                            "diskresjonskode",
                            Adressebeskyttelse.FORTROLIG.diskresjonskode
                        )
                    )
                }

                if (!brukerInnsynTilganger.tilgangTilSkjerming) {
                    boolQuery.mustNot(QueryBuilders.matchQuery("egen_ansatt", true))
                }

                boolQuery
            }

            BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ -> {
                val shouldBoolQuery = QueryBuilders.boolQuery()

                if (!brukerInnsynTilganger.tilgangTilAdressebeskyttelseStrengtFortrolig) {
                    shouldBoolQuery.should(
                        QueryBuilders.matchQuery(
                            "diskresjonskode",
                            Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode
                        )
                    )
                }

                if (!brukerInnsynTilganger.tilgangTilAdressebeskyttelseFortrolig) {
                    shouldBoolQuery.should(
                        QueryBuilders.matchQuery(
                            "diskresjonskode",
                            Adressebeskyttelse.FORTROLIG.diskresjonskode
                        )
                    )
                }

                if (!brukerInnsynTilganger.tilgangTilSkjerming) {
                    shouldBoolQuery.should(QueryBuilders.matchQuery("egen_ansatt", true))
                }

                boolQuery.must(shouldBoolQuery)

                boolQuery
            }
        }
    }


    fun leggTilBarnFilter(
        filtervalg: Filtervalg,
        boolQuery: BoolQueryBuilder,
        harTilgangKode6: Boolean,
        harTilgangKode7: Boolean
    ) {
        val tilgangTil6og7 = harTilgangKode6 && harTilgangKode7
        val tilgangTilKun6 = harTilgangKode6 && !harTilgangKode7
        val tilgangTil7 = !harTilgangKode6 && harTilgangKode7

        filtervalg.barnUnder18Aar.forEach(
            Consumer { harBarnUnder18Aar: BarnUnder18Aar? ->
                when (harBarnUnder18Aar) {
                    BarnUnder18Aar.HAR_BARN_UNDER_18_AAR -> {
                        filterForBarnUnder18(boolQuery, tilgangTil6og7, tilgangTilKun6, tilgangTil7)
                    }

                    else -> throw IllegalStateException("Ingen barn under 18 aar funnet")
                }
            })
    }

    fun leggTilBarnAlderFilter(
        boolQuery: BoolQueryBuilder,
        harTilgangKode6: Boolean,
        harTilgangKode7: Boolean,
        fraAlder: Int,
        tilAlder: Int
    ) {
        val tilgangTil6og7 = harTilgangKode6 && harTilgangKode7
        val tilgangTilKun6 = harTilgangKode6 && !harTilgangKode7
        val tilgangTil7 = !harTilgangKode6 && harTilgangKode7

        val barnUnder18aarQueryBuilder = BoolQueryBuilder()
        if (tilgangTil6og7) {
            barnUnder18aarQueryBuilder
                .must(QueryBuilders.existsQuery("barn_under_18_aar"))
                .must(QueryBuilders.rangeQuery("barn_under_18_aar.alder").gte(fraAlder).lte(tilAlder))
        } else if (tilgangTilKun6) {
            barnUnder18aarQueryBuilder
                .must(
                    QueryBuilders.boolQuery()
                        .should(QueryBuilders.matchQuery("barn_under_18_aar.diskresjonskode", "-1"))
                        .should(QueryBuilders.matchQuery("barn_under_18_aar.diskresjonskode", "6"))
                        .should(QueryBuilders.matchQuery("barn_under_18_aar.diskresjonskode", "19"))
                )
                .must(QueryBuilders.rangeQuery("barn_under_18_aar.alder").gte(fraAlder).lte(tilAlder))
        } else if (tilgangTil7) {
            barnUnder18aarQueryBuilder
                .must(
                    QueryBuilders.boolQuery()
                        .should(QueryBuilders.matchQuery("barn_under_18_aar.diskresjonskode", "-1"))
                        .should(QueryBuilders.matchQuery("barn_under_18_aar.diskresjonskode", "7"))
                )
                .must(QueryBuilders.rangeQuery("barn_under_18_aar.alder").gte(fraAlder).lte(tilAlder))
        } else {
            barnUnder18aarQueryBuilder
                .must(QueryBuilders.matchQuery("barn_under_18_aar.diskresjonskode", "-1"))
                .must(QueryBuilders.rangeQuery("barn_under_18_aar.alder").gte(fraAlder).lte(tilAlder))
        }
        val barnUnder18AarNested =
            QueryBuilders.nestedQuery("barn_under_18_aar", barnUnder18aarQueryBuilder, ScoreMode.Avg)
        boolQuery.must(barnUnder18AarNested)
    }

    private fun filterForBarnUnder18(
        boolQuery: BoolQueryBuilder,
        tilgangTil6og7: Boolean,
        tilgangTilKun6: Boolean,
        tilgangTil7: Boolean
    ) {
        val barnUnder18aarQueryBuilder = BoolQueryBuilder()
        if (tilgangTil6og7) {
            barnUnder18aarQueryBuilder.must(QueryBuilders.existsQuery("barn_under_18_aar"))
        } else if (tilgangTilKun6) {
            barnUnder18aarQueryBuilder.must(
                QueryBuilders.boolQuery()
                    .should(QueryBuilders.matchQuery("barn_under_18_aar.diskresjonskode", "-1"))
                    .should(QueryBuilders.matchQuery("barn_under_18_aar.diskresjonskode", "6"))
                    .should(QueryBuilders.matchQuery("barn_under_18_aar.diskresjonskode", "19"))
            )
        } else if (tilgangTil7) {
            barnUnder18aarQueryBuilder.must(
                QueryBuilders.boolQuery()
                    .should(QueryBuilders.matchQuery("barn_under_18_aar.diskresjonskode", "-1"))
                    .should(QueryBuilders.matchQuery("barn_under_18_aar.diskresjonskode", "7"))
            )
        } else {
            barnUnder18aarQueryBuilder.must(QueryBuilders.matchQuery("barn_under_18_aar.diskresjonskode", "-1"))
        }
        val barnUnder18Aar = QueryBuilders.nestedQuery("barn_under_18_aar", barnUnder18aarQueryBuilder, ScoreMode.Avg)
        boolQuery.must(barnUnder18Aar)
    }


    fun leggTilManuelleFilter(queryBuilder: BoolQueryBuilder, filtervalg: Filtervalg) {
        if (filtervalg.alder.isNotEmpty()) {
            val subQuery = QueryBuilders.boolQuery()
            filtervalg.alder.forEach(Consumer { alder: String -> byggAlderQuery(alder, subQuery) })
            queryBuilder.must(subQuery)
        }

        val fodseldagIMndQuery =
            filtervalg.fodselsdagIMnd.stream().map { s: String -> s.toInt() }.collect(Collectors.toList())

        byggManuellFilter(fodseldagIMndQuery, queryBuilder, "fodselsdag_i_mnd")
        byggManuellFilter(filtervalg.formidlingsgruppe, queryBuilder, "formidlingsgruppekode")
        byggManuellFilter(filtervalg.servicegruppe, queryBuilder, "kvalifiseringsgruppekode")
        byggManuellFilter(filtervalg.veiledere, queryBuilder, "veileder_id")
        byggManuellFilter(filtervalg.manuellBrukerStatus, queryBuilder, "manuell_bruker")
        byggManuellFilter(filtervalg.tiltakstyper, queryBuilder, "tiltak")
        byggManuellFilter(filtervalg.rettighetsgruppe, queryBuilder, "rettighetsgruppekode")
        byggManuellFilter(filtervalg.aktiviteterForenklet, queryBuilder, "aktiviteter")
        byggManuellFilter(filtervalg.innsatsgruppeGjeldendeVedtak14a, queryBuilder, "gjeldendeVedtak14a.innsatsgruppe")
        byggManuellFilter(filtervalg.hovedmalGjeldendeVedtak14a, queryBuilder, "gjeldendeVedtak14a.hovedmal")

        if (filtervalg.harYtelseAapArenaFilter() || filtervalg.harYtelseAapKelvinFilter()) {
            val subQueryKelvin = QueryBuilders.boolQuery()
            val subQueryArena = QueryBuilders.boolQuery()
            val combinedSubQuery = QueryBuilders.boolQuery()

            filtervalg.ytelseAapArena.forEach(Consumer { ytelseArena: YtelseAapArena? ->
                when (ytelseArena) {
                    YtelseAapArena.HAR_AAP_ORDINAR -> subQueryArena.should(
                        QueryBuilders.matchQuery(
                            "ytelse",
                            YtelseMapping.AAP_MAXTID
                        )
                    )

                    YtelseAapArena.HAR_AAP_UNNTAK -> subQueryArena.should(
                        QueryBuilders.matchQuery(
                            "ytelse",
                            YtelseMapping.AAP_UNNTAK
                        )
                    )

                    else -> {
                        throw IllegalStateException("ytelseArena har ugyldig verdi")
                    }
                }
            })

            filtervalg.ytelseAapKelvin.forEach(Consumer { ytelseKelvin: YtelseAapKelvin? ->
                when (ytelseKelvin) {
                    YtelseAapKelvin.HAR_AAP -> subQueryKelvin.should(QueryBuilders.termQuery("aap_kelvin", true))
                    else -> {
                        throw IllegalStateException("ytelseKelvin har ugyldig verdi")
                    }
                }
            })

            if (filtervalg.harYtelseAapArenaFilter()) {
                combinedSubQuery.should(subQueryArena)
            }
            if (filtervalg.harYtelseAapKelvinFilter()) {
                combinedSubQuery.should(subQueryKelvin)
            }

            queryBuilder.must(combinedSubQuery)
        }


        if (filtervalg.harYtelseTiltakspengerFilter() || filtervalg.harYtelseTiltakspengerArenaFilter()) {
            val subQueryTiltakspenger = QueryBuilders.boolQuery()
            val subQueryTiltakspengerArena = QueryBuilders.boolQuery()
            val combinedSubQuery = QueryBuilders.boolQuery()

            filtervalg.ytelseTiltakspenger.forEach(Consumer { ytelseTiltakspenger: YtelseTiltakspenger? ->
                when (ytelseTiltakspenger) {
                    YtelseTiltakspenger.HAR_TILTAKSPENGER -> subQueryTiltakspenger.should(
                        QueryBuilders.termQuery(
                            "tiltakspenger",
                            true
                        )
                    )

                    else -> {
                        throw IllegalStateException("ytelseTiltakspenger har ugyldig verdi")
                    }
                }
            })

            filtervalg.ytelseTiltakspengerArena.forEach(Consumer { ytelseTiltakspenger: YtelseTiltakspengerArena? ->
                when (ytelseTiltakspenger) {
                    YtelseTiltakspengerArena.HAR_TILTAKSPENGER -> subQueryTiltakspengerArena.should(
                        QueryBuilders.matchQuery(
                            "ytelse",
                            YtelseMapping.TILTAKSPENGER
                        )
                    )

                    else -> {
                        throw IllegalStateException("ytelseTiltakspengerArena har ugyldig verdi")
                    }
                }
            })

            if (filtervalg.harYtelseTiltakspengerFilter()) {
                combinedSubQuery.should(subQueryTiltakspenger)
            }
            if (filtervalg.harYtelseTiltakspengerArenaFilter()) {
                combinedSubQuery.should(subQueryTiltakspengerArena)
            }

            queryBuilder.must(combinedSubQuery)
        }


        if (filtervalg.harYtelseDagpengerArenaFilter()) {
            val subQueryArena = QueryBuilders.boolQuery()

            filtervalg.ytelseDagpengerArena.forEach(Consumer { ytelseArena: YtelseDagpengerArena? ->
                when (ytelseArena) {
                    YtelseDagpengerArena.HAR_DAGPENGER_ORDINAER -> subQueryArena.should(
                        QueryBuilders.matchQuery(
                            "ytelse",
                            YtelseMapping.ORDINARE_DAGPENGER
                        )
                    )

                    YtelseDagpengerArena.HAR_DAGPENGER_MED_PERMITTERING -> subQueryArena.should(
                        QueryBuilders.matchQuery(
                            "ytelse",
                            YtelseMapping.DAGPENGER_MED_PERMITTERING
                        )
                    )

                    YtelseDagpengerArena.HAR_DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI -> subQueryArena.should(
                        QueryBuilders.matchQuery(
                            "ytelse",
                            YtelseMapping.DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI
                        )
                    )

                    YtelseDagpengerArena.HAR_DAGPENGER_LONNSGARANTIMIDLER -> subQueryArena.should(
                        QueryBuilders.matchQuery(
                            "ytelse",
                            YtelseMapping.LONNSGARANTIMIDLER_DAGPENGER
                        )
                    )

                    else -> {
                        throw IllegalStateException("ytelseArena har ugyldig verdi")
                    }
                }
            })

            queryBuilder.must(subQueryArena)
        }

        if (filtervalg.harKjonnfilter()) {
            queryBuilder.must(QueryBuilders.matchQuery("kjonn", filtervalg.kjonn?.name))
        }

        if (filtervalg.harCvFilter()) {
            if (filtervalg.cvJobbprofil == CVjobbprofil.HAR_DELT_CV) {
                queryBuilder.must(QueryBuilders.matchQuery("har_delt_cv", true))
                queryBuilder.must(QueryBuilders.matchQuery("cv_eksistere", true))
            } else {
                val orQuery = QueryBuilders.boolQuery()
                orQuery.should(QueryBuilders.matchQuery("har_delt_cv", false))
                orQuery.should(QueryBuilders.matchQuery("cv_eksistere", false))
                queryBuilder.must(orQuery)
            }
        }

        if (filtervalg.harStillingFraNavFilter()) {
            filtervalg.stillingFraNavFilter.forEach(
                Consumer { stillingFraNAVFilter: StillingFraNAVFilter? ->
                    when (stillingFraNAVFilter) {
                        StillingFraNAVFilter.CV_KAN_DELES_STATUS_JA -> queryBuilder.must(
                            QueryBuilders.matchQuery(
                                "neste_cv_kan_deles_status",
                                "JA"
                            )
                        )

                        else -> throw IllegalStateException("Stilling fra Nav ikke funnet")
                    }
                })
        }

        if (filtervalg.harAktiviteterAvansert()) {
            byggAvansertAktivitetFilterQuery(filtervalg, queryBuilder)
        }

        if (filtervalg.harUlesteEndringerFilter()) {
            byggUlestEndringsFilter(filtervalg.sisteEndringKategori, queryBuilder)
        }

        if (filtervalg.harSisteEndringFilter()) {
            byggSisteEndringFilter(filtervalg.sisteEndringKategori, queryBuilder)
        }

        if (filtervalg.harNavnEllerFnrQuery()) {
            val query = filtervalg.navnEllerFnrQuery.trim { it <= ' ' }.lowercase(Locale.getDefault())
            if (StringUtils.isNumeric(query)) {
                queryBuilder.must(QueryBuilders.termQuery("fnr", query))
            } else {
                queryBuilder.must(QueryBuilders.termQuery("fullt_navn", query))
            }
        }

        if (filtervalg.harFoedelandFilter()) {
            val subQuery = QueryBuilders.boolQuery()
            filtervalg.foedeland.forEach(
                Consumer { foedeLand: String? ->
                    queryBuilder.must(
                        subQuery.should(
                            QueryBuilders.matchQuery(
                                "foedeland",
                                foedeLand
                            )
                        )
                    )
                }
            )
        }
        if (filtervalg.harLandgruppeFilter()) {
            val subQuery = QueryBuilders.boolQuery()
            val subQueryUnkjent = QueryBuilders.boolQuery()
            filtervalg.landgruppe.forEach(
                Consumer { landGruppe: String ->
                    val landgruppeCode = landGruppe.replace("LANDGRUPPE_", "")
                    if (landgruppeCode.equals("UKJENT", ignoreCase = true)) {
                        subQueryUnkjent.mustNot(QueryBuilders.existsQuery("landgruppe"))
                        subQuery.should(subQueryUnkjent)
                    } else {
                        subQuery.should(QueryBuilders.matchQuery("landgruppe", landgruppeCode))
                    }
                }
            )
            queryBuilder.must(subQuery)
        }
        if (filtervalg.harFargeKategoriFilter()) {
            val subQuery = QueryBuilders.boolQuery()
            val subQueryUnkjent = QueryBuilders.boolQuery()
            filtervalg.fargekategorier.forEach(
                Consumer { fargeKategori: String ->
                    if (FargekategoriVerdi.INGEN_KATEGORI.name == fargeKategori) {
                        subQueryUnkjent.mustNot(QueryBuilders.existsQuery("fargekategori"))
                        subQuery.should(subQueryUnkjent)
                    } else {
                        subQuery.should(QueryBuilders.matchQuery("fargekategori", fargeKategori))
                    }
                }
            )
            queryBuilder.must(subQuery)
        }
        /**
         * Tolkebehov-filteret fungerer slik:
         * - Filtrert på talespråktolk -> får ut dei som berre har talespråktolk
         * - Filtrert på tegnspråktolk -> får ut dei som berre har tegnspråktolk
         * - Filtrert på begge -> får ut alle som har behov for minst ein av tolketypane
         */
        if (filtervalg.harTalespraaktolkFilter() || filtervalg.harTegnspraakFilter()) {
            val tolkebehovSubQuery = QueryBuilders.boolQuery()
            val taletolkbehovSubQuery = QueryBuilders.boolQuery()
            val tolkBehovTegnSubQuery = QueryBuilders.boolQuery()

            if (filtervalg.harTalespraaktolkFilter()) {
                taletolkbehovSubQuery.must(QueryBuilders.existsQuery("talespraaktolk"))
                    .mustNot(QueryBuilders.matchQuery("talespraaktolk", ""))
                tolkebehovSubQuery.should(taletolkbehovSubQuery)
            }
            if (filtervalg.harTegnspraakFilter()) {
                tolkBehovTegnSubQuery.must(QueryBuilders.existsQuery("tegnspraaktolk"))
                    .mustNot(QueryBuilders.matchQuery("tegnspraaktolk", ""))
                tolkebehovSubQuery.should(tolkBehovTegnSubQuery)
            }

            tolkebehovSubQuery.minimumShouldMatch(1)

            queryBuilder.must(tolkebehovSubQuery)
        }
        if (filtervalg.harTolkbehovSpraakFilter()) {
            var tolkbehovSelected = false
            val tolkBehovSubquery = QueryBuilders.boolQuery()

            if (filtervalg.harTalespraaktolkFilter()) {
                filtervalg.tolkBehovSpraak?.forEach(
                    Consumer { tolkbehovSpraak: String? ->
                        tolkBehovSubquery.should(
                            QueryBuilders.matchQuery(
                                "talespraaktolk",
                                tolkbehovSpraak
                            )
                        )
                    }
                )
                tolkbehovSelected = true
            }
            if (filtervalg.harTegnspraakFilter()) {
                filtervalg.tolkBehovSpraak?.forEach(Consumer { tolkbehovSpraak: String? ->
                    tolkBehovSubquery.should(
                        QueryBuilders.matchQuery(
                            "tegnspraaktolk",
                            tolkbehovSpraak
                        )
                    )
                }
                )
                tolkbehovSelected = true
            }

            if (!tolkbehovSelected) {
                filtervalg.tolkBehovSpraak?.forEach(
                    Consumer { tolkbehovSpraak: String? ->
                        tolkBehovSubquery.should(
                            QueryBuilders.matchQuery(
                                "talespraaktolk",
                                tolkbehovSpraak
                            )
                        )
                    }
                )
                filtervalg.tolkBehovSpraak?.forEach(Consumer { tolkbehovSpraak: String? ->
                    tolkBehovSubquery.should(
                        QueryBuilders.matchQuery(
                            "tegnspraaktolk",
                            tolkbehovSpraak
                        )
                    )
                }
                )
            }
            queryBuilder.must(tolkBehovSubquery)
        }

        if (filtervalg.harBostedFilter()) {
            val bostedSubquery = QueryBuilders.boolQuery()
            filtervalg.geografiskBosted.forEach(Consumer { geografiskBosted: String? ->
                bostedSubquery.should(QueryBuilders.matchQuery("kommunenummer", geografiskBosted))
                bostedSubquery.should(QueryBuilders.matchQuery("bydelsnummer", geografiskBosted))
            }
            )
            queryBuilder.must(bostedSubquery)
        }

        if (filtervalg.harEnsligeForsorgereFilter() && filtervalg.ensligeForsorgere.contains(EnsligeForsorgere.OVERGANGSSTONAD)
        ) {
            queryBuilder.must(QueryBuilders.existsQuery("enslige_forsorgere_overgangsstonad"))
        }

        if (filtervalg.harDinSituasjonSvar()) {
            val brukerensSituasjonSubQuery = QueryBuilders.boolQuery()
            filtervalg.registreringstype.forEach(Consumer { jobbSituasjonBeskrivelse: JobbSituasjonBeskrivelse ->
                if (jobbSituasjonBeskrivelse == JobbSituasjonBeskrivelse.INGEN_DATA) {
                    brukerensSituasjonSubQuery.should(
                        QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("brukers_situasjoner"))
                    )
                } else {
                    inkludereSituasjonerFraBadeVeilarbregistreringOgArbeidssoekerregistrering(jobbSituasjonBeskrivelse).forEach(
                        Consumer { sokerOrd: String? ->
                            brukerensSituasjonSubQuery.should(
                                QueryBuilders.matchQuery(
                                    "brukers_situasjoner",
                                    sokerOrd
                                )
                            )
                        }
                    )
                }
            })
            queryBuilder.must(brukerensSituasjonSubQuery)
        }

        if (filtervalg.harUtdanningSvar()) {
            val brukerensUtdanningSubQuery = QueryBuilders.boolQuery()
            filtervalg.utdanning.forEach(Consumer { utdanningSvar: UtdanningSvar ->
                if (utdanningSvar == UtdanningSvar.INGEN_DATA) {
                    brukerensUtdanningSubQuery.should(
                        QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("utdanning"))
                    )
                    brukerensUtdanningSubQuery.should(QueryBuilders.matchQuery("utdanning", "INGEN_SVAR"))
                } else {
                    brukerensUtdanningSubQuery.should(QueryBuilders.matchQuery("utdanning", utdanningSvar))
                }
            })
            queryBuilder.must(brukerensUtdanningSubQuery)
        }

        if (filtervalg.harUtdanningBestattSvar()) {
            val brukerensUtdanningSubQuery = QueryBuilders.boolQuery()
            filtervalg.utdanningBestatt.forEach(Consumer { utdanningSvar: UtdanningBestattSvar ->
                if (utdanningSvar == UtdanningBestattSvar.INGEN_DATA) {
                    brukerensUtdanningSubQuery.should(
                        QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("utdanning_bestatt"))
                    )
                    brukerensUtdanningSubQuery.should(QueryBuilders.matchQuery("utdanning_bestatt", "INGEN_SVAR"))
                } else {
                    brukerensUtdanningSubQuery.should(QueryBuilders.matchQuery("utdanning_bestatt", utdanningSvar))
                }
            })
            queryBuilder.must(brukerensUtdanningSubQuery)
        }

        if (filtervalg.harUtdanningGodkjentSvar()) {
            val brukerensUtdanningSubQuery = QueryBuilders.boolQuery()
            filtervalg.utdanningGodkjent.forEach(Consumer { utdanningSvar: UtdanningGodkjentSvar ->
                if (utdanningSvar == UtdanningGodkjentSvar.INGEN_DATA) {
                    brukerensUtdanningSubQuery.should(
                        QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("utdanning_godkjent"))
                    )
                    brukerensUtdanningSubQuery.should(QueryBuilders.matchQuery("utdanning_godkjent", "INGEN_SVAR"))
                } else {
                    brukerensUtdanningSubQuery.should(QueryBuilders.matchQuery("utdanning_godkjent", utdanningSvar))
                }
            })
            queryBuilder.must(brukerensUtdanningSubQuery)
        }

        if (filtervalg.harGjeldendeVedtak14aFilter()) {
            val valgteGjeldendeVedtak14aFilter = filtervalg.gjeldendeVedtak14a.stream().map { name: String? ->
                GjeldendeVedtak14aFilter.valueOf(
                    name!!
                )
            }.toList()
            val subQuery = QueryBuilders.boolQuery()

            if (valgteGjeldendeVedtak14aFilter.contains(GjeldendeVedtak14aFilter.HAR_14A_VEDTAK)
                && !valgteGjeldendeVedtak14aFilter.contains(GjeldendeVedtak14aFilter.HAR_IKKE_14A_VEDTAK)
            ) {
                subQuery.must(QueryBuilders.existsQuery("gjeldendeVedtak14a"))
                queryBuilder.must(subQuery)
            } else if (valgteGjeldendeVedtak14aFilter.contains(GjeldendeVedtak14aFilter.HAR_IKKE_14A_VEDTAK)
                && !valgteGjeldendeVedtak14aFilter.contains(GjeldendeVedtak14aFilter.HAR_14A_VEDTAK)
            ) {
                subQuery.mustNot(QueryBuilders.existsQuery("gjeldendeVedtak14a"))
                queryBuilder.must(subQuery)
            }
        }
    }

    fun byggAvansertAktivitetFilterQuery(
        filtervalg: Filtervalg,
        queryBuilder: BoolQueryBuilder
    ): List<BoolQueryBuilder> {
        return filtervalg.aktiviteter.entries.stream()
            .map { entry: Map.Entry<String, AktivitetFiltervalg> ->
                val navnPaaAktivitet = entry.key
                val valg = entry.value
                if (AktivitetFiltervalg.JA == valg) {
                    return@map queryBuilder.filter(QueryBuilders.matchQuery("aktiviteter", navnPaaAktivitet))
                } else if (AktivitetFiltervalg.NEI == valg && "TILTAK" == navnPaaAktivitet) {
                    return@map queryBuilder.filter(
                        QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("tiltak"))
                    )
                } else if (AktivitetFiltervalg.NEI == valg) {
                    return@map queryBuilder.filter(
                        QueryBuilders.boolQuery().mustNot(QueryBuilders.matchQuery("aktiviteter", navnPaaAktivitet))
                    )
                } else {
                    return@map queryBuilder
                }
            }.collect(Collectors.toList())
    }

    fun leggTilFerdigFilter(brukerStatus: Brukerstatus, veiledereMedTilgangTilEnhet: List<String?>): QueryBuilder {
        val queryBuilder = when (brukerStatus) {
            Brukerstatus.UFORDELTE_BRUKERE -> byggUfordeltBrukereQuery(veiledereMedTilgangTilEnhet)
            Brukerstatus.TRENGER_OPPFOLGINGSVEDTAK -> byggTrengerOppfolgingsvedtakFilter()
            Brukerstatus.INAKTIVE_BRUKERE -> QueryBuilders.matchQuery("formidlingsgruppekode", "ISERV")
            Brukerstatus.VENTER_PA_SVAR_FRA_NAV -> QueryBuilders.existsQuery("venterpasvarfranav")
            Brukerstatus.VENTER_PA_SVAR_FRA_BRUKER -> QueryBuilders.existsQuery("venterpasvarfrabruker")
            Brukerstatus.I_AVTALT_AKTIVITET -> QueryBuilders.existsQuery("aktiviteter")
            Brukerstatus.I_AKTIVITET -> QueryBuilders.existsQuery("alleAktiviteter")
            Brukerstatus.IKKE_I_AVTALT_AKTIVITET -> QueryBuilders.boolQuery()
                .mustNot(QueryBuilders.existsQuery("aktiviteter"))

            Brukerstatus.UTLOPTE_AKTIVITETER -> QueryBuilders.existsQuery("nyesteutlopteaktivitet")
            Brukerstatus.MINE_HUSKELAPPER -> QueryBuilders.existsQuery("huskelapp")
            Brukerstatus.NYE_BRUKERE_FOR_VEILEDER -> QueryBuilders.matchQuery("ny_for_veileder", true)
            Brukerstatus.MOTER_IDAG -> byggMoteMedNavIdag()
            Brukerstatus.ER_SYKMELDT_MED_ARBEIDSGIVER -> byggErSykmeldtMedArbeidsgiverFilter()
            Brukerstatus.UNDER_VURDERING -> QueryBuilders.existsQuery("utkast_14a_status")
            Brukerstatus.TILTAKSHENDELSER -> QueryBuilders.existsQuery("tiltakshendelse")
            Brukerstatus.UTGATTE_VARSEL -> QueryBuilders.existsQuery("hendelser.UTGATT_VARSEL")
            Brukerstatus.UDELT_SAMTALEREFERAT -> QueryBuilders.existsQuery("hendelser.UDELT_SAMTALEREFERAT")

        }
        return queryBuilder
    }

    fun byggTrengerOppfolgingsvedtakFilter(): QueryBuilder {
        return QueryBuilders.boolQuery()
            .mustNot(QueryBuilders.existsQuery("gjeldendeVedtak14a"))
    }

    fun byggErSykmeldtMedArbeidsgiverFilter(): QueryBuilder {
        return QueryBuilders.boolQuery()
            .must(QueryBuilders.matchQuery("er_sykmeldt_med_arbeidsgiver", true))
            .mustNot(QueryBuilders.existsQuery("utkast_14a_status"))
    }

    // Brukere med veileder uten tilgang til denne enheten ansees som ufordelte brukere
    fun byggUfordeltBrukereQuery(veiledereMedTilgangTilEnhet: List<String?>): BoolQueryBuilder {
        val boolQuery = QueryBuilders.boolQuery()
        veiledereMedTilgangTilEnhet.forEach(Consumer { id: String? ->
            boolQuery.mustNot(
                QueryBuilders.matchQuery(
                    "veileder_id",
                    id
                )
            )
        })
        return boolQuery
    }

    fun <T> byggManuellFilter(
        filtervalgsListe: List<T>,
        queryBuilder: BoolQueryBuilder,
        matchQueryString: String
    ): BoolQueryBuilder {
        if (filtervalgsListe.isNotEmpty()) {
            val boolQueryBuilder = BoolQueryBuilder()
            filtervalgsListe.forEach(Consumer { filtervalg: T ->
                boolQueryBuilder.should(
                    QueryBuilders.matchQuery(
                        matchQueryString,
                        filtervalg
                    )
                )
            })
            return queryBuilder.filter(boolQueryBuilder)
        }

        return queryBuilder
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
    fun byggAlderQuery(alder: String, queryBuilder: BoolQueryBuilder) {
        if ("19-og-under" == alder) {
            queryBuilder.should(
                QueryBuilders.rangeQuery("fodselsdato")
                    .lte("now")
                    .gt("now-20y-1d")
            )
        } else {
            val fraTilAlder = alder.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val fraAlder = fraTilAlder[0].toInt()
            val tilAlder = fraTilAlder[1].toInt()

            queryBuilder.should(
                QueryBuilders.rangeQuery("fodselsdato")
                    .lte(String.format("now-%sy/d", fraAlder))
                    .gt(String.format("now-%sy-1d", tilAlder + 1))
            )
        }
    }

    fun byggPortefoljestorrelserQuery(enhetId: String): SearchSourceBuilder {
        val name = "portefoljestorrelser"

        return SearchSourceBuilder()
            .size(0)
            .query(QueryBuilders.termQuery("oppfolging", true))
            .aggregation(
                AggregationBuilders.filter(
                    name,
                    QueryBuilders.termQuery("enhet_id", enhetId)
                )
                    .subAggregation(
                        AggregationBuilders
                            .terms(name)
                            .field("veileder_id")
                            .size(9999)
                            .order(BucketOrder.key(true))
                    )
            )
    }

    fun byggStatustallQuery(
        filtrereVeilederOgEnhet: BoolQueryBuilder,
        veiledereMedTilgangTilEnhet: List<String?>
    ): SearchSourceBuilder {
        val filtre = arrayOf(
            erSykemeldtMedArbeidsgiverFilter(filtrereVeilederOgEnhet),
            mustExistFilter(filtrereVeilederOgEnhet, StatustallAggregationKey.I_AVTALT_AKTIVITET.key, "aktiviteter"),
            ikkeIavtaltAktivitet(filtrereVeilederOgEnhet),
            inaktiveBrukere(filtrereVeilederOgEnhet),
            mustBeTrueFilter(
                filtrereVeilederOgEnhet,
                StatustallAggregationKey.NYE_BRUKERE_FOR_VEILEDER.key
            ),
            totalt(filtrereVeilederOgEnhet),
            mustNotExistFilter(
                filtrereVeilederOgEnhet,
                StatustallAggregationKey.TRENGER_OPPFOLGINGSVEDTAK.key,
                "gjeldendeVedtak14a"
            ),
            mustExistFilter(
                filtrereVeilederOgEnhet,
                StatustallAggregationKey.VENTER_PA_SVAR_FRA_NAV.key,
                "venterpasvarfranav"
            ),
            mustExistFilter(
                filtrereVeilederOgEnhet,
                StatustallAggregationKey.VENTER_PA_SVAR_FRA_BRUKER.key,
                "venterpasvarfrabruker"
            ),
            ufordelteBrukere(filtrereVeilederOgEnhet, veiledereMedTilgangTilEnhet),
            mustExistFilter(
                filtrereVeilederOgEnhet,
                StatustallAggregationKey.UTLOPTE_AKTIVITETER.key,
                "nyesteutlopteaktivitet"
            ),
            moterMedNavIdag(filtrereVeilederOgEnhet),
            mustExistFilter(filtrereVeilederOgEnhet, StatustallAggregationKey.UNDER_VURDERING.key, "utkast_14a_status"),
            mustMatchQuery(
                filtrereVeilederOgEnhet,
                StatustallAggregationKey.FARGEKATEGORI_A.key,
                FargekategoriVerdi.FARGEKATEGORI_A.name
            ),
            mustMatchQuery(
                filtrereVeilederOgEnhet,
                StatustallAggregationKey.FARGEKATEGORI_B.key,
                FargekategoriVerdi.FARGEKATEGORI_B.name
            ),
            mustMatchQuery(
                filtrereVeilederOgEnhet,
                StatustallAggregationKey.FARGEKATEGORI_C.key,
                FargekategoriVerdi.FARGEKATEGORI_C.name
            ),
            mustMatchQuery(
                filtrereVeilederOgEnhet,
                StatustallAggregationKey.FARGEKATEGORI_D.key,
                FargekategoriVerdi.FARGEKATEGORI_D.name
            ),
            mustMatchQuery(
                filtrereVeilederOgEnhet,
                StatustallAggregationKey.FARGEKATEGORI_E.key,
                FargekategoriVerdi.FARGEKATEGORI_E.name
            ),
            mustMatchQuery(
                filtrereVeilederOgEnhet,
                StatustallAggregationKey.FARGEKATEGORI_F.key,
                FargekategoriVerdi.FARGEKATEGORI_F.name
            ),
            mustNotExistFilter(
                filtrereVeilederOgEnhet,
                StatustallAggregationKey.FARGEKATEGORI_INGEN_KATEGORI.key,
                "fargekategori"
            ),
            mustExistFilter(filtrereVeilederOgEnhet, StatustallAggregationKey.MINE_HUSKELAPPER.key, "huskelapp"),
            mustExistFilter(filtrereVeilederOgEnhet, StatustallAggregationKey.TILTAKSHENDELSER.key, "tiltakshendelse"),
            mustExistFilter(
                filtrereVeilederOgEnhet,
                StatustallAggregationKey.UTGATTE_VARSEL.key,
                "hendelser.UTGATT_VARSEL"
            ),
            mustExistFilter(
                filtrereVeilederOgEnhet,
                StatustallAggregationKey.UDELTE_SAMTALEREFERAT.key,
                "hendelser.UDELT_SAMTALEREFERAT"
            )
        )

        return SearchSourceBuilder()
            .size(0)
            .aggregation(AggregationBuilders.filters("statustall", *filtre))
    }

    private fun byggUlestEndringsFilter(sisteEndringKategori: String?, queryBuilder: BoolQueryBuilder) {
        val relevanteKategorier: List<String> =
            if (sisteEndringKategori.isNullOrBlank()) {
                SisteEndringsKategori.entries.map { it.name }
            } else {
                listOf(sisteEndringKategori)
            }

        val orQuery = QueryBuilders.boolQuery()

        relevanteKategorier.forEach { kategori ->
            orQuery.should(
                QueryBuilders.matchQuery("siste_endringer.$kategori.erSett", "N")
            )
        }

        queryBuilder.must(orQuery)
    }

    private fun byggSisteEndringFilter(sisteEndringKategori: String?, queryBuilder: BoolQueryBuilder) {
        queryBuilder.must(QueryBuilders.existsQuery("siste_endringer." + sisteEndringKategori))
    }

    private fun byggMoteMedNavIdag(): RangeQueryBuilder {
        val localDate = LocalDate.now()
        return QueryBuilders.rangeQuery("alle_aktiviteter_mote_startdato")
            .gte(DateUtils.toIsoUTC(localDate.atStartOfDay()))
            .lt(DateUtils.toIsoUTC(localDate.plusDays(1).atStartOfDay()))
    }

    private fun erSykemeldtMedArbeidsgiverFilter(filtrereVeilederOgEnhet: BoolQueryBuilder): FiltersAggregator.KeyedFilter {
        val boolQueryBuilder = QueryBuilders.boolQuery()
            .must(filtrereVeilederOgEnhet)
            .must(QueryBuilders.termQuery("er_sykmeldt_med_arbeidsgiver", true))
            .mustNot(QueryBuilders.existsQuery("utkast_14a_status"))

        return FiltersAggregator.KeyedFilter("erSykmeldtMedArbeidsgiver", boolQueryBuilder)
    }

    private fun moterMedNavIdag(filtrereVeilederOgEnhet: BoolQueryBuilder): FiltersAggregator.KeyedFilter {
        return FiltersAggregator.KeyedFilter(
            StatustallAggregationKey.MOTER_MED_NAV_I_DAG.key,
            QueryBuilders.boolQuery()
                .must(filtrereVeilederOgEnhet)
                .must(byggMoteMedNavIdag())
        )
    }

    private fun ufordelteBrukere(
        filtrereVeilederOgEnhet: BoolQueryBuilder,
        veiledereMedTilgangTilEnhet: List<String?>
    ): FiltersAggregator.KeyedFilter {
        return FiltersAggregator.KeyedFilter(
            StatustallAggregationKey.UFORDELTE_BRUKERE.key,
            QueryBuilders.boolQuery()
                .must(filtrereVeilederOgEnhet)
                .must(byggUfordeltBrukereQuery(veiledereMedTilgangTilEnhet))
        )
    }

    private fun totalt(filtrereVeilederOgEnhet: BoolQueryBuilder): FiltersAggregator.KeyedFilter {
        return FiltersAggregator.KeyedFilter(
            StatustallAggregationKey.TOTALT.key,
            QueryBuilders.boolQuery()
                .must(filtrereVeilederOgEnhet)
        )
    }

    private fun mustBeTrueFilter(
        filtrereVeilederOgEnhet: BoolQueryBuilder,
        key: String
    ): FiltersAggregator.KeyedFilter {
        return FiltersAggregator.KeyedFilter(
            key,
            QueryBuilders.boolQuery()
                .must(filtrereVeilederOgEnhet)
                .must(QueryBuilders.termQuery("ny_for_veileder", true))

        )
    }

    private fun inaktiveBrukere(filtrereVeilederOgEnhet: BoolQueryBuilder): FiltersAggregator.KeyedFilter {
        return FiltersAggregator.KeyedFilter(
            StatustallAggregationKey.INAKTIVE_BRUKERE.key,
            QueryBuilders.boolQuery()
                .must(filtrereVeilederOgEnhet)
                .must(QueryBuilders.matchQuery("formidlingsgruppekode", "ISERV"))

        )
    }

    private fun mustMatchQuery(
        filtrereVeilederOgEnhet: BoolQueryBuilder,
        key: String,
        value: String
    ): FiltersAggregator.KeyedFilter {
        return FiltersAggregator.KeyedFilter(
            key,
            QueryBuilders.boolQuery()
                .must(filtrereVeilederOgEnhet)
                .must(QueryBuilders.matchQuery("fargekategori", value))
        )
    }

    private fun ikkeIavtaltAktivitet(filtrereVeilederOgEnhet: BoolQueryBuilder): FiltersAggregator.KeyedFilter {
        return FiltersAggregator.KeyedFilter(
            StatustallAggregationKey.IKKE_I_AVTALT_AKTIVITET.key,
            QueryBuilders.boolQuery()
                .must(filtrereVeilederOgEnhet)
                .mustNot(QueryBuilders.existsQuery("aktiviteter"))

        )
    }

    private fun mustExistFilter(
        filtrereVeilederOgEnhet: BoolQueryBuilder,
        key: String,
        term: String
    ): FiltersAggregator.KeyedFilter {
        return FiltersAggregator.KeyedFilter(
            key,
            QueryBuilders.boolQuery()
                .must(filtrereVeilederOgEnhet)
                .must(QueryBuilders.existsQuery(term))
        )
    }

    private fun mustNotExistFilter(
        filtrereVeilederOgEnhet: BoolQueryBuilder,
        key: String,
        term: String
    ): FiltersAggregator.KeyedFilter {
        return FiltersAggregator.KeyedFilter(
            key,
            QueryBuilders.boolQuery()
                .must(filtrereVeilederOgEnhet)
                .mustNot(QueryBuilders.existsQuery(term))
        )
    }

}
