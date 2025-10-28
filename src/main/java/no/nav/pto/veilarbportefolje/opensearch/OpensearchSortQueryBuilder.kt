package no.nav.pto.veilarbportefolje.opensearch

import no.nav.pto.veilarbportefolje.auth.BrukerinnsynTilganger
import no.nav.pto.veilarbportefolje.domene.*
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import org.opensearch.script.Script
import org.opensearch.search.builder.SearchSourceBuilder
import org.opensearch.search.sort.FieldSortBuilder
import org.opensearch.search.sort.ScriptSortBuilder
import org.opensearch.search.sort.ScriptSortBuilder.ScriptSortType
import org.opensearch.search.sort.SortMode
import org.opensearch.search.sort.SortOrder
import java.util.*
import java.util.stream.Collectors
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OpensearchSortQueryBuilder {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(OpensearchFilterQueryBuilder::class.java)
    }
    fun byggVeilederPaaEnhetScript(veilederePaaEnhet: List<String?>): String {
        val veiledere = veilederePaaEnhet.stream()
            .map { id: String? -> String.format("\"%s\"", id) }
            .collect(Collectors.joining(","))

        val veilederListe = String.format("[%s]", veiledere)
        return String.format(
            "(doc.veileder_id.size() != 0 && %s.contains(doc.veileder_id.value)).toString()",
            veilederListe
        )
    }


    /**
     * Tar i mot en [SearchSourceBuilder] og utvider denne med sorteringsqueryer basert på valgt sorteringsrekkefølge,
     * sorteringsfelt og valgte filter.
     *
     *
     * Merk: [Sorteringsfelt] er en enum som representerer lovlige sorteringsfelter slik frontend har definert dem.
     * Disse mappes til felter i OpenSearch, dvs. for enkelte verdier av [Sorteringsfelt] kan det være at feltet
     * det faktisk sorteres på heter noe annet i OpenSearch. Siden [PortefoljebrukerOpensearchModell] er "fasiten" for hvilke felter
     * som er lov å sortere på i OpenSearch er det viktig at vi ikke legger til nye sorteringsfelter i [Sorteringsfelt]
     * uten å sørge for at disse også er tilgjengelige i [PortefoljebrukerOpensearchModell].
     */
    fun sorterQueryParametere(
        sorteringsrekkefolge: Sorteringsrekkefolge,
        sorteringsfelt: Sorteringsfelt,
        searchSourceBuilder: SearchSourceBuilder,
        filtervalg: Filtervalg,
        brukerinnsynTilganger: BrukerinnsynTilganger
    ): SearchSourceBuilder {
        val sorteringsrekkefolgeOpenSearch =
            if (Sorteringsrekkefolge.STIGENDE == sorteringsrekkefolge) SortOrder.ASC else SortOrder.DESC

        // Vi må assigne til en ny variabel for at kompilatoren sin exhaustiveness-check skal slå inn.
        // Dette er strengt tatt ikke nødvendig da vi bare kunne returnert searchSourceBuilder direkte, men da ville vi
        // mistet exhaustiveness-checken.
        val hovedsortering = when (sorteringsfelt) {
            Sorteringsfelt.IKKE_SATT -> {
                brukStandardsorteringBasertPaValgteFilter(filtervalg, searchSourceBuilder)
                searchSourceBuilder
            }

            Sorteringsfelt.VALGTE_AKTIVITETER -> {
                sorterValgteAktiviteter(filtervalg, searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.MOTER_MED_NAV_IDAG -> {
                searchSourceBuilder.sort("alle_aktiviteter_mote_startdato", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.MOTESTATUS -> {
                searchSourceBuilder.sort("aktivitet_mote_startdato", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.I_AVTALT_AKTIVITET -> {
                val builder = FieldSortBuilder("aktivitet_utlopsdatoer")
                    .order(sorteringsrekkefolgeOpenSearch)
                    .sortMode(SortMode.MIN)
                searchSourceBuilder.sort(builder)
                searchSourceBuilder
            }

            Sorteringsfelt.FODSELSNUMMER -> {
                searchSourceBuilder.sort("fnr.raw", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.UTLOPTE_AKTIVITETER -> {
                searchSourceBuilder.sort("nyesteutlopteaktivitet", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.AAP_TYPE -> {
                searchSourceBuilder.sort("ytelse", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.AAP_VURDERINGSFRIST -> {
                sorterAapVurderingsfrist(searchSourceBuilder, sorteringsrekkefolgeOpenSearch, filtervalg)
                searchSourceBuilder
            }

            Sorteringsfelt.AAP_RETTIGHETSPERIODE -> {
                sorterAapRettighetsPeriode(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.GJELDENDE_VEDTAK_14A_INNSATSGRUPPE -> {
                searchSourceBuilder.sort("gjeldendeVedtak14a.innsatsgruppe", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.GJELDENDE_VEDTAK_14A_HOVEDMAL -> {
                searchSourceBuilder.sort("gjeldendeVedtak14a.hovedmal", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.GJELDENDE_VEDTAK_14A_VEDTAKSDATO -> {
                sorterGjeldendeVedtak14aVedtaksdato(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.UTKAST_14A_STATUS -> {
                searchSourceBuilder.sort("utkast_14a_status", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.SISTE_ENDRING_DATO -> {
                sorterSisteEndringTidspunkt(searchSourceBuilder, sorteringsrekkefolgeOpenSearch, filtervalg)
                searchSourceBuilder
            }

            Sorteringsfelt.FODELAND -> {
                sorterFodeland(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.STATSBORGERSKAP -> {
                sorterStatsborgerskap(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.STATSBORGERSKAP_GYLDIG_FRA -> {
                sorterStatsborgerskapGyldigFra(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.TOLKESPRAK -> {
                sorterTolkeSpraak(filtervalg, searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.TOLKEBEHOV_SIST_OPPDATERT -> {
                searchSourceBuilder.sort("tolkBehovSistOppdatert", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.ENSLIGE_FORSORGERE_UTLOP_YTELSE -> {
                sorterEnsligeForsorgereUtlopsDato(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.ENSLIGE_FORSORGERE_VEDTAKSPERIODETYPE -> {
                sorterEnsligeForsorgereVedtaksPeriode(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.ENSLIGE_FORSORGERE_AKTIVITETSPLIKT -> {
                sorterEnsligeForsorgereAktivitetsPlikt(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.ENSLIGE_FORSORGERE_OM_BARNET -> {
                sorterEnsligeForsorgereOmBarnet(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.BARN_UNDER_18_AR -> {
                sorterBarnUnder18(
                    searchSourceBuilder,
                    sorteringsrekkefolgeOpenSearch,
                    brukerinnsynTilganger,
                    filtervalg
                )
                searchSourceBuilder
            }

            Sorteringsfelt.BRUKERS_SITUASJON_SIST_ENDRET -> {
                searchSourceBuilder.sort("brukers_situasjon_sist_endret", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.UTDANNING_OG_SITUASJON_SIST_ENDRET -> {
                searchSourceBuilder.sort("utdanning_og_situasjon_sist_endret", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.HUSKELAPP_FRIST -> {
                sorterHuskelappFrist(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.HUSKELAPP -> {
                sorterHuskelappEksistere(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.HUSKELAPP_KOMMENTAR -> {
                searchSourceBuilder.sort("huskelapp.kommentar", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.HUSKELAPP_SIST_ENDRET -> {
                sorterHuskelappSistEndret(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.FARGEKATEGORI -> {
                searchSourceBuilder.sort("fargekategori", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.TILTAKSHENDELSE_DATO_OPPRETTET -> {
                sorterTiltakshendelseOpprettetDato(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.TILTAKSHENDELSE_TEKST -> {
                searchSourceBuilder.sort("tiltakshendelse.tekst", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.UTGATT_VARSEL_DATO -> {
                sorterUtgattVarselHendelseDato(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.AAP_KELVIN_TOM_VEDTAKSDATO -> {
                sorterAapKelvinTomVedtaksdato(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.AAP_KELVIN_RETTIGHETSTYPE -> {
                searchSourceBuilder.sort("aap_kelvin_rettighetstype", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.TILDELT_TIDSPUNKT -> {
                sorterTildeltTidspunkt(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.TILTAKSPENGER_VEDTAKSDATO_TOM -> {
                sorterTiltakspengerVedtaksdatoTom(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.TILTAKSPENGER_RETTIGHET -> {
                searchSourceBuilder.sort("tiltakspenger_rettighet", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.ETTERNAVN, Sorteringsfelt.CV_SVARFRIST, Sorteringsfelt.AAP_MAXTID_UKE, Sorteringsfelt.AAP_UNNTAK_UKER_IGJEN, Sorteringsfelt.VENTER_PA_SVAR_FRA_NAV, Sorteringsfelt.VENTER_PA_SVAR_FRA_BRUKER, Sorteringsfelt.STARTDATO_FOR_AVTALT_AKTIVITET, Sorteringsfelt.NESTE_STARTDATO_FOR_AVTALT_AKTIVITET, Sorteringsfelt.FORRIGE_DATO_FOR_AVTALT_AKTIVITET, Sorteringsfelt.UTKAST_14A_STATUS_ENDRET, Sorteringsfelt.UTKAST_14A_ANSVARLIG_VEILEDER, Sorteringsfelt.BOSTED_KOMMUNE, Sorteringsfelt.BOSTED_BYDEL, Sorteringsfelt.BOSTED_SIST_OPPDATERT, Sorteringsfelt.OPPFOLGING_STARTET, Sorteringsfelt.UTLOPSDATO, Sorteringsfelt.VEILEDER_IDENT, Sorteringsfelt.DAGPENGER_UTLOP_UKE, Sorteringsfelt.DAGPENGER_PERM_UTLOP_UKE -> {
                searchSourceBuilder.sort(sorteringsfelt.sorteringsverdi, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }
        }
        addSecondarySort(hovedsortering)
        return hovedsortering
    }

    private fun brukStandardsorteringBasertPaValgteFilter(
        filtervalg: Filtervalg,
        searchSourceBuilder: SearchSourceBuilder
    ) {
        val filtrertPaTiltakshendelse =
            filtervalg.ferdigfilterListe != null && filtervalg.ferdigfilterListe.contains(Brukerstatus.TILTAKSHENDELSER)
        val filtrertPaUtgatteVarsel =
            filtervalg.ferdigfilterListe != null && filtervalg.ferdigfilterListe.contains(Brukerstatus.UTGATTE_VARSEL)
        val filtrertPaEtGjeldendeVedtak14aFilter = filtervalg.gjeldendeVedtak14a.contains("HAR_14A_VEDTAK") ||
                (filtervalg.innsatsgruppeGjeldendeVedtak14a != null && filtervalg.innsatsgruppeGjeldendeVedtak14a.isNotEmpty()) ||
                (filtervalg.hovedmalGjeldendeVedtak14a != null && filtervalg.hovedmalGjeldendeVedtak14a.isNotEmpty())

        if (filtrertPaTiltakshendelse) {
            sorterTiltakshendelseOpprettetDato(searchSourceBuilder, SortOrder.ASC)
        } else if (filtrertPaUtgatteVarsel) {
            sorterUtgattVarselHendelseDato(searchSourceBuilder, SortOrder.ASC)
        } else if (filtrertPaEtGjeldendeVedtak14aFilter) {
            sorterGjeldendeVedtak14aVedtaksdato(searchSourceBuilder, SortOrder.ASC)
        } else {
            searchSourceBuilder.sort("aktoer_id", SortOrder.ASC)
        }
    }

    fun sorterSisteEndringTidspunkt(builder: SearchSourceBuilder, order: SortOrder?, filtervalg: Filtervalg) {
        if (filtervalg.sisteEndringKategori.size == 0) {
            return
        }
        if (filtervalg.sisteEndringKategori.size != 1) {
            log.error(
                "Det ble sortert på flere ulike siste endringer: {}",
                filtervalg.sisteEndringKategori.size
            )
            throw IllegalStateException("Filtrering på flere siste_endringer er ikke tilatt.")
        }
        val expresion =
            "doc['siste_endringer." + filtervalg.sisteEndringKategori[0] + ".tidspunkt']?.value.toInstant().toEpochMilli()"

        val script = Script(expresion)
        val scriptBuilder = ScriptSortBuilder(script, ScriptSortType.NUMBER)
        scriptBuilder.order(order)
        builder.sort(scriptBuilder)
    }

    fun sorterFodeland(searchSourceBuilder: SearchSourceBuilder, order: SortOrder?) {
        searchSourceBuilder.sort("foedelandFulltNavn", order)
    }

    fun sorterStatsborgerskap(searchSourceBuilder: SearchSourceBuilder, order: SortOrder?) {
        searchSourceBuilder.sort("hovedStatsborgerskap.statsborgerskap", order)
    }

    fun sorterStatsborgerskapGyldigFra(searchSourceBuilder: SearchSourceBuilder, order: SortOrder?) {
        searchSourceBuilder.sort("hovedStatsborgerskap.gyldigFra", order)
    }

    fun sorterTiltakshendelseOpprettetDato(searchSourceBuilder: SearchSourceBuilder, order: SortOrder?) {
        searchSourceBuilder.sort("tiltakshendelse.opprettet", order)
    }

    fun sorterUtgattVarselHendelseDato(searchSourceBuilder: SearchSourceBuilder, order: SortOrder?) {
        searchSourceBuilder.sort("utgatt_varsel.dato", order)
    }

    fun sorterGjeldendeVedtak14aVedtaksdato(searchSourceBuilder: SearchSourceBuilder, order: SortOrder?) {
        searchSourceBuilder.sort("gjeldendeVedtak14a.fattetDato", order)
    }

    fun sorterTolkeSpraak(filtervalg: Filtervalg, searchSourceBuilder: SearchSourceBuilder, order: SortOrder?) {
        if (filtervalg.harTalespraaktolkFilter()) {
            searchSourceBuilder.sort("talespraaktolk", order)
        }
        if (filtervalg.harTegnspraakFilter()) {
            searchSourceBuilder.sort("tegnspraaktolk", order)
        }
    }

    fun sorterAapRettighetsPeriode(builder: SearchSourceBuilder, order: SortOrder?): SearchSourceBuilder {
        val script =
            Script("Math.max((doc.aapmaxtiduke.size() != 0) ? doc.aapmaxtiduke.value : 0, (doc.aapunntakukerigjen.size() != 0) ? doc.aapunntakukerigjen.value : 0)")
        val scriptBuilder = ScriptSortBuilder(script, ScriptSortType.NUMBER)
        scriptBuilder.order(order)
        builder.sort(scriptBuilder)
        return builder
    }

    fun sorterAapVurderingsfrist(builder: SearchSourceBuilder, order: SortOrder?, filtervalg: Filtervalg) {
        var expression = ""
        if (filtervalg.harYtelseAapArenaFilter() && filtervalg.ytelseAapArena.size == 2) {
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
                    
                    """.trimIndent()
        } else if (filtervalg.harYtelseAapArenaFilter() && filtervalg.ytelseAapArena.contains(YtelseAapArena.HAR_AAP_ORDINAR)) {
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
                    
                    """.trimIndent()
        } else if (filtervalg.harYtelseAapArenaFilter() && filtervalg.ytelseAapArena.contains(YtelseAapArena.HAR_AAP_UNNTAK)) {
            expression = """
                    if (doc.containsKey('utlopsdato') && !doc['utlopsdato'].empty) {
                        return doc['utlopsdato'].value.toInstant().toEpochMilli();
                    }
                    else {
                        return 0;
                    }
                    
                    """.trimIndent()
        }

        if (expression.isNotEmpty()) {
            val script = Script(expression)
            val scriptBuilder = ScriptSortBuilder(script, ScriptSortType.NUMBER)
            scriptBuilder.order(order)
            builder.sort(scriptBuilder)
        }
    }
fun sorterValgteAktiviteter(
    filtervalg: Filtervalg,
    builder: SearchSourceBuilder,
    order: SortOrder?
): SearchSourceBuilder {
    val sorteringsAktiviteter: List<String> = when {
        filtervalg.harAktiviteterForenklet() -> filtervalg.aktiviteterForenklet
        filtervalg.tiltakstyper.isNotEmpty() -> listOf("TILTAK")
        else -> filtervalg.aktiviteter
            .filter { it.value == AktivitetFiltervalg.JA }
            .map { it.key ?: "" }
            .filter { it.isNotEmpty() }
    }

    if (sorteringsAktiviteter.isEmpty()) {
        return builder
    }

    val script = buildString {
        append("List l = new ArrayList(); ")
        sorteringsAktiviteter.forEach { aktivitet ->
            append("l.add(doc['aktivitet_${aktivitet.lowercase(Locale.getDefault())}_utlopsdato']?.value.toInstant().toEpochMilli()); ")
        }
        append("return l.stream().sorted().findFirst().get();")
    }

    val scriptBuilder = ScriptSortBuilder(Script(script), ScriptSortType.NUMBER)
    scriptBuilder.order(order)
    builder.sort(scriptBuilder)
    return builder
}

    private fun sorterHuskelappFrist(builder: SearchSourceBuilder, order: SortOrder) {
        val expression = if (order === SortOrder.ASC) {
            """
                            if (doc.containsKey('huskelapp.frist') && !doc['huskelapp.frist'].empty) {
                                return doc['huskelapp.frist'].value.toInstant().toEpochMilli();
                            } else {
                                return 33064243200001.0;
                            }
                            
                            """.trimIndent()
        } else {
            """
                            if (doc.containsKey('huskelapp.frist') && !doc['huskelapp.frist'].empty) {
                                return doc['huskelapp.frist'].value.toInstant().toEpochMilli();
                            } else {
                                return 0.0;
                            }
                            
                            """.trimIndent()
        }

        val script = Script(expression)
        val scriptBuilder = ScriptSortBuilder(script, ScriptSortType.NUMBER)
        scriptBuilder.order(order)
        builder.sort(scriptBuilder)
    }

    private fun sorterHuskelappSistEndret(builder: SearchSourceBuilder, order: SortOrder) {
        val expression = """
                    if (doc.containsKey('huskelapp.endretDato') && !doc['huskelapp.endretDato'].empty) {
                        return doc['huskelapp.endretDato'].value.toInstant().toEpochMilli();
                    } else {
                        return 33064243200001.0;
                    }
                    
                    """.trimIndent()

        val script = Script(expression)
        val scriptBuilder = ScriptSortBuilder(script, ScriptSortType.NUMBER)
        scriptBuilder.order(order)
        builder.sort(scriptBuilder)
    }


    private fun sorterHuskelappEksistere(builder: SearchSourceBuilder, order: SortOrder) {
        val expresion = """
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
                
                """.trimIndent()
        val script = Script(expresion)
        val scriptBuilder = ScriptSortBuilder(script, ScriptSortType.NUMBER)
        scriptBuilder.order(order)
        builder.sort(scriptBuilder)
    }

    private fun sorterAapKelvinTomVedtaksdato(builder: SearchSourceBuilder, order: SortOrder) {
        val expression = """
                    if (doc.containsKey('aap_kelvin_tom_vedtaksdato') && !doc['aap_kelvin_tom_vedtaksdato'].empty) {
                        return doc['aap_kelvin_tom_vedtaksdato'].value.toInstant().toEpochMilli();
                    } else {
                        return 33064243200001.0;
                    }
                    
                    """.trimIndent()

        val script = Script(expression)
        val scriptBuilder = ScriptSortBuilder(script, ScriptSortType.NUMBER)
        scriptBuilder.order(order)
        builder.sort(scriptBuilder)
    }

    private fun sorterTiltakspengerVedtaksdatoTom(builder: SearchSourceBuilder, order: SortOrder) {
        val expression = """
                    if (doc.containsKey('tiltakspenger_vedtaksdato_tom') && !doc['tiltakspenger_vedtaksdato_tom'].empty) {
                        return doc['tiltakspenger_vedtaksdato_tom'].value.toInstant().toEpochMilli();
                    } else {
                        return 33064243200001.0;
                    }
                    
                    """.trimIndent()

        val script = Script(expression)
        val scriptBuilder = ScriptSortBuilder(script, ScriptSortType.NUMBER)
        scriptBuilder.order(order)
        builder.sort(scriptBuilder)
    }

    private fun sorterTildeltTidspunkt(builder: SearchSourceBuilder, order: SortOrder) {
        val expression = """
                    if (doc.containsKey('tildelt_tidspunkt') && !doc['tildelt_tidspunkt'].empty) {
                        return doc['tildelt_tidspunkt'].value.toInstant().toEpochMilli();
                    } else {
                        return 33064243200001.0;
                    }
                    
                    """.trimIndent()

        val script = Script(expression)
        val scriptBuilder = ScriptSortBuilder(script, ScriptSortType.NUMBER)
        scriptBuilder.order(order)
        builder.sort(scriptBuilder)
    }

    private fun sorterEnsligeForsorgereUtlopsDato(builder: SearchSourceBuilder, order: SortOrder) {
        val expresion = """
                if (doc.containsKey('enslige_forsorgere_overgangsstonad.utlopsDato') && !doc['enslige_forsorgere_overgangsstonad.utlopsDato'].empty) {
                    return doc['enslige_forsorgere_overgangsstonad.utlopsDato'].value.toInstant().toEpochMilli();
                }
                else {
                  return 0;
                }
                
                """.trimIndent()
        val script = Script(expresion)
        val scriptBuilder = ScriptSortBuilder(script, ScriptSortType.NUMBER)
        scriptBuilder.order(order)
        builder.sort(scriptBuilder)
    }

    private fun sorterEnsligeForsorgereOmBarnet(builder: SearchSourceBuilder, order: SortOrder) {
        val expresion = """
                if (doc.containsKey('enslige_forsorgere_overgangsstonad.yngsteBarnsFødselsdato') && !doc['enslige_forsorgere_overgangsstonad.yngsteBarnsFødselsdato'].empty) {
                    return doc['enslige_forsorgere_overgangsstonad.yngsteBarnsFødselsdato'].value.toInstant().toEpochMilli();
                }
                else {
                    return 0;
                }
                
                """.trimIndent()
        val script = Script(expresion)
        val scriptBuilder = ScriptSortBuilder(script, ScriptSortType.NUMBER)
        scriptBuilder.order(order)
        builder.sort(scriptBuilder)
    }

    private fun sorterBarnUnder18(
        searchSourceBuilder: SearchSourceBuilder,
        order: SortOrder,
        brukerinnsynTilganger: BrukerinnsynTilganger,
        filtervalg: Filtervalg
    ) {
        val harTilgangKode6 = brukerinnsynTilganger.tilgangTilAdressebeskyttelseStrengtFortrolig
        val harTilgangKode7 = brukerinnsynTilganger.tilgangTilAdressebeskyttelseFortrolig
        val hartilgangKode6og7 = harTilgangKode6 && harTilgangKode7

        val harBaretilgangKode6 = harTilgangKode6 && !harTilgangKode7
        val harBaretilgangKode7 = !harTilgangKode6 && harTilgangKode7


        if (filtervalg.harBarnUnder18AarFilter()) {
            val expressionTilgang6og7 = """
                    params._source.barn_under_18_aar.size()
                    
                    """.trimIndent()

            val expression6 = """
                    int count = 0;
                    for (item in params._source.barn_under_18_aar) {
                        if ((item.diskresjonskode == null) || (item.diskresjonskode == '6') || (item.diskresjonskode == '19')){ count = count + 1; }
                    }
                    return count;
                    
                    """.trimIndent()

            val expression7 = """
                    int count = 0;
                    for (item in params._source.barn_under_18_aar) {
                        if ((item.diskresjonskode == null) || (item.diskresjonskode == '7')){ count = count + 1; }
                    }
                    return count;
                    
                    """.trimIndent()

            val expressionIngen = """
                    int count = 0;
                    for (item in params._source.barn_under_18_aar) {
                        if (item.diskresjonskode == null){ count = count + 1; }
                    }
                    return count;
                    
                    """.trimIndent()
            val expressionToUse: String = if (hartilgangKode6og7) {
                expressionTilgang6og7
            } else if (harBaretilgangKode6) {
                expression6
            } else if (harBaretilgangKode7) {
                expression7
            } else {
                expressionIngen
            }

            val script = Script(expressionToUse)
            val scriptBuilder = ScriptSortBuilder(script, ScriptSortType.NUMBER)
            scriptBuilder.order(order)
            searchSourceBuilder.sort(scriptBuilder)
        }
    }

    private fun sorterEnsligeForsorgereVedtaksPeriode(builder: SearchSourceBuilder, order: SortOrder) {
        builder.sort("enslige_forsorgere_overgangsstonad.vedtaksPeriodetype", order)
    }

    private fun sorterEnsligeForsorgereAktivitetsPlikt(builder: SearchSourceBuilder, order: SortOrder) {
        builder.sort("enslige_forsorgere_overgangsstonad.harAktivitetsplikt", order)
    }

    private fun addSecondarySort(searchSourceBuilder: SearchSourceBuilder) {
        searchSourceBuilder.sort("aktoer_id", SortOrder.ASC)
    }
}