package no.nav.pto.veilarbportefolje.opensearch

import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.JobbSituasjonBeskrivelse
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.inkludereSituasjonerFraBadeVeilarbregistreringOgArbeidssoekerregistrering
import no.nav.pto.veilarbportefolje.auth.BrukerinnsynTilganger
import no.nav.pto.veilarbportefolje.dagpenger.domene.DagpengerRettighetstype
import no.nav.pto.veilarbportefolje.domene.YtelseMapping
import no.nav.pto.veilarbportefolje.domene.filtervalg.*
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriVerdi
import no.nav.pto.veilarbportefolje.hendelsesfilter.Kategori
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Aktiviteter.AKTIVITETER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Aktiviteter.ALLE_AKTIVITETER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Aktiviteter.ALLE_AKTIVITETER_MOTE_STARTDATO
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Aktiviteter.NYESTE_UTLOPTE_AKTIVITET
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Aktiviteter.SISTE_ENDRINGER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Aktiviteter.SISTE_ENDRINGER_ER_SETT
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Aktiviteter.TILTAK
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Annet.FARGEKATEGORI
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Annet.FORMIDLINGSGRUPPE_KODE
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Annet.HENDELSER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Annet.HUSKELAPP
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Annet.TILTAKSHENDELSE
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Arbeidsforhold.ER_SYKMELDT_MED_ARBEIDSGIVER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Arbeidssoeker.BRUKERS_SITUASJONER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Arbeidssoeker.UTDANNING
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Arbeidssoeker.UTDANNING_BESTATT
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Arbeidssoeker.UTDANNING_GODKJENT
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.CV.CV_EKSISTERE
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.CV.NESTE_CV_KAN_DELES_STATUS
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Dialog.VENTER_PA_SVAR_FRA_BRUKER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Dialog.VENTER_PA_SVAR_FRA_NAV
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.NavAnsatt.EGEN_ANSATT
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Oppfolging.ENHET_ID
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Oppfolging.GJELDENDE_VEDTAK_14A
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Oppfolging.GJELDENDE_VEDTAK_14A_HOVEDMAL
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Oppfolging.GJELDENDE_VEDTAK_14A_INNSATSGRUPPE
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Oppfolging.KVALIFISERINGSGRUPPE_KODE
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Oppfolging.MANUELL_BRUKER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Oppfolging.NY_FOR_VEILEDER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Oppfolging.OPPFOLGING
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Oppfolging.UTKAST_14A_STATUS
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Oppfolging.VEILEDER_ID
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.BARN_UNDER_18_AAR
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.BARN_UNDER_18_AAR_ALDER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.BARN_UNDER_18_AAR_DISKRESJONSKODE
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.BYDELSNUMMER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.DISKRESJONSKODE
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.FNR
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.FODSELSDAG_I_MND
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.FODSELSDATO
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.FOEDELAND
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.FULLT_NAVN
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.KJONN
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.KOMMUNENUMMER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.LANDGRUPPE
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.TALESPRAAK_TOLK
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.TEGNSPRAAK_TOLK
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.AAP_KELVIN
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.DAGPENGER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.DAGPENGER_HAR_DAGPENGER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.DAGPENGER_RETTIGHETSTYPE
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.ENSLIGE_FORSORGERE_OVERGANGSSTONAD
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.RETTIGHETSGRUPPE_KODE
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.TILTAKSPENGER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.YTELSE
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
                            DISKRESJONSKODE,
                            Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode
                        )
                    )
                }

                if (!brukerInnsynTilganger.tilgangTilAdressebeskyttelseFortrolig) {
                    boolQuery.mustNot(
                        QueryBuilders.matchQuery(
                            DISKRESJONSKODE,
                            Adressebeskyttelse.FORTROLIG.diskresjonskode
                        )
                    )
                }

                if (!brukerInnsynTilganger.tilgangTilSkjerming) {
                    boolQuery.mustNot(QueryBuilders.matchQuery(EGEN_ANSATT, true))
                }

                boolQuery
            }

            BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ -> {
                val shouldBoolQuery = QueryBuilders.boolQuery()

                if (!brukerInnsynTilganger.tilgangTilAdressebeskyttelseStrengtFortrolig) {
                    shouldBoolQuery.should(
                        QueryBuilders.matchQuery(
                            DISKRESJONSKODE,
                            Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode
                        )
                    )
                }

                if (!brukerInnsynTilganger.tilgangTilAdressebeskyttelseFortrolig) {
                    shouldBoolQuery.should(
                        QueryBuilders.matchQuery(
                            DISKRESJONSKODE,
                            Adressebeskyttelse.FORTROLIG.diskresjonskode
                        )
                    )
                }

                if (!brukerInnsynTilganger.tilgangTilSkjerming) {
                    shouldBoolQuery.should(QueryBuilders.matchQuery(EGEN_ANSATT, true))
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
                .must(QueryBuilders.existsQuery(BARN_UNDER_18_AAR))
                .must(
                    QueryBuilders.rangeQuery("$BARN_UNDER_18_AAR.$BARN_UNDER_18_AAR_ALDER").gte(fraAlder).lte(tilAlder)
                )
        } else if (tilgangTilKun6) {
            barnUnder18aarQueryBuilder
                .must(
                    QueryBuilders.boolQuery()
                        .should(QueryBuilders.matchQuery("$BARN_UNDER_18_AAR.$BARN_UNDER_18_AAR_DISKRESJONSKODE", "-1"))
                        .should(QueryBuilders.matchQuery("$BARN_UNDER_18_AAR.$BARN_UNDER_18_AAR_DISKRESJONSKODE", "6"))
                        .should(QueryBuilders.matchQuery("$BARN_UNDER_18_AAR.$BARN_UNDER_18_AAR_DISKRESJONSKODE", "19"))
                )
                .must(
                    QueryBuilders.rangeQuery("$BARN_UNDER_18_AAR.$BARN_UNDER_18_AAR_ALDER").gte(fraAlder).lte(tilAlder)
                )
        } else if (tilgangTil7) {
            barnUnder18aarQueryBuilder
                .must(
                    QueryBuilders.boolQuery()
                        .should(QueryBuilders.matchQuery("$BARN_UNDER_18_AAR.$BARN_UNDER_18_AAR_DISKRESJONSKODE", "-1"))
                        .should(QueryBuilders.matchQuery("$BARN_UNDER_18_AAR.$BARN_UNDER_18_AAR_DISKRESJONSKODE", "7"))
                )
                .must(
                    QueryBuilders.rangeQuery("$BARN_UNDER_18_AAR.$BARN_UNDER_18_AAR_ALDER").gte(fraAlder).lte(tilAlder)
                )
        } else {
            barnUnder18aarQueryBuilder
                .must(QueryBuilders.matchQuery("$BARN_UNDER_18_AAR.$BARN_UNDER_18_AAR_DISKRESJONSKODE", "-1"))
                .must(
                    QueryBuilders.rangeQuery("$BARN_UNDER_18_AAR.$BARN_UNDER_18_AAR_ALDER").gte(fraAlder).lte(tilAlder)
                )
        }
        val barnUnder18AarNested =
            QueryBuilders.nestedQuery(BARN_UNDER_18_AAR, barnUnder18aarQueryBuilder, ScoreMode.Avg)
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
            barnUnder18aarQueryBuilder.must(QueryBuilders.existsQuery(BARN_UNDER_18_AAR))
        } else if (tilgangTilKun6) {
            barnUnder18aarQueryBuilder.must(
                QueryBuilders.boolQuery()
                    .should(QueryBuilders.matchQuery("$BARN_UNDER_18_AAR.$BARN_UNDER_18_AAR_DISKRESJONSKODE", "-1"))
                    .should(QueryBuilders.matchQuery("$BARN_UNDER_18_AAR.$BARN_UNDER_18_AAR_DISKRESJONSKODE", "6"))
                    .should(QueryBuilders.matchQuery("$BARN_UNDER_18_AAR.$BARN_UNDER_18_AAR_DISKRESJONSKODE", "19"))
            )
        } else if (tilgangTil7) {
            barnUnder18aarQueryBuilder.must(
                QueryBuilders.boolQuery()
                    .should(QueryBuilders.matchQuery("$BARN_UNDER_18_AAR.$BARN_UNDER_18_AAR_DISKRESJONSKODE", "-1"))
                    .should(QueryBuilders.matchQuery("$BARN_UNDER_18_AAR.$BARN_UNDER_18_AAR_DISKRESJONSKODE", "7"))
            )
        } else {
            barnUnder18aarQueryBuilder.must(
                QueryBuilders.matchQuery(
                    "$BARN_UNDER_18_AAR.$BARN_UNDER_18_AAR_DISKRESJONSKODE",
                    "-1"
                )
            )
        }
        val barnUnder18Aar = QueryBuilders.nestedQuery(BARN_UNDER_18_AAR, barnUnder18aarQueryBuilder, ScoreMode.Avg)
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

        byggManuellFilter(fodseldagIMndQuery, queryBuilder, FODSELSDAG_I_MND)
        byggManuellFilter(filtervalg.formidlingsgruppe, queryBuilder, FORMIDLINGSGRUPPE_KODE)
        byggManuellFilter(filtervalg.servicegruppe, queryBuilder, KVALIFISERINGSGRUPPE_KODE)
        byggManuellFilter(filtervalg.veiledere, queryBuilder, VEILEDER_ID)
        byggManuellFilter(filtervalg.manuellBrukerStatus, queryBuilder, MANUELL_BRUKER)
        byggManuellFilter(filtervalg.tiltakstyper, queryBuilder, TILTAK)
        byggManuellFilter(filtervalg.rettighetsgruppe, queryBuilder, RETTIGHETSGRUPPE_KODE)
        byggManuellFilter(filtervalg.aktiviteterForenklet, queryBuilder, AKTIVITETER)
        byggManuellFilter(
            filtervalg.innsatsgruppeGjeldendeVedtak14a,
            queryBuilder,
            "$GJELDENDE_VEDTAK_14A.$GJELDENDE_VEDTAK_14A_INNSATSGRUPPE"
        )
        byggManuellFilter(
            filtervalg.hovedmalGjeldendeVedtak14a,
            queryBuilder,
            "$GJELDENDE_VEDTAK_14A.$GJELDENDE_VEDTAK_14A_HOVEDMAL"
        )

        if (filtervalg.harYtelseAapArenaFilter() || filtervalg.harYtelseAapKelvinFilter()) {
            val subQueryKelvin = QueryBuilders.boolQuery()
            val subQueryArena = QueryBuilders.boolQuery()
            val combinedSubQuery = QueryBuilders.boolQuery()

            filtervalg.ytelseAapArena.forEach(Consumer { ytelseArena: YtelseAapArena? ->
                when (ytelseArena) {
                    YtelseAapArena.HAR_AAP_ORDINAR -> subQueryArena.should(
                        QueryBuilders.matchQuery(
                            YTELSE,
                            YtelseMapping.AAP_MAXTID
                        )
                    )

                    YtelseAapArena.HAR_AAP_UNNTAK -> subQueryArena.should(
                        QueryBuilders.matchQuery(
                            YTELSE,
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
                    YtelseAapKelvin.HAR_AAP -> subQueryKelvin.should(QueryBuilders.termQuery(AAP_KELVIN, true))
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
                            TILTAKSPENGER,
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
                            YTELSE,
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


        if (filtervalg.harYtelseDagpengerFilter() || filtervalg.harYtelseDagpengerArenaFilter()) {
            val subQueryDagpengerArena = QueryBuilders.boolQuery()
            val subQueryDagpenger = QueryBuilders.boolQuery().must(
                QueryBuilders.termQuery(
                    "$DAGPENGER.$DAGPENGER_HAR_DAGPENGER",
                    true
                )
            )

            val combinedSubQuery = QueryBuilders.boolQuery()

            filtervalg.ytelseDagpenger?.forEach(Consumer { ytelseDagpenger: YtelseDagpenger? ->
                when (ytelseDagpenger) {
                    YtelseDagpenger.HAR_DAGPENGER_ORDINAER -> {
                        subQueryDagpenger.should(
                            QueryBuilders.matchQuery(
                                "$DAGPENGER.$DAGPENGER_RETTIGHETSTYPE",
                                DagpengerRettighetstype.DAGPENGER_ARBEIDSSOKER_ORDINAER
                            )
                        )
                    }

                    YtelseDagpenger.HAR_DAGPENGER_MED_PERMITTERING -> {
                        subQueryDagpenger.should(
                            QueryBuilders.matchQuery(
                                "$DAGPENGER.$DAGPENGER_RETTIGHETSTYPE",
                                DagpengerRettighetstype.DAGPENGER_PERMITTERING_ORDINAER
                            )
                        )

                    }

                    YtelseDagpenger.HAR_DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI -> {
                        subQueryDagpenger.should(
                            QueryBuilders.matchQuery(
                                "$DAGPENGER.$DAGPENGER_RETTIGHETSTYPE",
                                DagpengerRettighetstype.DAGPENGER_PERMITTERING_FISKEINDUSTRI.toString()
                            )
                        )
                    }

                    else -> {
                        throw IllegalStateException("ytelseDagspenger har ugyldig verdi")
                    }
                }
            })

            filtervalg.ytelseDagpengerArena.forEach(Consumer { ytelseArena: YtelseDagpengerArena? ->
                when (ytelseArena) {
                    YtelseDagpengerArena.HAR_DAGPENGER_ORDINAER -> subQueryDagpengerArena.should(
                        QueryBuilders.matchQuery(
                            YTELSE,
                            YtelseMapping.ORDINARE_DAGPENGER
                        )
                    )

                    YtelseDagpengerArena.HAR_DAGPENGER_MED_PERMITTERING -> subQueryDagpengerArena.should(
                        QueryBuilders.matchQuery(
                            YTELSE,
                            YtelseMapping.DAGPENGER_MED_PERMITTERING
                        )
                    )

                    YtelseDagpengerArena.HAR_DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI -> subQueryDagpengerArena.should(
                        QueryBuilders.matchQuery(
                            YTELSE,
                            YtelseMapping.DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI
                        )
                    )

                    YtelseDagpengerArena.HAR_DAGPENGER_LONNSGARANTIMIDLER -> subQueryDagpengerArena.should(
                        QueryBuilders.matchQuery(
                            YTELSE,
                            YtelseMapping.LONNSGARANTIMIDLER_DAGPENGER
                        )
                    )

                    else -> {
                        throw IllegalStateException("ytelseArena har ugyldig verdi")
                    }
                }
            })

            if (filtervalg.harYtelseDagpengerFilter()) {
                subQueryDagpenger.minimumShouldMatch(1)
                combinedSubQuery.should(subQueryDagpenger)
            }
            if (filtervalg.harYtelseDagpengerArenaFilter()) {
                combinedSubQuery.should(subQueryDagpengerArena)
            }

            queryBuilder.must(combinedSubQuery)
        }

        if (filtervalg.harKjonnfilter()) {
            queryBuilder.must(QueryBuilders.matchQuery(KJONN, filtervalg.kjonn?.name))
        }

        if (filtervalg.harCvFilter()) {
            if (filtervalg.cvJobbprofil == CVjobbprofil.HAR_REGISTRERT_CV) {
                queryBuilder.must(QueryBuilders.matchQuery(CV_EKSISTERE, true))
            } else {
                val orQuery = QueryBuilders.boolQuery()
                orQuery.should(QueryBuilders.matchQuery(CV_EKSISTERE, false))
                queryBuilder.must(orQuery)
            }
        }

        if (filtervalg.harStillingFraNavFilter()) {
            filtervalg.stillingFraNavFilter.forEach(
                Consumer { stillingFraNAVFilter: StillingFraNAVFilter? ->
                    when (stillingFraNAVFilter) {
                        StillingFraNAVFilter.CV_KAN_DELES_STATUS_JA -> queryBuilder.must(
                            QueryBuilders.matchQuery(
                                NESTE_CV_KAN_DELES_STATUS,
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
                queryBuilder.must(QueryBuilders.termQuery(FNR, query))
            } else {
                queryBuilder.must(QueryBuilders.termQuery(FULLT_NAVN, query))
            }
        }

        if (filtervalg.harFoedelandFilter()) {
            val subQuery = QueryBuilders.boolQuery()
            filtervalg.foedeland.forEach(
                Consumer { foedeLand: String? ->
                    queryBuilder.must(
                        subQuery.should(
                            QueryBuilders.matchQuery(
                                FOEDELAND,
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
                        subQueryUnkjent.mustNot(QueryBuilders.existsQuery(LANDGRUPPE))
                        subQuery.should(subQueryUnkjent)
                    } else {
                        subQuery.should(QueryBuilders.matchQuery(LANDGRUPPE, landgruppeCode))
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
                        subQueryUnkjent.mustNot(QueryBuilders.existsQuery(FARGEKATEGORI))
                        subQuery.should(subQueryUnkjent)
                    } else {
                        subQuery.should(QueryBuilders.matchQuery(FARGEKATEGORI, fargeKategori))
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
                taletolkbehovSubQuery.must(QueryBuilders.existsQuery(TALESPRAAK_TOLK))
                    .mustNot(QueryBuilders.matchQuery(TALESPRAAK_TOLK, ""))
                tolkebehovSubQuery.should(taletolkbehovSubQuery)
            }
            if (filtervalg.harTegnspraakFilter()) {
                tolkBehovTegnSubQuery.must(QueryBuilders.existsQuery(TEGNSPRAAK_TOLK))
                    .mustNot(QueryBuilders.matchQuery(TEGNSPRAAK_TOLK, ""))
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
                                TALESPRAAK_TOLK,
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
                            TEGNSPRAAK_TOLK,
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
                                TALESPRAAK_TOLK,
                                tolkbehovSpraak
                            )
                        )
                    }
                )
                filtervalg.tolkBehovSpraak?.forEach(Consumer { tolkbehovSpraak: String? ->
                    tolkBehovSubquery.should(
                        QueryBuilders.matchQuery(
                            TEGNSPRAAK_TOLK,
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
                bostedSubquery.should(QueryBuilders.matchQuery(KOMMUNENUMMER, geografiskBosted))
                bostedSubquery.should(QueryBuilders.matchQuery(BYDELSNUMMER, geografiskBosted))
            }
            )
            queryBuilder.must(bostedSubquery)
        }

        if (filtervalg.harEnsligeForsorgereFilter() && filtervalg.ensligeForsorgere.contains(EnsligeForsorgere.OVERGANGSSTONAD)
        ) {
            queryBuilder.must(QueryBuilders.existsQuery(ENSLIGE_FORSORGERE_OVERGANGSSTONAD))
        }

        if (filtervalg.harDinSituasjonSvar()) {
            val brukerensSituasjonSubQuery = QueryBuilders.boolQuery()
            filtervalg.registreringstype.forEach(Consumer { jobbSituasjonBeskrivelse: JobbSituasjonBeskrivelse ->
                if (jobbSituasjonBeskrivelse == JobbSituasjonBeskrivelse.INGEN_DATA) {
                    brukerensSituasjonSubQuery.should(
                        QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(BRUKERS_SITUASJONER))
                    )
                } else {
                    inkludereSituasjonerFraBadeVeilarbregistreringOgArbeidssoekerregistrering(jobbSituasjonBeskrivelse).forEach(
                        Consumer { sokerOrd: String? ->
                            brukerensSituasjonSubQuery.should(
                                QueryBuilders.matchQuery(
                                    BRUKERS_SITUASJONER,
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
                        QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(UTDANNING))
                    )
                    brukerensUtdanningSubQuery.should(QueryBuilders.matchQuery(UTDANNING, "INGEN_SVAR"))
                } else {
                    brukerensUtdanningSubQuery.should(QueryBuilders.matchQuery(UTDANNING, utdanningSvar))
                }
            })
            queryBuilder.must(brukerensUtdanningSubQuery)
        }

        if (filtervalg.harUtdanningBestattSvar()) {
            val brukerensUtdanningSubQuery = QueryBuilders.boolQuery()
            filtervalg.utdanningBestatt.forEach(Consumer { utdanningSvar: UtdanningBestattSvar ->
                if (utdanningSvar == UtdanningBestattSvar.INGEN_DATA) {
                    brukerensUtdanningSubQuery.should(
                        QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(UTDANNING_BESTATT))
                    )
                    brukerensUtdanningSubQuery.should(QueryBuilders.matchQuery(UTDANNING_BESTATT, "INGEN_SVAR"))
                } else {
                    brukerensUtdanningSubQuery.should(QueryBuilders.matchQuery(UTDANNING_BESTATT, utdanningSvar))
                }
            })
            queryBuilder.must(brukerensUtdanningSubQuery)
        }

        if (filtervalg.harUtdanningGodkjentSvar()) {
            val brukerensUtdanningSubQuery = QueryBuilders.boolQuery()
            filtervalg.utdanningGodkjent.forEach(Consumer { utdanningSvar: UtdanningGodkjentSvar ->
                if (utdanningSvar == UtdanningGodkjentSvar.INGEN_DATA) {
                    brukerensUtdanningSubQuery.should(
                        QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(UTDANNING_GODKJENT))
                    )
                    brukerensUtdanningSubQuery.should(QueryBuilders.matchQuery(UTDANNING_GODKJENT, "INGEN_SVAR"))
                } else {
                    brukerensUtdanningSubQuery.should(QueryBuilders.matchQuery(UTDANNING_GODKJENT, utdanningSvar))
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
                subQuery.must(QueryBuilders.existsQuery(GJELDENDE_VEDTAK_14A))
                queryBuilder.must(subQuery)
            } else if (valgteGjeldendeVedtak14aFilter.contains(GjeldendeVedtak14aFilter.HAR_IKKE_14A_VEDTAK)
                && !valgteGjeldendeVedtak14aFilter.contains(GjeldendeVedtak14aFilter.HAR_14A_VEDTAK)
            ) {
                subQuery.mustNot(QueryBuilders.existsQuery(GJELDENDE_VEDTAK_14A))
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
                    return@map queryBuilder.filter(QueryBuilders.matchQuery(AKTIVITETER, navnPaaAktivitet))
                } else if (AktivitetFiltervalg.NEI == valg && "TILTAK" == navnPaaAktivitet) {
                    return@map queryBuilder.filter(
                        QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(TILTAK))
                    )
                } else if (AktivitetFiltervalg.NEI == valg) {
                    return@map queryBuilder.filter(
                        QueryBuilders.boolQuery().mustNot(QueryBuilders.matchQuery(AKTIVITETER, navnPaaAktivitet))
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
            Brukerstatus.INAKTIVE_BRUKERE -> QueryBuilders.matchQuery(FORMIDLINGSGRUPPE_KODE, "ISERV")
            Brukerstatus.VENTER_PA_SVAR_FRA_NAV -> QueryBuilders.existsQuery(VENTER_PA_SVAR_FRA_NAV)
            Brukerstatus.VENTER_PA_SVAR_FRA_BRUKER -> QueryBuilders.existsQuery(VENTER_PA_SVAR_FRA_BRUKER)
            Brukerstatus.I_AVTALT_AKTIVITET -> QueryBuilders.existsQuery(AKTIVITETER)
            Brukerstatus.I_AKTIVITET -> QueryBuilders.existsQuery(ALLE_AKTIVITETER)
            Brukerstatus.IKKE_I_AVTALT_AKTIVITET -> QueryBuilders.boolQuery()
                .mustNot(QueryBuilders.existsQuery(AKTIVITETER))

            Brukerstatus.UTLOPTE_AKTIVITETER -> QueryBuilders.existsQuery(NYESTE_UTLOPTE_AKTIVITET)
            Brukerstatus.MINE_HUSKELAPPER -> QueryBuilders.existsQuery(HUSKELAPP)
            Brukerstatus.NYE_BRUKERE_FOR_VEILEDER -> QueryBuilders.matchQuery(NY_FOR_VEILEDER, true)
            Brukerstatus.MOTER_IDAG -> byggMoteMedNavIdag()
            Brukerstatus.ER_SYKMELDT_MED_ARBEIDSGIVER -> byggErSykmeldtMedArbeidsgiverFilter()
            Brukerstatus.UNDER_VURDERING -> QueryBuilders.existsQuery(UTKAST_14A_STATUS)
            Brukerstatus.TILTAKSHENDELSER -> QueryBuilders.existsQuery(TILTAKSHENDELSE)
            Brukerstatus.UTGATTE_VARSEL -> QueryBuilders.existsQuery("$HENDELSER.${Kategori.UTGATT_VARSEL.name}")
            Brukerstatus.UDELT_SAMTALEREFERAT -> QueryBuilders.existsQuery("$HENDELSER.${Kategori.UDELT_SAMTALEREFERAT.name}")

        }
        return queryBuilder
    }

    fun byggTrengerOppfolgingsvedtakFilter(): QueryBuilder {
        return QueryBuilders.boolQuery()
            .mustNot(QueryBuilders.existsQuery(GJELDENDE_VEDTAK_14A))
    }

    fun byggErSykmeldtMedArbeidsgiverFilter(): QueryBuilder {
        return QueryBuilders.boolQuery()
            .must(QueryBuilders.matchQuery(ER_SYKMELDT_MED_ARBEIDSGIVER, true))
            .mustNot(QueryBuilders.existsQuery(UTKAST_14A_STATUS))
    }

    // Brukere med veileder uten tilgang til denne enheten ansees som ufordelte brukere
    fun byggUfordeltBrukereQuery(veiledereMedTilgangTilEnhet: List<String?>): BoolQueryBuilder {
        val boolQuery = QueryBuilders.boolQuery()
        veiledereMedTilgangTilEnhet.forEach(Consumer { id: String? ->
            boolQuery.mustNot(
                QueryBuilders.matchQuery(
                    VEILEDER_ID,
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
                QueryBuilders.rangeQuery(FODSELSDATO)
                    .lte("now")
                    .gt("now-20y-1d")
            )
        } else {
            val fraTilAlder = alder.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val fraAlder = fraTilAlder[0].toInt()
            val tilAlder = fraTilAlder[1].toInt()

            queryBuilder.should(
                QueryBuilders.rangeQuery(FODSELSDATO)
                    .lte(String.format("now-%sy/d", fraAlder))
                    .gt(String.format("now-%sy-1d", tilAlder + 1))
            )
        }
    }

    fun byggPortefoljestorrelserQuery(enhetId: String): SearchSourceBuilder {
        val name = "portefoljestorrelser"

        return SearchSourceBuilder()
            .size(0)
            .query(QueryBuilders.termQuery(OPPFOLGING, true))
            .aggregation(
                AggregationBuilders.filter(
                    name,
                    QueryBuilders.termQuery(ENHET_ID, enhetId)
                )
                    .subAggregation(
                        AggregationBuilders
                            .terms(name)
                            .field(VEILEDER_ID)
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
            mustExistFilter(filtrereVeilederOgEnhet, StatustallAggregationKey.I_AVTALT_AKTIVITET.key, AKTIVITETER),
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
                GJELDENDE_VEDTAK_14A
            ),
            mustExistFilter(
                filtrereVeilederOgEnhet,
                StatustallAggregationKey.VENTER_PA_SVAR_FRA_NAV.key,
                VENTER_PA_SVAR_FRA_NAV
            ),
            mustExistFilter(
                filtrereVeilederOgEnhet,
                StatustallAggregationKey.VENTER_PA_SVAR_FRA_BRUKER.key,
                VENTER_PA_SVAR_FRA_BRUKER
            ),
            ufordelteBrukere(filtrereVeilederOgEnhet, veiledereMedTilgangTilEnhet),
            mustExistFilter(
                filtrereVeilederOgEnhet,
                StatustallAggregationKey.UTLOPTE_AKTIVITETER.key,
                NYESTE_UTLOPTE_AKTIVITET
            ),
            moterMedNavIdag(filtrereVeilederOgEnhet),
            mustExistFilter(filtrereVeilederOgEnhet, StatustallAggregationKey.UNDER_VURDERING.key, UTKAST_14A_STATUS),
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
                FARGEKATEGORI
            ),
            mustExistFilter(filtrereVeilederOgEnhet, StatustallAggregationKey.MINE_HUSKELAPPER.key, HUSKELAPP),
            mustExistFilter(filtrereVeilederOgEnhet, StatustallAggregationKey.TILTAKSHENDELSER.key, TILTAKSHENDELSE),
            mustExistFilter(
                filtrereVeilederOgEnhet,
                StatustallAggregationKey.UTGATTE_VARSEL.key,
                "$HENDELSER.${Kategori.UTGATT_VARSEL.name}"
            ),
            mustExistFilter(
                filtrereVeilederOgEnhet,
                StatustallAggregationKey.UDELTE_SAMTALEREFERAT.key,
                "$HENDELSER.${Kategori.UDELT_SAMTALEREFERAT.name}"
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
                QueryBuilders.matchQuery("$SISTE_ENDRINGER.$kategori.$SISTE_ENDRINGER_ER_SETT", "N")
            )
        }

        queryBuilder.must(orQuery)
    }

    private fun byggSisteEndringFilter(sisteEndringKategori: String?, queryBuilder: BoolQueryBuilder) {
        queryBuilder.must(QueryBuilders.existsQuery("$SISTE_ENDRINGER.$sisteEndringKategori"))
    }

    private fun byggMoteMedNavIdag(): RangeQueryBuilder {
        val localDate = LocalDate.now()
        return QueryBuilders.rangeQuery(ALLE_AKTIVITETER_MOTE_STARTDATO)
            .gte(DateUtils.toIsoUTC(localDate.atStartOfDay()))
            .lt(DateUtils.toIsoUTC(localDate.plusDays(1).atStartOfDay()))
    }

    private fun erSykemeldtMedArbeidsgiverFilter(filtrereVeilederOgEnhet: BoolQueryBuilder): FiltersAggregator.KeyedFilter {
        val boolQueryBuilder = QueryBuilders.boolQuery()
            .must(filtrereVeilederOgEnhet)
            .must(QueryBuilders.termQuery(ER_SYKMELDT_MED_ARBEIDSGIVER, true))
            .mustNot(QueryBuilders.existsQuery(UTKAST_14A_STATUS))

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
                .must(QueryBuilders.termQuery(NY_FOR_VEILEDER, true))

        )
    }

    private fun inaktiveBrukere(filtrereVeilederOgEnhet: BoolQueryBuilder): FiltersAggregator.KeyedFilter {
        return FiltersAggregator.KeyedFilter(
            StatustallAggregationKey.INAKTIVE_BRUKERE.key,
            QueryBuilders.boolQuery()
                .must(filtrereVeilederOgEnhet)
                .must(QueryBuilders.matchQuery(FORMIDLINGSGRUPPE_KODE, "ISERV"))

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
                .must(QueryBuilders.matchQuery(FARGEKATEGORI, value))
        )
    }

    private fun ikkeIavtaltAktivitet(filtrereVeilederOgEnhet: BoolQueryBuilder): FiltersAggregator.KeyedFilter {
        return FiltersAggregator.KeyedFilter(
            StatustallAggregationKey.IKKE_I_AVTALT_AKTIVITET.key,
            QueryBuilders.boolQuery()
                .must(filtrereVeilederOgEnhet)
                .mustNot(QueryBuilders.existsQuery(AKTIVITETER))

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
