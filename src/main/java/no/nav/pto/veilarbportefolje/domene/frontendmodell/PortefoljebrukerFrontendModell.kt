package no.nav.pto.veilarbportefolje.domene.frontendmodell

import no.nav.pto.veilarbportefolje.domene.HuskelappForBruker
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime


data class PortefoljebrukerFrontendModell(
    var aktoerid: String? = null, // brukes ikke i frontend, kun i tester

    var etiketter: Etiketter,

    // personaliaData
    var fnr: String? = null,
    var fornavn: String? = null,
    var etternavn: String? = null,
    var hovedStatsborgerskap: StatsborgerskapForBruker?,
    var foedeland: String? = null,
    var geografiskBosted: GeografiskBostedForBruker,
    var tolkebehov: Tolkebehov? = null, // maaange nullsjekker i frontend
    var barnUnder18AarData: List<BarnUnder18AarData>? = null, //kolonne

    // Oppfolgingsdata
    val oppfolgingStartdato: LocalDate? = null,
    val tildeltTidspunkt: LocalDate? = null,
    val veilederId: String? = null,

    // NavAnasattData - skjermet info
    var egenAnsatt: Boolean = false,
    var skjermetTil: LocalDate? = null,

    val tiltakshendelse: TiltakshendelseForBruker? = null,
    var hendelse: HendelseInnhold? = null,
    val meldingerVenterPaSvar: MeldingerVenterPaSvar,

    // AktiviterData
    val nyesteUtlopteAktivitet: LocalDate? = null,
    var nesteUtlopsdatoAktivitet: LocalDateTime? = null,
    var aktiviteter: MutableMap<String, Timestamp> = mutableMapOf(),
    val aktivitetStart: LocalDate? = null,
    val nesteAktivitetStart: LocalDate? = null,
    val forrigeAktivitetStart: LocalDate? = null,

    // filtrert på "møter med nav i dag"
    val moteStartTid: LocalDateTime? = null, // gjør en sjekk på om dato er i dag, og setter "avtalt med nav" hvis true. Er kun for avtalte møter
    val alleMoterStartTid: LocalDateTime? = null, // Førstkommende møte. bruker klokkeslett og regner ut varighet med alleMoterSluttTid. Inkluderer både pliktige og upliktige aktiviteter
    val alleMoterSluttTid: LocalDateTime? = null, // kun for å regne ut varighet på møtet.

    val sisteEndringAvBruker: SisteEndringAvBruker?,
    val utdanningOgSituasjonSistEndret: LocalDate? = null,
    val nesteSvarfristCvStillingFraNav: LocalDate? = null,

    val ytelser: YtelserForBruker,
    val vedtak14a: Vedtak14aForBruker,

    // AnnetData
    var huskelapp: HuskelappForBruker? = null,
    var fargekategori: String? = null,
    val fargekategoriEnhetId: String? = null,
)


