package no.nav.pto.veilarbportefolje.opensearch

import no.nav.pto.veilarbportefolje.auth.BrukerinnsynTilganger
import no.nav.pto.veilarbportefolje.domene.Sorteringsfelt
import no.nav.pto.veilarbportefolje.domene.Sorteringsrekkefolge
import no.nav.pto.veilarbportefolje.domene.filtervalg.AktivitetFiltervalg
import no.nav.pto.veilarbportefolje.domene.filtervalg.Brukerstatus
import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg
import no.nav.pto.veilarbportefolje.domene.filtervalg.YtelseAapArena
import no.nav.pto.veilarbportefolje.hendelsesfilter.Kategori
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Aktiviteter.AKTIVITET_MOTE_STARTDATO
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Aktiviteter.AKTIVITET_UTLOPSDATOER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Aktiviteter.ALLE_AKTIVITETER_MOTE_STARTDATO
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Aktiviteter.NYESTE_UTLOPTE_AKTIVITET
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Aktiviteter.SISTE_ENDRINGER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Aktiviteter.SISTE_ENDRINGER_TIDSPUNKT
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Annet.FARGEKATEGORI
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Annet.HENDELSER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Annet.HENDELSER_DATO
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Annet.HUSKELAPP
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Annet.HUSKELAPP_ENDRET_DATO
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Annet.HUSKELAPP_FRIST
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Annet.HUSKELAPP_KOMMENTAR
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Annet.TILTAKSHENDELSE
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Annet.TILTAKSHENDELSE_OPPRETTET
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Annet.TILTAKSHENDELSE_TEKST
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Arbeidssoeker.BRUKERS_SITUASJON_SIST_ENDRET
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Arbeidssoeker.UTDANNING_OG_SITUASJON_SIST_ENDRET
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Oppfolging.GJELDENDE_VEDTAK_14A
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Oppfolging.GJELDENDE_VEDTAK_14A_FATTET_DATO
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Oppfolging.GJELDENDE_VEDTAK_14A_HOVEDMAL
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Oppfolging.GJELDENDE_VEDTAK_14A_INNSATSGRUPPE
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Oppfolging.TILDELT_TIDSPUNKT
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Oppfolging.UTKAST_14A_STATUS
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.AKTOER_ID
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.BARN_UNDER_18_AAR
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.BARN_UNDER_18_AAR_DISKRESJONSKODE
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.FNR
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.FOEDELAND_FULLT_NAVN
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.HOVED_STATSBORGERSKAP
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.TALESPRAAK_TOLK
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.TEGNSPRAAK_TOLK
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Personalia.TOLKBEHOV_SIST_OPPDATERT
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.AAP_KELVIN_RETTIGHETSTYPE
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.AAP_KELVIN_TOM_VEDTAKSDATO
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.AAP_MAXTID_UKE
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.AAP_ORDINER_UTLOPSDATO
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.AAP_UNNTAK_UKER_IGJEN
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.DAGPENGER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.DAGPENGER_ANTALL_RESTERENDE_DAGER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.DAGPENGER_DATO_PLANLAGT_STANS
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.DAGPENGER_HAR_DAGPENGER
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.DAGPENGER_RETTIGHETSTYPE
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.ENSLIGE_FORSORGERE_OVERGANGSSTONAD
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.ENSLIGE_FORSORGERE_OVERGANGSSTONAD_HAR_AKTIVITETSPLIKT
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.ENSLIGE_FORSORGERE_OVERGANGSSTONAD_UTLOPSDATO
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.ENSLIGE_FORSORGERE_OVERGANGSSTONAD_VEDTAKSPERIODETYPE
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.ENSLIGE_FORSORGERE_OVERGANGSSTONAD_YNGSTE_BARNS_FØDSELSDATO
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.TILTAKSPENGER_RETTIGHET
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.TILTAKSPENGER_VEDTAKSDATO_TOM
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.UTLOPSDATO
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys.Ytelser.YTELSE
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import org.opensearch.script.Script
import org.opensearch.search.builder.SearchSourceBuilder
import org.opensearch.search.sort.FieldSortBuilder
import org.opensearch.search.sort.ScriptSortBuilder
import org.opensearch.search.sort.ScriptSortBuilder.ScriptSortType
import org.opensearch.search.sort.SortMode
import org.opensearch.search.sort.SortOrder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.stream.Collectors

@Service
class OpensearchSortQueryBuilder {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(OpensearchSortQueryBuilder::class.java)
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
                searchSourceBuilder.sort(ALLE_AKTIVITETER_MOTE_STARTDATO, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.MOTESTATUS -> {
                searchSourceBuilder.sort(AKTIVITET_MOTE_STARTDATO, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.I_AVTALT_AKTIVITET -> {
                val builder = FieldSortBuilder(AKTIVITET_UTLOPSDATOER)
                    .order(sorteringsrekkefolgeOpenSearch)
                    .sortMode(SortMode.MIN)
                searchSourceBuilder.sort(builder)
                searchSourceBuilder
            }

            Sorteringsfelt.FODSELSNUMMER -> {
                searchSourceBuilder.sort("$FNR.raw", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.UTLOPTE_AKTIVITETER -> {
                searchSourceBuilder.sort(NYESTE_UTLOPTE_AKTIVITET, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.AAP_TYPE -> {
                searchSourceBuilder.sort(YTELSE, sorteringsrekkefolgeOpenSearch)
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
                searchSourceBuilder.sort(
                    "$GJELDENDE_VEDTAK_14A.$GJELDENDE_VEDTAK_14A_INNSATSGRUPPE",
                    sorteringsrekkefolgeOpenSearch
                )
                searchSourceBuilder
            }

            Sorteringsfelt.GJELDENDE_VEDTAK_14A_HOVEDMAL -> {
                searchSourceBuilder.sort(
                    "$GJELDENDE_VEDTAK_14A.$GJELDENDE_VEDTAK_14A_HOVEDMAL",
                    sorteringsrekkefolgeOpenSearch
                )
                searchSourceBuilder
            }

            Sorteringsfelt.GJELDENDE_VEDTAK_14A_VEDTAKSDATO -> {
                sorterGjeldendeVedtak14aVedtaksdato(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.UTKAST_14A_STATUS -> {
                searchSourceBuilder.sort(UTKAST_14A_STATUS, sorteringsrekkefolgeOpenSearch)
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
                searchSourceBuilder.sort(TOLKBEHOV_SIST_OPPDATERT, sorteringsrekkefolgeOpenSearch)
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
                searchSourceBuilder.sort(BRUKERS_SITUASJON_SIST_ENDRET, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.UTDANNING_OG_SITUASJON_SIST_ENDRET -> {
                searchSourceBuilder.sort(UTDANNING_OG_SITUASJON_SIST_ENDRET, sorteringsrekkefolgeOpenSearch)
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
                searchSourceBuilder.sort("$HUSKELAPP.$HUSKELAPP_KOMMENTAR", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.HUSKELAPP_SIST_ENDRET -> {
                sorterHuskelappSistEndret(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.FARGEKATEGORI -> {
                searchSourceBuilder.sort(FARGEKATEGORI, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.TILTAKSHENDELSE_DATO_OPPRETTET -> {
                sorterTiltakshendelseOpprettetDato(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.TILTAKSHENDELSE_TEKST -> {
                searchSourceBuilder.sort("$TILTAKSHENDELSE.$TILTAKSHENDELSE_TEKST", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.FILTERHENDELSE_DATO -> {
                if (filtervalg.ferdigfilterListe.contains(Brukerstatus.UTGATTE_VARSEL)) {
                    sorterUtgattVarselHendelseDato(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                } else if (filtervalg.ferdigfilterListe.contains(Brukerstatus.UDELT_SAMTALEREFERAT)) {
                    sorterUdeltSamtalereferatHendelseDato(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                }
                searchSourceBuilder
            }

            Sorteringsfelt.AAP_KELVIN_TOM_VEDTAKSDATO -> {
                sorterAapKelvinTomVedtaksdato(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.AAP_KELVIN_RETTIGHETSTYPE -> {
                searchSourceBuilder.sort(AAP_KELVIN_RETTIGHETSTYPE, sorteringsrekkefolgeOpenSearch)
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
                searchSourceBuilder.sort(TILTAKSPENGER_RETTIGHET, sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.DAGPENGER_RETTIGHETSTYPE -> {
                searchSourceBuilder.sort("$DAGPENGER.$DAGPENGER_RETTIGHETSTYPE", sorteringsrekkefolgeOpenSearch)
                searchSourceBuilder
            }

            Sorteringsfelt.DAGPENGER_ANTALL_RESTERENDE_DAGER -> {
                searchSourceBuilder.sort(
                    "$DAGPENGER.$DAGPENGER_ANTALL_RESTERENDE_DAGER",
                    sorteringsrekkefolgeOpenSearch
                )
                searchSourceBuilder
            }

            Sorteringsfelt.DAGPENGER_PLANGLAGT_STANS -> {
                sorterDagpengerPlanlagtStansDato(searchSourceBuilder, sorteringsrekkefolgeOpenSearch)
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
        val filtrertPaTiltakshendelse = filtervalg.ferdigfilterListe.contains(Brukerstatus.TILTAKSHENDELSER)
        val filtrertPaUtgatteVarsel = filtervalg.ferdigfilterListe.contains(Brukerstatus.UTGATTE_VARSEL)
        val filtrertPaEtGjeldendeVedtak14aFilter = filtervalg.gjeldendeVedtak14a.contains("HAR_14A_VEDTAK") ||
                (filtervalg.harInnsatsgruppeGjeldendeVedtak14a()) ||
                (filtervalg.harHovedmalGjeldendeVedtak14a())

        if (filtrertPaTiltakshendelse) {
            sorterTiltakshendelseOpprettetDato(searchSourceBuilder, SortOrder.ASC)
        } else if (filtrertPaUtgatteVarsel) {
            sorterUtgattVarselHendelseDato(searchSourceBuilder, SortOrder.ASC)
        } else if (filtrertPaEtGjeldendeVedtak14aFilter) {
            sorterGjeldendeVedtak14aVedtaksdato(searchSourceBuilder, SortOrder.ASC)
        } else {
            searchSourceBuilder.sort(AKTOER_ID, SortOrder.ASC)
        }
    }

    fun sorterSisteEndringTidspunkt(builder: SearchSourceBuilder, order: SortOrder?, filtervalg: Filtervalg) {
        if (!filtervalg.harSisteEndringFilter()) {
            return
        }
        val expresion =
            "doc['$SISTE_ENDRINGER.${filtervalg.sisteEndringKategori}.$SISTE_ENDRINGER_TIDSPUNKT']?.value.toInstant().toEpochMilli()"

        val script = Script(expresion)
        val scriptBuilder = ScriptSortBuilder(script, ScriptSortType.NUMBER)
        scriptBuilder.order(order)
        builder.sort(scriptBuilder)
    }

    fun sorterFodeland(searchSourceBuilder: SearchSourceBuilder, order: SortOrder?) {
        searchSourceBuilder.sort(FOEDELAND_FULLT_NAVN, order)
    }

    fun sorterStatsborgerskap(searchSourceBuilder: SearchSourceBuilder, order: SortOrder?) {
        searchSourceBuilder.sort("$HOVED_STATSBORGERSKAP.statsborgerskap", order)
    }

    fun sorterStatsborgerskapGyldigFra(searchSourceBuilder: SearchSourceBuilder, order: SortOrder?) {
        searchSourceBuilder.sort("$HOVED_STATSBORGERSKAP.gyldigFra", order)
    }

    fun sorterTiltakshendelseOpprettetDato(searchSourceBuilder: SearchSourceBuilder, order: SortOrder?) {
        searchSourceBuilder.sort("$TILTAKSHENDELSE.$TILTAKSHENDELSE_OPPRETTET", order)
    }

    fun sorterUtgattVarselHendelseDato(searchSourceBuilder: SearchSourceBuilder, order: SortOrder?) {
        searchSourceBuilder.sort("$HENDELSER.${Kategori.UTGATT_VARSEL.name}.$HENDELSER_DATO", order)
    }

    fun sorterUdeltSamtalereferatHendelseDato(searchSourceBuilder: SearchSourceBuilder, order: SortOrder?) {
        searchSourceBuilder.sort("$HENDELSER.${Kategori.UDELT_SAMTALEREFERAT.name}.$HENDELSER_DATO", order)
    }

    fun sorterGjeldendeVedtak14aVedtaksdato(searchSourceBuilder: SearchSourceBuilder, order: SortOrder?) {
        searchSourceBuilder.sort("$GJELDENDE_VEDTAK_14A.$GJELDENDE_VEDTAK_14A_FATTET_DATO", order)
    }

    fun sorterTolkeSpraak(filtervalg: Filtervalg, searchSourceBuilder: SearchSourceBuilder, order: SortOrder?) {
        if (filtervalg.harTalespraaktolkFilter()) {
            searchSourceBuilder.sort(TALESPRAAK_TOLK, order)
        }
        if (filtervalg.harTegnspraakFilter()) {
            searchSourceBuilder.sort(TEGNSPRAAK_TOLK, order)
        }
    }

    fun sorterAapRettighetsPeriode(builder: SearchSourceBuilder, order: SortOrder?): SearchSourceBuilder {
        val script =
            Script("Math.max((doc.$AAP_MAXTID_UKE.size() != 0) ? doc.$AAP_MAXTID_UKE.value : 0, (doc.$AAP_UNNTAK_UKER_IGJEN.size() != 0) ? doc.$AAP_UNNTAK_UKER_IGJEN.value : 0)")
        val scriptBuilder = ScriptSortBuilder(script, ScriptSortType.NUMBER)
        scriptBuilder.order(order)
        builder.sort(scriptBuilder)
        return builder
    }

    fun sorterAapVurderingsfrist(builder: SearchSourceBuilder, order: SortOrder?, filtervalg: Filtervalg) {
        var expression = ""
        if (filtervalg.harYtelseAapArenaFilter() && filtervalg.ytelseAapArena.size == 2) {
            expression = """
                    if (doc.containsKey('$AAP_UNNTAK_UKER_IGJEN') && !doc['$AAP_UNNTAK_UKER_IGJEN'].empty && doc['$AAP_UNNTAK_UKER_IGJEN'].value != 0) {
                        return doc['$UTLOPSDATO'].value.toInstant().toEpochMilli();
                    }
                    else if (doc.containsKey('$AAP_ORDINER_UTLOPSDATO') && !doc['$AAP_ORDINER_UTLOPSDATO'].empty) {
                        return doc['$AAP_ORDINER_UTLOPSDATO'].value.toInstant().toEpochMilli();
                    }
                    else if (doc.containsKey('$AAP_MAXTID_UKE')) {
                        // Legger til 01.01.2050 i millis for å sortere bak de som har dato
                        return 2524653462000.0 + doc['$AAP_MAXTID_UKE'].value;
                    }
                    else {
                       return 0;
                    }
                    
                    """.trimIndent()
        } else if (filtervalg.harYtelseAapArenaFilter() && filtervalg.ytelseAapArena.contains(YtelseAapArena.HAR_AAP_ORDINAR)) {
            expression = """
                    if (doc.containsKey('$AAP_ORDINER_UTLOPSDATO') && !doc['$AAP_ORDINER_UTLOPSDATO'].empty) {
                        return doc['$AAP_ORDINER_UTLOPSDATO'].value.toInstant().toEpochMilli();
                    }
                    else if (doc.containsKey('$AAP_MAXTID_UKE') && !doc['$AAP_MAXTID_UKE'].empty) {
                        // Legger til 01.01.2050 i millis for å sortere bak de som har dato
                        return 2524653462000.0 + doc['$AAP_MAXTID_UKE'].value;
                    }
                    else {
                        return 0;
                    }
                    
                    """.trimIndent()
        } else if (filtervalg.harYtelseAapArenaFilter() && filtervalg.ytelseAapArena.contains(YtelseAapArena.HAR_AAP_UNNTAK)) {
            expression = """
                    if (doc.containsKey('$UTLOPSDATO') && !doc['$UTLOPSDATO'].empty) {
                        return doc['$UTLOPSDATO'].value.toInstant().toEpochMilli();
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
                            if (doc.containsKey('$HUSKELAPP.$HUSKELAPP_FRIST') && !doc['$HUSKELAPP.$HUSKELAPP_FRIST'].empty) {
                                return doc['$HUSKELAPP.$HUSKELAPP_FRIST'].value.toInstant().toEpochMilli();
                            } else {
                                return 33064243200001.0;
                            }
                            
                            """.trimIndent()
        } else {
            """
                            if (doc.containsKey('$HUSKELAPP.$HUSKELAPP_FRIST') && !doc['$HUSKELAPP.$HUSKELAPP_FRIST'].empty) {
                                return doc['$HUSKELAPP.$HUSKELAPP_FRIST'].value.toInstant().toEpochMilli();
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
                    if (doc.containsKey('$HUSKELAPP.$HUSKELAPP_ENDRET_DATO') && !doc['$HUSKELAPP.$HUSKELAPP_ENDRET_DATO'].empty) {
                        return doc['$HUSKELAPP.$HUSKELAPP_ENDRET_DATO'].value.toInstant().toEpochMilli();
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
                if (!doc['$HUSKELAPP.$HUSKELAPP_FRIST'].empty) {
                    // Trekker fra (fra DateUtils.java) FAR_IN_THE_FUTURE_DATE = "3017-10-07T00:00:00Z" i millis
                    return doc['$HUSKELAPP.$HUSKELAPP_FRIST'].value.toInstant().toEpochMilli() - 33064243200000.0;
                }
                else if (!doc['$HUSKELAPP.$HUSKELAPP_KOMMENTAR'].empty) {
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
                    if (doc.containsKey('$AAP_KELVIN_TOM_VEDTAKSDATO') && !doc['$AAP_KELVIN_TOM_VEDTAKSDATO'].empty) {
                        return doc['$AAP_KELVIN_TOM_VEDTAKSDATO'].value.toInstant().toEpochMilli();
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
                    if (doc.containsKey('$TILTAKSPENGER_VEDTAKSDATO_TOM') && !doc['$TILTAKSPENGER_VEDTAKSDATO_TOM'].empty) {
                        return doc['$TILTAKSPENGER_VEDTAKSDATO_TOM'].value.toInstant().toEpochMilli();
                    } else {
                        return 33064243200001.0;
                    }
                    
                    """.trimIndent()

        val script = Script(expression)
        val scriptBuilder = ScriptSortBuilder(script, ScriptSortType.NUMBER)
        scriptBuilder.order(order)
        builder.sort(scriptBuilder)
    }

    private fun sorterDagpengerPlanlagtStansDato(builder: SearchSourceBuilder, order: SortOrder) {
        val expression = if (order === SortOrder.ASC) {
            """
                    if (doc.containsKey('$DAGPENGER.$DAGPENGER_DATO_PLANLAGT_STANS') && !doc['$DAGPENGER.$DAGPENGER_DATO_PLANLAGT_STANS'].empty) {
                        return doc['$DAGPENGER.$DAGPENGER_DATO_PLANLAGT_STANS'].value.toInstant().toEpochMilli();
                    } else if (doc.containsKey('$DAGPENGER.$DAGPENGER_HAR_DAGPENGER') 
                    && !doc['$DAGPENGER.$DAGPENGER_HAR_DAGPENGER'].empty
                    && doc['$DAGPENGER.$DAGPENGER_HAR_DAGPENGER'].value == true) {
                        return 33064243200001.0;
                    } else {
                         return 43064243200001.0;
                    }
                    """.trimIndent()
        } else {
            """
                    if (doc.containsKey('$DAGPENGER.$DAGPENGER_DATO_PLANLAGT_STANS') && !doc['$DAGPENGER.$DAGPENGER_DATO_PLANLAGT_STANS'].empty) {
                        return doc['$DAGPENGER.$DAGPENGER_DATO_PLANLAGT_STANS'].value.toInstant().toEpochMilli();
                    } else if (doc.containsKey('$DAGPENGER.$DAGPENGER_HAR_DAGPENGER') 
                    && !doc['$DAGPENGER.$DAGPENGER_HAR_DAGPENGER'].empty
                    && doc['$DAGPENGER.$DAGPENGER_HAR_DAGPENGER'].value == true) {
                        return 33064243200001.0;
                    } else {
                         return 0;
                    }
                    """.trimIndent()
        }

        val script = Script(expression)
        val scriptBuilder = ScriptSortBuilder(script, ScriptSortType.NUMBER)
        scriptBuilder.order(order)
        builder.sort(scriptBuilder)
    }

    private fun sorterTildeltTidspunkt(builder: SearchSourceBuilder, order: SortOrder) {
        val expression = """
                    if (doc.containsKey('$TILDELT_TIDSPUNKT') && !doc['$TILDELT_TIDSPUNKT'].empty) {
                        return doc['$TILDELT_TIDSPUNKT'].value.toInstant().toEpochMilli();
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
                if (doc.containsKey('$ENSLIGE_FORSORGERE_OVERGANGSSTONAD.$ENSLIGE_FORSORGERE_OVERGANGSSTONAD_UTLOPSDATO') && !doc['$ENSLIGE_FORSORGERE_OVERGANGSSTONAD.$ENSLIGE_FORSORGERE_OVERGANGSSTONAD_UTLOPSDATO'].empty) {
                    return doc['$ENSLIGE_FORSORGERE_OVERGANGSSTONAD.$ENSLIGE_FORSORGERE_OVERGANGSSTONAD_UTLOPSDATO'].value.toInstant().toEpochMilli();
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
                if (doc.containsKey('$ENSLIGE_FORSORGERE_OVERGANGSSTONAD.$ENSLIGE_FORSORGERE_OVERGANGSSTONAD_YNGSTE_BARNS_FØDSELSDATO') && !doc['$ENSLIGE_FORSORGERE_OVERGANGSSTONAD.$ENSLIGE_FORSORGERE_OVERGANGSSTONAD_YNGSTE_BARNS_FØDSELSDATO'].empty) {
                    return doc['$ENSLIGE_FORSORGERE_OVERGANGSSTONAD.$ENSLIGE_FORSORGERE_OVERGANGSSTONAD_YNGSTE_BARNS_FØDSELSDATO'].value.toInstant().toEpochMilli();
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
                    params._source.$BARN_UNDER_18_AAR.size()
                    
                    """.trimIndent()

            val expression6 = """
                    int count = 0;
                    for (item in params._source.$BARN_UNDER_18_AAR) {
                        if ((item.$BARN_UNDER_18_AAR_DISKRESJONSKODE == null) || (item.$BARN_UNDER_18_AAR_DISKRESJONSKODE == '6') || (item.$BARN_UNDER_18_AAR_DISKRESJONSKODE == '19')){ count = count + 1; }
                    }
                    return count;
                    
                    """.trimIndent()

            val expression7 = """
                    int count = 0;
                    for (item in params._source.$BARN_UNDER_18_AAR) {
                        if ((item.$BARN_UNDER_18_AAR_DISKRESJONSKODE == null) || (item.$BARN_UNDER_18_AAR_DISKRESJONSKODE == '7')){ count = count + 1; }
                    }
                    return count;
                    
                    """.trimIndent()

            val expressionIngen = """
                    int count = 0;
                    for (item in params._source.$BARN_UNDER_18_AAR) {
                        if (item.$BARN_UNDER_18_AAR_DISKRESJONSKODE == null){ count = count + 1; }
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
        builder.sort(
            "$ENSLIGE_FORSORGERE_OVERGANGSSTONAD.$ENSLIGE_FORSORGERE_OVERGANGSSTONAD_VEDTAKSPERIODETYPE",
            order
        )
    }

    private fun sorterEnsligeForsorgereAktivitetsPlikt(builder: SearchSourceBuilder, order: SortOrder) {
        builder.sort(
            "$ENSLIGE_FORSORGERE_OVERGANGSSTONAD.$ENSLIGE_FORSORGERE_OVERGANGSSTONAD_HAR_AKTIVITETSPLIKT",
            order
        )
    }

    private fun addSecondarySort(searchSourceBuilder: SearchSourceBuilder) {
        searchSourceBuilder.sort(AKTOER_ID, SortOrder.ASC)
    }
}
