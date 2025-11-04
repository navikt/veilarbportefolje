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
    // tags
    var etiketter: Etiketter,
    var erDoed: Boolean = false, // kun tag
    val erSykmeldtMedArbeidsgiver: Boolean = false, // kun tag
    val trengerOppfolgingsvedtak: Boolean = false, // kun tag
    val nyForVeileder: Boolean = false, // kun tag
    val nyForEnhet: Boolean = false, //  kun tag
    var diskresjonskode: String? = null, // kun tag
    val profileringResultat: Profileringsresultat? = null, // kun tags når trengerOppfolgingsvedtak er true
    val vurderingsBehov: VurderingsBehov? = null, // todo brukes denne? står kommentar at den kan slettes etter vedtakssotte er på lufta
    // kun tag med sjekk på undergruppen ARBEIDSEVNE_VURDERING


    // personaliaData
    var fnr: String? = null,
    var aktoerid: String? = null,
    var fornavn: String? = null,
    var etternavn: String? = null,
    var barnUnder18AarData: List<BarnUnder18AarData>? = null, //kolonne
    var sikkerhetstiltak: List<String>? = null, //todo, endre til bool og ikke liste, er kun tag

    var tolkebehov: Tolkebehov? = null, // kolonne, maaange nullsjekker osv i frontend

    var foedeland: String? = null, // kolonne
    var hovedStatsborgerskap: Statsborgerskap? = null, // kolonne, gyldig til brukes ikke

        // Geografisk bosted
    var bostedKommune: String? = null,
    var bostedBydel: String? = null,
    var bostedSistOppdatert: LocalDate? = null,
    var harUtelandsAddresse: Boolean = false, // brukes kun til å sette "utland" på kommune
    var harUkjentBosted: Boolean = false, // brukes kun til å sette "ukjent" på kommune

    // Oppfolgingsdata
    val avvik14aVedtak: Avvik14aVedtak? = null, // kolonne
    val gjeldendeVedtak14a: GjeldendeVedtak14a? = null, // kolonne
    val oppfolgingStartdato: LocalDate? = null, // kolonne
    val utkast14a: Utkast14a? = null, // kolonne
    val veilederId: String? = null, // kolonne

    val tildeltTidspunkt: LocalDate? = null, // kolonne

    // Arbeidssokerdata
    val utdanningOgSituasjonSistEndret: LocalDate? = null, // kolonne

    // AktiviterData
    val nyesteUtlopteAktivitet: LocalDate? = null, // kolonne
    val aktivitetStart: LocalDate? = null, // kolonne
    val nesteAktivitetStart: LocalDate? = null, // kolonne
    val forrigeAktivitetStart: LocalDate? = null, // kolonne

    // filtrert på "møter med nav i dag"
    val moteStartTid: LocalDateTime? = null, // gjør en sjekk på om dato er i dag, og setter "avtalt med nav" hvis true. Er kun for avtalte møter
    val alleMoterStartTid: LocalDateTime? = null, // Førstkommende møte. bruker klokkeslett og regner ut varighet med alleMoterSluttTid. Inkluderer både pliktige og upliktige aktiviteter
    val alleMoterSluttTid: LocalDateTime? = null, // kun for å regne ut varighet på møtet.


    var aktiviteter: MutableMap<String, Timestamp> = mutableMapOf(),
    var nesteUtlopsdatoAktivitet: LocalDateTime? = null, // kolonne

    val nesteSvarfristCvStillingFraNav: LocalDate? = null, // kolonne aktivitet
    val tiltakshendelse: TiltakshendelseForBruker? = null,
    val utgattVarsel: Hendelse.HendelseInnhold? = null, // todo lag egen frontendtype her

    // siste endringer hendelser
    var sisteEndringKategori: String? = null, // kolonne
    var sisteEndringTidspunkt: LocalDateTime? = null, // kolonne
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


