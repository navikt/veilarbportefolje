package no.nav.pto.veilarbportefolje.domene

import no.nav.pto.veilarbportefolje.domene.filtervalg.Brukerstatus
import no.nav.pto.veilarbportefolje.domene.filtervalg.EnsligeForsorgere
import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg
import no.nav.pto.veilarbportefolje.domene.filtervalg.StillingFraNAVFilter

fun getFiltervalgDefaults(): Filtervalg = Filtervalg(
    ferdigfilterListe = emptyList(),
    alder = emptyList(),
    kjonn = null,
    fodselsdagIMnd = emptyList(),
    formidlingsgruppe = emptyList(),
    servicegruppe = emptyList(),
    rettighetsgruppe = emptyList(),
    veiledere = emptyList(),
    aktiviteter = mapOf(),
    tiltakstyper = emptyList(),
    manuellBrukerStatus = emptyList(),
    navnEllerFnrQuery = "",
    registreringstype = emptyList(),
    utdanning = emptyList(),
    utdanningBestatt = emptyList(),
    utdanningGodkjent = emptyList(),
    sisteEndringKategori = null,
    aktiviteterForenklet = emptyList(),
    ulesteEndringer = null,
    cvJobbprofil = null,
    landgruppe = emptyList(),
    foedeland = emptyList(),
    tolkebehov = emptyList(),
    tolkBehovSpraak = emptyList(),
    stillingFraNavFilter = emptyList(),
    barnUnder18Aar = emptyList(),
    barnUnder18AarAlder = emptyList(),
    geografiskBosted = emptyList(),
    ensligeForsorgere = emptyList(),
    fargekategorier = emptyList(),
    gjeldendeVedtak14a = emptyList(),
    innsatsgruppeGjeldendeVedtak14a = emptyList(),
    hovedmalGjeldendeVedtak14a = emptyList(),
    ytelseAapArena = emptyList(),
    ytelseAapKelvin = emptyList(),
    ytelseTiltakspenger = emptyList(),
    ytelseTiltakspengerArena = emptyList(),
    ytelseDagpenger = emptyList(),
    ytelseDagpengerArena = emptyList()
)

@JvmOverloads
fun getFiltervalgAktivteterForJavaTester(
    ferdigfilterListe: List<Brukerstatus> = emptyList(),
    navnEllerFrn: String = "",
    stillingFraNav: List<StillingFraNAVFilter> = emptyList()
): Filtervalg =
    getFiltervalgDefaults().copy(
        ferdigfilterListe = ferdigfilterListe,
        navnEllerFnrQuery = navnEllerFrn,
        stillingFraNavFilter = stillingFraNav
    )

@JvmOverloads
fun getFiltervalgSisteEndringForJavaTester(
    sisteEndringKategori: String? = null,
    ulesteEndringer: String? = null
): Filtervalg =
    getFiltervalgDefaults().copy(
        sisteEndringKategori = sisteEndringKategori,
        ulesteEndringer = ulesteEndringer,
    )

fun getFiltervalgMedTiltakstyperForJavaTester(
    tiltakstyper: List<String>
): Filtervalg =
    getFiltervalgDefaults().copy(
        tiltakstyper = tiltakstyper
    )

fun getFiltervalgMedEnsligeforsorgereForJavaTester(
    ensligeForsorgere: List<EnsligeForsorgere>
): Filtervalg =
    getFiltervalgDefaults().copy(
        ensligeForsorgere = ensligeForsorgere
    )
