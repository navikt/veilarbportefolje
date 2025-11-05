package no.nav.pto.veilarbportefolje.domene.frontendmodell

import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.Profileringsresultat
import no.nav.pto.veilarbportefolje.domene.HuskelappForBruker
import no.nav.pto.veilarbportefolje.domene.Statsborgerskap
import no.nav.pto.veilarbportefolje.domene.YtelseMapping
import no.nav.pto.veilarbportefolje.hendelsesfilter.Hendelse
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.avvik14aVedtak.Avvik14aVedtak
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.GjeldendeVedtak14a
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime


data class PortefoljebrukerFrontendModell(
    var etiketter: Etiketter,

    // personaliaData
    var fnr: String? = null,
    var aktoerid: String? = null,
    var fornavn: String? = null,
    var etternavn: String? = null,
    var barnUnder18AarData: List<BarnUnder18AarData>? = null, //kolonne

    var tolkebehov: Tolkebehov? = null, // maaange nullsjekker i frontend

    var foedeland: String? = null,
    var hovedStatsborgerskap: Statsborgerskap? = null, // gyldig til brukes ikke

        // Geografisk bosted
    var bostedKommune: String? = null,
    var bostedBydel: String? = null,
    var bostedSistOppdatert: LocalDate? = null,
    var harUtelandsAddresse: Boolean = false, // brukes kun til å sette "utland" på kommune
    var harUkjentBosted: Boolean = false, // brukes kun til å sette "ukjent" på kommune

    // Oppfolgingsdata
    val avvik14aVedtak: Avvik14aVedtak? = null,
    val gjeldendeVedtak14a: GjeldendeVedtak14a? = null,
    val oppfolgingStartdato: LocalDate? = null,
    val utkast14a: Utkast14a? = null,
    val veilederId: String? = null,
    val tildeltTidspunkt: LocalDate? = null,

    // Arbeidssokerdata
    val utdanningOgSituasjonSistEndret: LocalDate? = null,

    // AktiviterData
    val nyesteUtlopteAktivitet: LocalDate? = null,
    val aktivitetStart: LocalDate? = null,
    val nesteAktivitetStart: LocalDate? = null,
    val forrigeAktivitetStart: LocalDate? = null,

    // filtrert på "møter med nav i dag"
    val moteStartTid: LocalDateTime? = null, // gjør en sjekk på om dato er i dag, og setter "avtalt med nav" hvis true. Er kun for avtalte møter
    val alleMoterStartTid: LocalDateTime? = null, // Førstkommende møte. bruker klokkeslett og regner ut varighet med alleMoterSluttTid. Inkluderer både pliktige og upliktige aktiviteter
    val alleMoterSluttTid: LocalDateTime? = null, // kun for å regne ut varighet på møtet.

    var aktiviteter: MutableMap<String, Timestamp> = mutableMapOf(),
    var nesteUtlopsdatoAktivitet: LocalDateTime? = null,

    val nesteSvarfristCvStillingFraNav: LocalDate? = null,
    val tiltakshendelse: TiltakshendelseForBruker? = null,
    val utgattVarsel: UtgattVarselForHendelse? = null,

    // siste endringer hendelser
    var sisteEndringKategori: String? = null,
    var sisteEndringTidspunkt: LocalDateTime? = null,
    var sisteEndringAktivitetId: String? = null, // sjekk og oppslagg

    // YtelseData
    val innsatsgruppe: String? = null, // aap arena, sjekker på gruppe BATT
    val ytelse: YtelseMapping? = null,
    val utlopsdato: LocalDateTime? = null, // for aap og tp arena, brukes for uker igjen til utløpsdato
    val dagputlopUke: Int? = null,
    val permutlopUke: Int? = null,
    val aapmaxtidUke: Int? = null,
    val aapUnntakUkerIgjen: Int? = null,
    val aapordinerutlopsdato: LocalDate? = null,
    val aapKelvin: AapKelvinForBruker? = null,
    val tiltakspenger: TiltakspengerForBruker? = null,
    val ensligeForsorgereOvergangsstonad: EnsligeForsorgereOvergangsstonadFrontend? = null,

    // DialogData
    val venterPaSvarFraNAV: LocalDate? = null,
    val venterPaSvarFraBruker: LocalDate? = null,

    // NavAnasattData - skjermet info
    var egenAnsatt: Boolean = false,
    var skjermetTil: LocalDate? = null,

    // AnnetData
    var huskelapp: HuskelappForBruker? = null,
    var fargekategori: String? = null,
    val fargekategoriEnhetId: String? = null,


)


