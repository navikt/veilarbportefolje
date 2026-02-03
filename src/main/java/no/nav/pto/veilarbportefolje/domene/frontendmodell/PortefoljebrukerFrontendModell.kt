package no.nav.pto.veilarbportefolje.domene.frontendmodell

import no.nav.pto.veilarbportefolje.domene.HuskelappForBruker
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData
import java.time.LocalDate


data class PortefoljebrukerFrontendModell(
    var aktoerid: String?, // brukes ikke i frontend, kun i tester

    var etiketter: Etiketter,

    var fnr: String?,
    var fornavn: String?,
    var etternavn: String?,
    var hovedStatsborgerskap: StatsborgerskapForBruker?,
    var foedeland: String?,
    var geografiskBosted: GeografiskBostedForBruker,
    var tolkebehov: Tolkebehov,
    var barnUnder18AarData: List<BarnUnder18AarData>?,

    val oppfolgingStartdato: LocalDate?,
    val tildeltTidspunkt: LocalDate?,
    val veilederId: String?,

    var egenAnsatt: Boolean,
    var skjermetTil: LocalDate?,

    val tiltakshendelse: TiltakshendelseForBruker?,
    var hendelse: HendelseInnhold?,
    val meldingerVenterPaSvar: MeldingerVenterPaSvar,

    val aktiviteterAvtaltMedNav: AktiviteterAvtaltMedNav?,
    val moteMedNavIDag: MoteMedNavIDag?,

    val sisteEndringAvBruker: SisteEndringAvBruker?,
    val utdanningOgSituasjonSistEndret: LocalDate?,
    val nesteSvarfristCvStillingFraNav: LocalDate?,

    val ytelser: YtelserForBruker,
    val vedtak14a: Vedtak14aForBruker,

    var huskelapp: HuskelappForBruker?,
    var fargekategori: String?,
    val fargekategoriEnhetId: String?,
)


