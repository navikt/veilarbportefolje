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
    val fnr: String?,
    val aktoerid: String?,
    val fornavn: String?,
    val etternavn: String?,
    val erDoed: Boolean?,
    val barnUnder18AarData: List<BarnUnder18AarData>?,
    val sikkerhetstiltak: List<String>?, //todo, endre til string og ikke liste
    val diskresjonskode: String?,
    val tolkebehov: Tolkebehov?,

    val foedeland: String?,
    val hovedStatsborgerskap: Statsborgerskap?,

    val bostedKommune: String?,
    val bostedBydel: String?,
    val bostedSistOppdatert: LocalDate?,
    val harUtelandsAddresse: Boolean?,
    val harUkjentBosted: Boolean?,

    // 0ppfolgingsdata
    val avvik14aVedtak: Avvik14aVedtak?,
    val gjeldendeVedtak14a: GjeldendeVedtak14a?,
    val oppfolgingStartdato: LocalDateTime?,
    val utkast14a: Utkast14a?,
    val veilederId: String?,
    val nyForVeileder: Boolean?,
    val nyForEnhet: Boolean?,
    val tildeltTidspunkt: LocalDateTime?,

    val trengerOppfolgingsvedtak: Boolean?,
    val vurderingsBehov: VurderingsBehov?, // todo brukes denne? står kommentar at den kan slettes etter vedtakssotte er på lufta

    // Arbeidssokerdata
    val profileringResultat: Profileringsresultat?,
    val utdanningOgSituasjonSistEndret: LocalDate?,

    // ArbeidsforholdData
    val erSykmeldtMedArbeidsgiver: Boolean?,

    // AktiviterData
    val nyesteUtlopteAktivitet: LocalDateTime?,
    val aktivitetStart: LocalDateTime?,
    val nesteAktivitetStart: LocalDateTime?,
    val forrigeAktivitetStart: LocalDateTime?,

    val moteStartTid: LocalDateTime?,
    val alleMoterStartTid: LocalDateTime?,
    val alleMoterSluttTid: LocalDateTime?,

    var aktiviteter: MutableMap<String, Timestamp> = mutableMapOf(),
    var nesteUtlopsdatoAktivitet: LocalDateTime?,
    var sisteEndringKategori: String?,
    var sisteEndringTidspunkt: LocalDateTime?,
    var sisteEndringAktivitetId: String?,

    // YtelseData
    val ytelse: YtelseMapping?, // denne kan slettes når dagpenger blir dratt ut i eget arena filter
    val utlopsdato: LocalDateTime?,
    val dagputlopUke: Int?,
    val permutlopUke: Int?,
    val aapmaxtidUke: Int?,
    val aapUnntakUkerIgjen: Int?,
    val aapordinerutlopsdato: LocalDate?,
    val aapKelvin: AapKelvinForBruker?,
    val tiltakspenger: TiltakspengerForBruker?,
    val ensligeForsorgereOvergangsstonad: EnsligeForsorgereOvergangsstonadFrontend?,


    // DialogData
    val venterPaSvalFraNAV: LocalDateTime?, //todo fikse typo og endre NAV til Nav
    val venterPaSvalFraBruker: LocalDateTime?, //todo fikse typo

    // NavAnasattData
    val egenAnsatt: Boolean?,
    val skjermetTil: LocalDateTime?,

    // CvData
    val nesteSvalfristCvStillingFraNav: LocalDate?, // todo fikse typo


    // AnnetData
    val huskelapp: HuskelappForBruker?,
    val fargekategori: String?,
    val fargekategoriEnhetId: String?,
    val tiltakshendelse: TiltakshendelseForBruker?,
    val utgattvalsel: Hendelse.HendelseInnhold?, // todo lag egen frontendtype her

    val innsatsgruppe: String?,

)


