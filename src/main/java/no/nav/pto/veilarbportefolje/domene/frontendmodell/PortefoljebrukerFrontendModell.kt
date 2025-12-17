package no.nav.pto.veilarbportefolje.domene.frontendmodell

import no.nav.pto.veilarbportefolje.domene.HuskelappForBruker
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData
import java.time.LocalDate
import java.time.LocalDateTime


data class PortefoljebrukerFrontendModell(
    var aktoerid: String?, // brukes ikke i frontend, kun i tester

    var etiketter: Etiketter,

    // personaliaData
    var fnr: String?,
    var fornavn: String?,
    var etternavn: String?,
    var hovedStatsborgerskap: StatsborgerskapForBruker?,
    var foedeland: String?,
    var geografiskBosted: GeografiskBostedForBruker,
    var tolkebehov: Tolkebehov,
    var barnUnder18AarData: List<BarnUnder18AarData>?,

    // Oppfolgingsdata
    val oppfolgingStartdato: LocalDate?,
    val tildeltTidspunkt: LocalDate?,
    val veilederId: String?,

    // NavAnasattData - skjermet info
    var egenAnsatt: Boolean,
    var skjermetTil: LocalDate?,

    val tiltakshendelse: TiltakshendelseForBruker?,
    var hendelse: HendelseInnhold?,
    val meldingerVenterPaSvar: MeldingerVenterPaSvar,

    // AktiviterData
    val aktiviteterAvtaltMedNav: AktiviteterAvtaltMedNav?,

    // filtrert på "møter med nav i dag"
    val moterMedNav: MoterMedNav,

    val moteStartTid: LocalDateTime?, // gjør en sjekk på om dato er i dag, og setter "avtalt med nav" hvis true. Er kun for avtalte møter
    val alleMoterStartTid: LocalDateTime?, // Førstkommende møte. bruker klokkeslett og regner ut varighet med alleMoterSluttTid. Inkluderer både pliktige og upliktige aktiviteter
    val alleMoterSluttTid: LocalDateTime?, // kun for å regne ut varighet på møtet.

    val sisteEndringAvBruker: SisteEndringAvBruker?,
    val utdanningOgSituasjonSistEndret: LocalDate?,
    val nesteSvarfristCvStillingFraNav: LocalDate?,

    val ytelser: YtelserForBruker,
    val vedtak14a: Vedtak14aForBruker,

    // AnnetData
    var huskelapp: HuskelappForBruker?,
    var fargekategori: String?,
    val fargekategoriEnhetId: String?,
)


