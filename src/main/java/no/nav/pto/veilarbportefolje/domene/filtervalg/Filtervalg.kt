package no.nav.pto.veilarbportefolje.domene.filtervalg

import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.JobbSituasjonBeskrivelse
import no.nav.pto.veilarbportefolje.domene.Kjonn
import no.nav.pto.veilarbportefolje.domene.ManuellBrukerStatus
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe

data class Filtervalg(
    val ferdigfilterListe: List<Brukerstatus>,
    val alder: List<String>,
    val kjonn: Kjonn?,
    val fodselsdagIMnd: List<String>,
    val formidlingsgruppe: List<Formidlingsgruppe>,
    val servicegruppe: List<Servicegruppe>,
    val rettighetsgruppe: List<Rettighetsgruppe>,
    val veiledere: List<String>,
    val aktiviteter: Map<String, AktivitetFiltervalg>,
    val aktiviteterForenklet: List<String>,
    val tiltakstyper: List<String>,
    val manuellBrukerStatus: List<ManuellBrukerStatus>,
    val navnEllerFnrQuery: String,
    val registreringstype: List<JobbSituasjonBeskrivelse>,
    val utdanning: List<UtdanningSvar>,
    val utdanningBestatt: List<UtdanningBestattSvar>,
    val utdanningGodkjent: List<UtdanningGodkjentSvar>,
    val sisteEndringKategori: String?,
    val ulesteEndringer: String?,
    val cvJobbprofil: CVjobbprofil?,
    val landgruppe: List<String>,
    val foedeland: List<String>,
    val tolkebehov: List<String>,
    val tolkBehovSpraak: List<String>,
    val stillingFraNavFilter: List<StillingFraNAVFilter>,
    val barnUnder18Aar: List<BarnUnder18Aar>,
    val barnUnder18AarAlder: List<String>,
    val geografiskBosted: List<String>,
    val ensligeForsorgere: List<EnsligeForsorgere>,
    val fargekategorier: List<String>,
    val gjeldendeVedtak14a: List<String>,
    val innsatsgruppeGjeldendeVedtak14a: List<Innsatsgruppe>,
    val hovedmalGjeldendeVedtak14a: List<Hovedmal>,
    val ytelseAapArena: List<YtelseAapArena>,
    val ytelseAapKelvin: List<YtelseAapKelvin>,
    val ytelseTiltakspenger: List<YtelseTiltakspenger>,
    val ytelseTiltakspengerArena: List<YtelseTiltakspengerArena>,
    val ytelseDagpenger: List<YtelseDagpenger>,
    val ytelseDagpengerArena: List<YtelseDagpengerArena>
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
                harDinSituasjonSvar() ||
                harUtdanningSvar() ||
                harUtdanningBestattSvar() ||
                harUtdanningGodkjentSvar() ||
                harSisteEndringFilter() ||
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
                harYtelseDagpengerFilter() ||
                harYtelseDagpengerArenaFilter()

    fun harGjeldendeVedtak14aFilter(): Boolean =
        gjeldendeVedtak14a.isNotEmpty()

    fun harInnsatsgruppeGjeldendeVedtak14a(): Boolean =
        innsatsgruppeGjeldendeVedtak14a.isNotEmpty()

    fun harHovedmalGjeldendeVedtak14a(): Boolean =
        hovedmalGjeldendeVedtak14a.isNotEmpty()

    fun harEnsligeForsorgereFilter(): Boolean =
        ensligeForsorgere.isNotEmpty()

    fun harCvFilter(): Boolean =
        cvJobbprofil != null

    fun harFerdigFilter(): Boolean =
        ferdigfilterListe.isNotEmpty()

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

    fun harYtelseDagpengerFilter(): Boolean =
        ytelseDagpenger.isNotEmpty()

    fun harKjonnfilter(): Boolean =
        kjonn != null

    fun harAktiviteterAvansert(): Boolean =
        aktiviteter.values.any {
            it == AktivitetFiltervalg.JA || it == AktivitetFiltervalg.NEI
        }

    fun harAktiviteterForenklet(): Boolean =
        aktiviteterForenklet.isNotEmpty()

    fun harSisteEndringFilter(): Boolean =
        !sisteEndringKategori.isNullOrBlank()

    fun harManuellBrukerStatus(): Boolean =
        manuellBrukerStatus.isNotEmpty()

    fun harNavnEllerFnrQuery(): Boolean =
        navnEllerFnrQuery.isNotBlank()

    fun harUlesteEndringerFilter(): Boolean =
        !ulesteEndringer.isNullOrBlank()

    fun harFoedelandFilter(): Boolean =
        foedeland.isNotEmpty()

    fun harTolkbehovSpraakFilter(): Boolean =
        tolkBehovSpraak.isNotEmpty()

    fun harTalespraaktolkFilter(): Boolean =
        tolkebehov.contains("TALESPRAAKTOLK")

    fun harTegnspraakFilter(): Boolean =
        tolkebehov.contains("TEGNSPRAAKTOLK")

    fun harLandgruppeFilter(): Boolean =
        landgruppe.isNotEmpty()

    fun harFargeKategoriFilter(): Boolean =
        fargekategorier.isNotEmpty()

    fun harStillingFraNavFilter(): Boolean =
        stillingFraNavFilter.isNotEmpty()

    fun harBarnUnder18AarFilter(): Boolean =
        barnUnder18Aar.isNotEmpty() || barnUnder18AarAlder.isNotEmpty()

    fun harBostedFilter(): Boolean =
        geografiskBosted.isNotEmpty()

    fun harDinSituasjonSvar(): Boolean =
        registreringstype.isNotEmpty()

    fun harUtdanningSvar(): Boolean =
        utdanning.isNotEmpty()

    fun harUtdanningBestattSvar(): Boolean =
        utdanningBestatt.isNotEmpty()

    fun harUtdanningGodkjentSvar(): Boolean =
        utdanningGodkjent.isNotEmpty()

}
