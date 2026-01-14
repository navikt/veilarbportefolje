package no.nav.pto.veilarbportefolje.opensearch.domene

import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.Profileringsresultat
import no.nav.pto.veilarbportefolje.domene.EnsligeForsorgereOvergangsstonad
import no.nav.pto.veilarbportefolje.domene.HuskelappForBruker
import no.nav.pto.veilarbportefolje.domene.Statsborgerskap
import no.nav.pto.veilarbportefolje.hendelsesfilter.Hendelse
import no.nav.pto.veilarbportefolje.hendelsesfilter.Kategori
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.GjeldendeVedtak14a
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet
import no.nav.pto.veilarbportefolje.util.DateUtils.getFarInTheFutureDate
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Representerer en person under arbeidsrettet oppfølging, med tilhørende opplysninger, som er relevant i oppfølgingsøyemed.
 * Opplysningene mappes stort 1-til-1 til properties i [no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModell],
 * med unntak av noen properties her som bare brukes i forbindelse med filtrering i spørringer mot OpenSearch.
 *
 * NB: Alle properties her brukes også for å utlede navn på felt ved indeksering av enkeltopplysninger. Dersom det gjøres
 * endringer her, eksempelvis renaming av properties, vil dette kunne påvirke [no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt].
 * Dette er med hensikt; se [DatafeltKeys] for flere detaljer. Skal man ta inn nye opplysninger, og med andre ord legge
 * til nye properties her, bør det legges nye koblinger i [DatafeltKeys], som igjen bør brukes i [no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt]
 * til fordel for å hardkode strengverdier.
 */
data class PortefoljebrukerOpensearchModell(
    // Personalia
    var aktoer_id: String? = null,
    var barn_under_18_aar: List<BarnUnder18AarData>? = emptyList(),
    var bostedSistOppdatert: LocalDate? = null,
    var bydelsnummer: String? = null,
    var diskresjonskode: String? = null,
    var er_doed: Boolean = false,
    var etternavn: String? = null,
    var fnr: String? = null,
    var fodselsdag_i_mnd: Int? = null,
    var fodselsdato: String? = null,
    var foedeland: String? = null,
    var foedelandFulltNavn: String? = null,
    var fornavn: String? = null,
    var fullt_navn: String? = null,
    var harFlereStatsborgerskap: Boolean = false,
    var harUkjentBosted: Boolean = false,
    var hovedStatsborgerskap: Statsborgerskap? = null,
    var kjonn: String? = null,
    var kommunenummer: String? = null,
    var landgruppe: String? = null,
    var sikkerhetstiltak: String? = null,
    var sikkerhetstiltak_beskrivelse: String? = null,
    var sikkerhetstiltak_gyldig_fra: String? = null,
    var sikkerhetstiltak_gyldig_til: String? = null,
    var talespraaktolk: String? = null,
    var tegnspraaktolk: String? = null,
    var tolkBehovSistOppdatert: LocalDate? = null,
    var utenlandskAdresse: String? = null,

    // Oppfølging
    var enhet_id: String? = null,
    var gjeldendeVedtak14a: GjeldendeVedtak14a? = null,
    var hovedmaalkode: String? = null,
    var iserv_fra_dato: String? = null,
    var kvalifiseringsgruppekode: String? = null,
    var manuell_bruker: String? = null,
    var ny_for_veileder: Boolean = false,
    var oppfolging: Boolean = false,
    var oppfolging_startdato: String? = null,
    var tildelt_tidspunkt: LocalDateTime? = null,
    var trenger_vurdering: Boolean = false,
    var utkast_14a_ansvarlig_veileder: String? = null,
    var utkast_14a_status: String? = null,
    var utkast_14a_status_endret: String? = null,
    var veileder_id: String? = null,

    // Arbeidssøker
    var brukers_situasjon_sist_endret: LocalDate? = null,
    var brukers_situasjoner: List<String>? = null,
    var profilering_resultat: Profileringsresultat? = null,
    var utdanning: String? = null,
    var utdanning_bestatt: String? = null,
    var utdanning_godkjent: String? = null,
    var utdanning_og_situasjon_sist_endret: LocalDate? = null,

    // Arbeidsforhold
    var er_sykmeldt_med_arbeidsgiver: Boolean = false,

    // Aktiviteter
    var aktivitet_behandling_utlopsdato: String? = getFarInTheFutureDate(),
    var aktivitet_egen_utlopsdato: String? = getFarInTheFutureDate(),
    var aktivitet_gruppeaktivitet_utlopsdato: String? = getFarInTheFutureDate(),
    var aktivitet_ijobb_utlopsdato: String? = getFarInTheFutureDate(),
    var aktivitet_mote_startdato: String? = getFarInTheFutureDate(),
    var aktivitet_mote_utlopsdato: String? = getFarInTheFutureDate(),
    var aktivitet_sokeavtale_utlopsdato: String? = getFarInTheFutureDate(),
    var aktivitet_start: String? = null,
    var aktivitet_stilling_utlopsdato: String? = getFarInTheFutureDate(),
    var aktivitet_tiltak_utlopsdato: String? = getFarInTheFutureDate(),
    var aktivitet_utdanningaktivitet_utlopsdato: String? = getFarInTheFutureDate(),
    var aktiviteter: Set<String>? = emptySet(),
    var alleAktiviteter: Set<String>? = emptySet(),
    var alle_aktiviteter_behandling_utlopsdato: String? = getFarInTheFutureDate(),
    var alle_aktiviteter_egen_utlopsdato: String? = getFarInTheFutureDate(),
    var alle_aktiviteter_ijobb_utlopsdato: String? = getFarInTheFutureDate(),
    var alle_aktiviteter_mote_startdato: String? = getFarInTheFutureDate(),
    var alle_aktiviteter_mote_utlopsdato: String? = getFarInTheFutureDate(),
    var alle_aktiviteter_sokeavtale_utlopsdato: String? = getFarInTheFutureDate(),
    var alle_aktiviteter_stilling_utlopsdato: String? = getFarInTheFutureDate(),
    var forrige_aktivitet_start: String? = null,
    var neste_aktivitet_start: String? = null,
    var nyesteutlopteaktivitet: String? = null,
    var siste_endringer: Map<String, Endring>? = null,
    var tiltak: Set<String>? = emptySet(),

    // Ytelser
    var aap_kelvin: Boolean = false,
    var aap_kelvin_rettighetstype: AapRettighetstype? = null,
    var aap_kelvin_tom_vedtaksdato: LocalDate? = null,
    var aapmaxtiduke: Int? = null,
    var aapordinerutlopsdato: LocalDate? = null,
    var aapunntakukerigjen: Int? = null,
    var dagputlopuke: Int? = null,
    var enslige_forsorgere_overgangsstonad: EnsligeForsorgereOvergangsstonad? = null,
    var permutlopuke: Int? = null,
    var rettighetsgruppekode: String? = null,
    var tiltakspenger: Boolean = false,
    var tiltakspenger_rettighet: TiltakspengerRettighet? = null,
    var tiltakspenger_vedtaksdato_tom: LocalDate? = null,
    var utlopsdato: String? = null,
    var ytelse: String? = null,

    // Dialog
    var venterpasvarfrabruker: String? = null,
    var venterpasvarfranav: String? = null,

    // Nav ansatt
    var egen_ansatt: Boolean = false,
    var skjermet_til: LocalDateTime? = null,

    // CV
    var cv_eksistere: Boolean = false,
    var har_delt_cv: Boolean = false,
    var neste_cv_kan_deles_status: String? = null,
    var neste_svarfrist_stilling_fra_nav: LocalDate? = null,

    // Annet
    var fargekategori: String? = null,
    var fargekategori_enhetId: String? = null,
    var formidlingsgruppekode: String? = null,
    var huskelapp: HuskelappForBruker? = null,
    var tiltakshendelse: Tiltakshendelse? = null,
    var utgatt_varsel: Hendelse.HendelseInnhold? = null,
    var hendelser: Map<Kategori, Hendelse.HendelseInnhold>? = null
)