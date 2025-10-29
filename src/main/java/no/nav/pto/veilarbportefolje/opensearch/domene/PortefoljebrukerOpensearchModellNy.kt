package no.nav.pto.veilarbportefolje.opensearch.domene

import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.Profileringsresultat
import no.nav.pto.veilarbportefolje.domene.EnsligeForsorgereOvergangsstonad
import no.nav.pto.veilarbportefolje.domene.HuskelappForBruker
import no.nav.pto.veilarbportefolje.domene.Statsborgerskap
import no.nav.pto.veilarbportefolje.hendelsesfilter.Hendelse
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.avvik14aVedtak.Avvik14aVedtak
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.GjeldendeVedtak14a
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet
import no.nav.pto.veilarbportefolje.util.DateUtils.getFarInTheFutureDate
import java.time.LocalDate
import java.time.LocalDateTime

data class PortefoljebrukerOpensearchModellNy(
    val personaliaData: PersonaliaData,
    val oppfolgingData: OppfolgingData,
    val arbeidssoekerData: ArbeidssoekerData,
    val arbeidsforholdData: ArbeidsforholdData,
    val aktiviteterData: AktiviteterData,
    val ytelseData: YtelseData,
    val dialogData: DialogData,
    val navAnsattData: NavAnsattData,
    val cvData: CvData,
    val annetData: AnnetData,
) {

    data class PersonaliaData(
        val aktoerId: String?,
        val erDoed: Boolean?,
        val etternavn: String?,
        val fnr: String?,
        val fodselsdagIMnd: Int?,
        val fodselsdato: String?,
        val foedeland: String?,
        val foedelandFulltNavn: String?,
        val fornavn: String?,
        val fulltNavn: String?,
        val kjonn: String?,
        val sikkerhetstiltak: String?,
        val sikkerhetstiltakGyldigFra: String?,
        val sikkerhetstiltakGyldigTil: String?,
        val sikkerhetstiltakBeskrivelse: String?,
        val diskresjonskode: String?,
        val talespraaktolk: String?,
        val tegnspraaktolk: String?,
        val tolkBehovSistOppdatert: LocalDate?,
        val landgruppe: String?,
        val harFlereStatsborgerskap: Boolean?,
        val hovedStatsborgerskap: Statsborgerskap?,
        val kommunenummer: String?,
        val bydelsnummer: String?,
        val utenlandskAdresse: String?,
        val harUkjentBosted: Boolean?,
        val bostedSistOppdatert: LocalDate?,
        val barnUnder18Aar: List<BarnUnder18AarData> = emptyList(),
    )

    data class OppfolgingData(
        val avvik14aVedtak: Avvik14aVedtak?,
        val gjeldendeVedtak14a: GjeldendeVedtak14a?,
        val hovedmaalkode: String?,
        val oppfolging: Boolean?,
        val oppfolgingStartdato: String?,
        val utkast14aAnsvarligVeileder: String?,
        val utkast14aStatus: String?,
        val utkast14aStatusEndret: String?,
        val veilederId: String?,
        val nyForVeileder: Boolean?,
        val tildeltTidspunkt: LocalDateTime?,
        val trengerVurdering: Boolean?,
        val manuellBruker: String?,
        val enhetId: String?,
        val iservFraDato: String?,
        val kvalifiseringsgruppeKode: String?,
    )

    data class ArbeidssoekerData(
        val brukersSituasjoner: List<String>?,
        val brukersSituasjonSistEndret: LocalDate?,
        val utdanningOgSituasjonSistEndret: LocalDate?,
        val profileringResultat: Profileringsresultat?,
        val utdanning: String?,
        val utdanningBestatt: String?,
        val utdanningGodkjent: String?,
    )

    data class ArbeidsforholdData(
        val erSykmeldtMedArbeidsgiver: Boolean?,
    )

    data class AktiviteterData(
        val nyesteUtlopteAktivitet: String?,
        val aktivitetStart: String?,
        val nesteAktivitetStart: String?,
        val forrigeAktivitetStart: String?,
        val aktivitetMoteStartdato: String = getFarInTheFutureDate(),
        val aktivitetMoteUtlopsdato: String = getFarInTheFutureDate(),
        val aktivitetStillingUtlopsdato: String = getFarInTheFutureDate(),
        val aktivitetEgenUtlopsdato: String = getFarInTheFutureDate(),
        val aktivitetBehandlingUtlopsdato: String = getFarInTheFutureDate(),
        val aktivitetIjobbUtlopsdato: String = getFarInTheFutureDate(),
        val aktivitetSokeavtaleUtlopsdato: String = getFarInTheFutureDate(),
        val aktivitetTiltakUtlopsdato: String = getFarInTheFutureDate(),
        val aktivitetUtdanningaktivitetUtlopsdato: String = getFarInTheFutureDate(),
        val aktivitetGruppeaktivitetUtlopsdato: String = getFarInTheFutureDate(),
        val alleAktiviteterMoteStartdato: String = getFarInTheFutureDate(),
        val alleAktiviteterMoteUtlopsdato: String = getFarInTheFutureDate(),
        val alleAktiviteterStillingUtlopsdato: String = getFarInTheFutureDate(),
        val alleAktiviteterEgenUtlopsdato: String = getFarInTheFutureDate(),
        val alleAktiviteterBehandlingUtlopsdato: String = getFarInTheFutureDate(),
        val alleAktiviteterIjobbUtlopsdato: String = getFarInTheFutureDate(),
        val alleAktiviteterSokeavtaleUtlopsdato: String = getFarInTheFutureDate(),
        val alleAktiviteter: Set<String> = emptySet(),
        val aktiviteter: Set<String> = emptySet(),
        val sisteEndringer: Map<String, Endring>?,
        val tiltak: Set<String> = emptySet(),
    )

    data class YtelseData(
        val ytelse: String?,
        val utlopsdato: String?,
        val dagputlopuke: Int?,
        val permutlopuke: Int?,
        val aapmaxtiduke: Int?,
        val aapunntakukerigjen: Int?,
        val aapordinerutlopsdato: LocalDate?,
        val aapKelvin: Boolean?,
        val aapKelvinTomVedtaksdato: LocalDate?,
        val aapKelvinRettighetstype: AapRettighetstype?,
        val tiltakspenger: Boolean?,
        val tiltakspengerVedtaksdatoTom: LocalDate?,
        val tiltakspengerRettighet: TiltakspengerRettighet?,
        val ensligeForsorgereOvergangsstonad: EnsligeForsorgereOvergangsstonad?,
        val rettighetsgruppeKode: String?,
    )

    data class DialogData(
        val venterPaSvarFraBruker: String?,
        val venterPaSvarFraNav: String?,
    )

    data class NavAnsattData(
        val egenAnsatt: Boolean?,
        val skjermetTil: LocalDateTime?,
    )

    data class CvData(
        val harDeltCv: Boolean?,
        val cvEksistere: Boolean?,
        val nesteCvKanDelesStatus: String?,
        val nesteSvarfristStillingFraNav: LocalDate?,
    )

    data class AnnetData(
        val formidlingsgruppeKode: String?,
        val huskelapp: HuskelappForBruker?,
        val fargekategori: String?,
        val fargekategoriEnhetId: String?,
        val tiltakshendelse: Tiltakshendelse?,
        val utgattVarsel: Hendelse.HendelseInnhold?,
    )
}
