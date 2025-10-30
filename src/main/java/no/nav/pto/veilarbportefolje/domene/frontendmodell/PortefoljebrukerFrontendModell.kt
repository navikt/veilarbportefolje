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
    // personaliaData
    var fnr: String? = null,
    var aktoerid: String? = null,
    var fornavn: String? = null,
    var etternavn: String? = null,
    var erDoed: Boolean? = null,
    var barnUnder18AarData: List<BarnUnder18AarData>? = null,
    var sikkerhetstiltak: List<String>? = null, //todo, endre til string og ikke liste
    var diskresjonskode: String? = null,
    var tolkebehov: Tolkebehov? = null,

    var foedeland: String? = null,
    var hovedStatsborgerskap: Statsborgerskap? = null,

    var bostedKommune: String? = null,
    var bostedBydel: String? = null,
    var bostedSistOppdatert: LocalDate? = null,
    var harUtelandsAddresse: Boolean? = null,
    var harUkjentBosted: Boolean? = null,

    // Oppfolgingsdata
    val avvik14aVedtak: Avvik14aVedtak? = null,
    val gjeldendeVedtak14a: GjeldendeVedtak14a? = null,
    val oppfolgingStartdato: LocalDateTime? = null,
    val utkast14a: Utkast14a? = null,
    val veilederId: String? = null,
    val nyForVeileder: Boolean? = null,
    val nyForEnhet: Boolean? = null,
    val tildeltTidspunkt: LocalDateTime? = null,

    val trengerOppfolgingsvedtak: Boolean? = null,
    val vurderingsBehov: VurderingsBehov? = null, // todo brukes denne? står kommentar at den kan slettes etter vedtakssotte er på lufta

    // Arbeidssokerdata
    val profileringResultat: Profileringsresultat? = null,
    val utdanningOgSituasjonSistEndret: LocalDate? = null,

    // ArbeidsforholdData
    val erSykmeldtMedArbeidsgiver: Boolean? = null,

    // AktiviterData
    val nyesteUtlopteAktivitet: LocalDateTime? = null,
    val aktivitetStart: LocalDateTime? = null,
    val nesteAktivitetStart: LocalDateTime? = null,
    val forrigeAktivitetStart: LocalDateTime? = null,

    val moteStartTid: LocalDateTime? = null,
    val alleMoterStartTid: LocalDateTime? = null,
    val alleMoterSluttTid: LocalDateTime? = null,

    var aktiviteter: MutableMap<String, Timestamp> = mutableMapOf(),
    var nesteUtlopsdatoAktivitet: LocalDateTime? = null,
    var sisteEndringKategori: String? = null,
    var sisteEndringTidspunkt: LocalDateTime? = null,
    var sisteEndringAktivitetId: String? = null,

    // YtelseData
    val ytelse: YtelseMapping? = null,
    val utlopsdato: LocalDateTime? = null,
    val dagputlopUke: Int? = null,
    val permutlopUke: Int? = null,
    val aapmaxtidUke: Int? = null,
    val aapUnntakUkerIgjen: Int? = null,
    val aapordinerutlopsdato: LocalDate? = null,
    val aapKelvin: AapKelvinForBruker? = null,
    val tiltakspenger: TiltakspengerForBruker? = null,
    val ensligeForsorgereOvergangsstonad: EnsligeForsorgereOvergangsstonadFrontend? = null,


    // DialogData
    val venterPaSvarFraNAV: LocalDateTime? = null,
    val venterPaSvarFraBruker: LocalDateTime? = null,

    // NavAnasattData
    var egenAnsatt: Boolean? = null,
    var skjermetTil: LocalDateTime? = null,

    // CvData
    val nesteSvarfristCvStillingFraNav: LocalDate? = null,


    // AnnetData
    var huskelapp: HuskelappForBruker? = null,
    var fargekategori: String? = null,
    val fargekategoriEnhetId: String? = null,
    val tiltakshendelse: TiltakshendelseForBruker? = null,
    val utgattVarsel: Hendelse.HendelseInnhold? = null, // todo lag egen frontendtype her

    val innsatsgruppe: String? = null,

    )


