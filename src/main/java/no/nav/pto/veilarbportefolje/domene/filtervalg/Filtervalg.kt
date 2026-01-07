package no.nav.pto.veilarbportefolje.domene.filtervalg

import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.JobbSituasjonBeskrivelse
import no.nav.pto.veilarbportefolje.domene.Kjonn
import no.nav.pto.veilarbportefolje.domene.ManuellBrukerStatus
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe
import java.util.Collections.emptyList

data class Filtervalg(
    var ferdigfilterListe: List<Brukerstatus>? = null,
    var alder: List<String> = emptyList(),
    var kjonn: Kjonn?,
    var fodselsdagIMnd: List<String> = emptyList(),
    var formidlingsgruppe: List<Formidlingsgruppe> = emptyList(),
    var servicegruppe: List<Servicegruppe> = emptyList(),
    var rettighetsgruppe: List<Rettighetsgruppe> = emptyList(),
    var veiledere: List<String> = emptyList(),
    var aktiviteter: MutableMap<String, AktivitetFiltervalg> = mutableMapOf(),
    var tiltakstyper: List<String> = emptyList(),
    var manuellBrukerStatus: List<ManuellBrukerStatus> = emptyList(),
    var navnEllerFnrQuery: String?,
    var registreringstype: List<JobbSituasjonBeskrivelse> = emptyList(),
    var utdanning: List<UtdanningSvar> = emptyList(),
    var utdanningBestatt: List<UtdanningBestattSvar> = emptyList(),
    var utdanningGodkjent: List<UtdanningGodkjentSvar> = emptyList(),
    var sisteEndringKategori: List<String> = emptyList(), // TODO: trenger dette å være en liste? sender kun en kategori via radioknapper
    var aktiviteterForenklet: List<String> = emptyList(),
    var alleAktiviteter: List<String> = emptyList(), // finst ikkje i veilarbportefoljeflatefs
    var ulesteEndringer: String?,
    var cvJobbprofil: CVjobbprofil?,
    var landgruppe: List<String>?,
    var foedeland: List<String>?,
    var tolkebehov: List<String>?,
    var tolkBehovSpraak: List<String>?,
    var stillingFraNavFilter: List<StillingFraNAVFilter>?,
    var barnUnder18Aar: List<BarnUnder18Aar> = emptyList(),
    var barnUnder18AarAlder: List<String> = emptyList(),
    var geografiskBosted: List<String>?,
    var ensligeForsorgere: List<EnsligeForsorgere>?,
    var fargekategorier: List<String> = emptyList(),
    var gjeldendeVedtak14a: List<String> = emptyList(),
    var innsatsgruppeGjeldendeVedtak14a: List<Innsatsgruppe> = emptyList(),
    var hovedmalGjeldendeVedtak14a: List<Hovedmal> = emptyList(),
    var ytelseAapArena: List<YtelseAapArena> = emptyList(),
    var ytelseAapKelvin: List<YtelseAapKelvin> = emptyList(),
    var ytelseTiltakspenger: List<YtelseTiltakspenger> = emptyList(),
    var ytelseTiltakspengerArena: List<YtelseTiltakspengerArena> = emptyList(),
    var ytelseDagpengerArena: List<YtelseDagpengerArena> = emptyList()
) {

    fun harAktiveFilter(): Boolean =
        harFerdigFilter() ||
                alder.isNotEmpty() ||
                harKjonnfilter() ||
                fodselsdagIMnd.isNotEmpty() ||
                formidlingsgruppe.isNotEmpty() ||
                servicegruppe.isNotEmpty() ||
                rettighetsgruppe.isNotEmpty() ||
                veiledere.isNotEmpty() ||
                aktiviteter.isNotEmpty() ||
                tiltakstyper.isNotEmpty() ||
                registreringstype.isNotEmpty() ||
                utdanning.isNotEmpty() ||
                utdanningBestatt.isNotEmpty() ||
                utdanningGodkjent.isNotEmpty() ||
                sisteEndringKategori.isNotEmpty() ||
                harAktiviteterForenklet() ||
                harCvFilter() ||
                harManuellBrukerStatus() ||
                harNavnEllerFnrQuery() ||
                harTolkbehovSpraakFilter() ||
                harTegnspraakFilter() ||
                harTalespraaktolkFilter() ||
                harFoedelandFilter() ||
                harStillingFraNavFilter() ||
                harBarnUnder18AarFilter() ||
                harLandgruppeFilter() ||
                harBostedFilter() ||
                harEnsligeForsorgereFilter() ||
                harFargeKategoriFilter() ||
                harGjeldendeVedtak14aFilter() ||
                harInnsatsgruppeGjeldendeVedtak14a() ||
                harHovedmalGjeldendeVedtak14a() ||
                harYtelseAapArenaFilter() ||
                harYtelseAapKelvinFilter() ||
                harYtelseTiltakspengerFilter() ||
                harYtelseTiltakspengerArenaFilter() ||
                harYtelseDagpengerArenaFilter()

    fun harGjeldendeVedtak14aFilter(): Boolean =
        gjeldendeVedtak14a.isNotEmpty()

    fun harInnsatsgruppeGjeldendeVedtak14a(): Boolean =
        innsatsgruppeGjeldendeVedtak14a.isNotEmpty()

    fun harHovedmalGjeldendeVedtak14a(): Boolean =
        hovedmalGjeldendeVedtak14a.isNotEmpty()

    fun harEnsligeForsorgereFilter(): Boolean =
        !ensligeForsorgere.isNullOrEmpty()

    fun harCvFilter(): Boolean =
        cvJobbprofil != null

    fun harFerdigFilter(): Boolean =
        !ferdigfilterListe.isNullOrEmpty()

    fun harYtelseAapKelvinFilter(): Boolean =
        ytelseAapKelvin.isNotEmpty()

    fun harYtelseAapArenaFilter(): Boolean =
        ytelseAapArena.isNotEmpty()

    fun harYtelseTiltakspengerFilter(): Boolean =
        ytelseTiltakspenger.isNotEmpty()

    fun harYtelseTiltakspengerArenaFilter(): Boolean =
        ytelseTiltakspengerArena.isNotEmpty()

    fun harYtelseDagpengerArenaFilter(): Boolean =
        ytelseDagpengerArena.isNotEmpty()

    fun harKjonnfilter(): Boolean =
        kjonn != null

    //TODO: denne er alltid true, og sjekker kun en type av aktivitetene. Fjernes eller endres?
    fun harAktivitetFilter(): Boolean =
        tiltakstyper != null

    fun harSisteEndringFilter(): Boolean =
        sisteEndringKategori.isNotEmpty()

    fun harManuellBrukerStatus(): Boolean =
        manuellBrukerStatus.isNotEmpty()

    fun harAktiviteterForenklet(): Boolean =
        aktiviteterForenklet.isNotEmpty()

    fun harNavnEllerFnrQuery(): Boolean =
        !navnEllerFnrQuery.isNullOrBlank()

    fun harUlesteEndringerFilter(): Boolean =
        !ulesteEndringer.isNullOrBlank()

    fun harFoedelandFilter(): Boolean =
        !foedeland.isNullOrEmpty()

    fun harTolkbehovSpraakFilter(): Boolean =
        !tolkBehovSpraak.isNullOrEmpty()

    fun harTalespraaktolkFilter(): Boolean =
        tolkebehov?.contains("TALESPRAAKTOLK") == true

    fun harTegnspraakFilter(): Boolean =
        tolkebehov?.contains("TEGNSPRAAKTOLK") == true

    fun harLandgruppeFilter(): Boolean =
        !landgruppe.isNullOrEmpty()

    fun harFargeKategoriFilter(): Boolean =
        fargekategorier.isNotEmpty()

    fun harStillingFraNavFilter(): Boolean =
        !stillingFraNavFilter.isNullOrEmpty()

    fun harBarnUnder18AarFilter(): Boolean =
        barnUnder18Aar.isNotEmpty() || barnUnder18AarAlder.isNotEmpty()

    fun harBostedFilter(): Boolean =
        !geografiskBosted.isNullOrEmpty()

    fun harDinSituasjonSvar(): Boolean =
        registreringstype.isNotEmpty()

    fun harUtdanningSvar(): Boolean =
        utdanning.isNotEmpty()

    fun harUtdanningBestattSvar(): Boolean =
        utdanningBestatt.isNotEmpty()

    fun harUtdanningGodkjentSvar(): Boolean =
        utdanningGodkjent.isNotEmpty()

}
