package no.nav.pto.veilarbportefolje.domene.frontendmodell

import no.nav.pto.veilarbportefolje.domene.HuskelappForBruker
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
    var hovedStatsborgerskap: StatsborgerskapForBruker?,
    var geografiskBosted: GeografiskBostedForBruker,

    val vedtak14a: Vedtak14aForBruker,

    // Oppfolgingsdata
    val oppfolgingStartdato: LocalDate? = null,
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
    var hendelse: HendelseInnhold? = null,

    // siste endringer hendelser
    var sisteEndringKategori: String? = null,
    var sisteEndringTidspunkt: LocalDateTime? = null,
    var sisteEndringAktivitetId: String? = null, // sjekk og oppslagg

    val ytelser: YtelserForBruker,
    val meldingerVenterPaSvar: MeldingerVenterPaSvar,

    // NavAnasattData - skjermet info
    var egenAnsatt: Boolean = false,
    var skjermetTil: LocalDate? = null,

    // AnnetData
    var huskelapp: HuskelappForBruker? = null,
    var fargekategori: String? = null,
    val fargekategoriEnhetId: String? = null,
)


