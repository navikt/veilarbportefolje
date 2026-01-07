package no.nav.pto.veilarbportefolje.opensearch

import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.JobbSituasjonBeskrivelse
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.Profileringsresultat
import no.nav.pto.veilarbportefolje.domene.EnsligeForsorgereOvergangsstonad
import no.nav.pto.veilarbportefolje.domene.HuskelappForBruker
import no.nav.pto.veilarbportefolje.domene.Statsborgerskap
import no.nav.pto.veilarbportefolje.hendelsesfilter.Hendelse
import no.nav.pto.veilarbportefolje.opensearch.OpensearchConfig.BRUKERINDEKS_ALIAS
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys
import no.nav.pto.veilarbportefolje.opensearch.domene.Endring
import no.nav.pto.veilarbportefolje.opensearch.domene.OpensearchResponse
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.GjeldendeVedtak14a
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData
import no.nav.pto.veilarbportefolje.persononinfo.domene.Adressebeskyttelse
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakstype
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.OpensearchTestClient.pollOpensearchUntil
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.opensearch.search.builder.SearchSourceBuilder
import org.springframework.beans.factory.annotation.Autowired
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import kotlin.reflect.full.declaredMemberProperties

class OpensearchServiceSerderAlleFelterIntTest(
    @param:Autowired val opensearchService: OpensearchService,
) : EndToEndTest() {

    @Test
    fun `alle felter i PortefoljebrukerOpensearchModell skal indekseres og mappes korrekt tilbake til PortefoljebrukerOpensearchModell`() {
        // Given
        opensearchTestClient.createUserInOpensearch(portefoljebrukerOpensearchModell)
        pollOpensearchUntil { opensearchTestClient.countDocuments() > 0 }

        // When
        val opensearchResponse =
            opensearchService.search(SearchSourceBuilder(), BRUKERINDEKS_ALIAS, OpensearchResponse::class.java)

        // Then
        assertThat(opensearchResponse.hits.hits.first()._source)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(portefoljebrukerOpensearchModell)
    }

    @Test
    fun `temp`() {
        val alleFeltNavn = DatafeltKeys::class.declaredMemberProperties.map { it.name }
    }

    companion object Testdata {
        val portefoljebrukerOpensearchModell = PortefoljebrukerOpensearchModell(
            // Personalia
            aktoer_id = PortefoljebrukerOpensearchModell.AKTOR_ID,
            barn_under_18_aar = PortefoljebrukerOpensearchModell.BARN_UNDER_18_AAR,
            bostedSistOppdatert = PortefoljebrukerOpensearchModell.BOSTED_SIST_OPPDATERT,
            bydelsnummer = PortefoljebrukerOpensearchModell.BYDELSNUMMER,
            diskresjonskode = PortefoljebrukerOpensearchModell.DISKRESJONSKODE,
            er_doed = PortefoljebrukerOpensearchModell.ER_DOED,
            etternavn = PortefoljebrukerOpensearchModell.ETTERNAVN,
            fnr = PortefoljebrukerOpensearchModell.FNR,
            fodselsdag_i_mnd = PortefoljebrukerOpensearchModell.FODSELSDAG_I_MND,
            fodselsdato = PortefoljebrukerOpensearchModell.FODSELSDATO,
            foedeland = PortefoljebrukerOpensearchModell.FOEDELAND,
            foedelandFulltNavn = PortefoljebrukerOpensearchModell.FOEDELAND_FULLT_NAVN,
            fornavn = PortefoljebrukerOpensearchModell.FORNAVN,
            fullt_navn = PortefoljebrukerOpensearchModell.FULLT_NAVN,
            harFlereStatsborgerskap = PortefoljebrukerOpensearchModell.HAR_FLERE_STATSBORGERSKAP,
            harUkjentBosted = PortefoljebrukerOpensearchModell.HAR_UKJENT_BOSTED,
            hovedStatsborgerskap = PortefoljebrukerOpensearchModell.HOVED_STATSBORGERSKAP,
            kjonn = PortefoljebrukerOpensearchModell.KJONN,
            kommunenummer = PortefoljebrukerOpensearchModell.KOMMUNENUMMER,
            landgruppe = PortefoljebrukerOpensearchModell.LANDGRUPPE,
            sikkerhetstiltak = PortefoljebrukerOpensearchModell.SIKKERHETSTILTAK,
            sikkerhetstiltak_beskrivelse = PortefoljebrukerOpensearchModell.SIKKERHETSTILTAK_BESKRIVELSE,
            sikkerhetstiltak_gyldig_fra = PortefoljebrukerOpensearchModell.SIKKERHETSTILTAK_GYLDIG_FRA,
            sikkerhetstiltak_gyldig_til = PortefoljebrukerOpensearchModell.SIKKERHETSTILTAK_GYLDIG_TIL,
            talespraaktolk = PortefoljebrukerOpensearchModell.TALESPRAAK_TOLK,
            tegnspraaktolk = PortefoljebrukerOpensearchModell.TEGNSPRAAK_TOLK,
            tolkBehovSistOppdatert = PortefoljebrukerOpensearchModell.TOLKBEHOV_SIST_OPPDATERT,
            utenlandskAdresse = PortefoljebrukerOpensearchModell.UTENLANDSK_ADRESSE,

            // Oppfølging
            enhet_id = PortefoljebrukerOpensearchModell.ENHET_ID,
            gjeldendeVedtak14a = PortefoljebrukerOpensearchModell.GJELDENDE_VEDTAK_14A,
            hovedmaalkode = PortefoljebrukerOpensearchModell.HOVEDMAAL_KODE,
            iserv_fra_dato = PortefoljebrukerOpensearchModell.ISERV_FRA_DATO,
            kvalifiseringsgruppekode = PortefoljebrukerOpensearchModell.KVALIFISERINGSGRUPPE_KODE,
            manuell_bruker = PortefoljebrukerOpensearchModell.MANUELL_BRUKER,
            ny_for_veileder = PortefoljebrukerOpensearchModell.NY_FOR_VEILEDER,
            oppfolging = PortefoljebrukerOpensearchModell.OPPFOLGING,
            oppfolging_startdato = PortefoljebrukerOpensearchModell.OPPFOLGING_STARTDATO,
            tildelt_tidspunkt = PortefoljebrukerOpensearchModell.TILDELT_TIDSPUNKT,
            trenger_vurdering = PortefoljebrukerOpensearchModell.TRENGER_VURDERING,
            utkast_14a_ansvarlig_veileder = PortefoljebrukerOpensearchModell.UTKAST_14_A_ANSVARLIG_VEILEDER,
            utkast_14a_status = PortefoljebrukerOpensearchModell.UTKAST_14_A_STATUS,
            utkast_14a_status_endret = PortefoljebrukerOpensearchModell.UTKAST_14_A_STATUS_ENDRET,
            veileder_id = PortefoljebrukerOpensearchModell.VEILEDER_ID,

            // Arbeidssøker
            brukers_situasjon_sist_endret = PortefoljebrukerOpensearchModell.BRUKERS_SITUASJON_SIST_ENDRET,
            brukers_situasjoner = PortefoljebrukerOpensearchModell.BRUKERS_SITUASJONER,
            profilering_resultat = PortefoljebrukerOpensearchModell.PROFILERING_RESULTAT,
            utdanning = PortefoljebrukerOpensearchModell.UTDANNING,
            utdanning_bestatt = PortefoljebrukerOpensearchModell.UTDANNING_BESTATT,
            utdanning_godkjent = PortefoljebrukerOpensearchModell.UTDANNING_GODKJENT,
            utdanning_og_situasjon_sist_endret = PortefoljebrukerOpensearchModell.UTDANNING_OG_SITUASJON_SIST_ENDRET,

            // Arbeidsforhold
            er_sykmeldt_med_arbeidsgiver = PortefoljebrukerOpensearchModell.ER_SYKMELDT_MED_ARBEIDSGIVER,

            // Aktiviteter
            aktivitet_behandling_utlopsdato = PortefoljebrukerOpensearchModell.AKTIVITET_BEHANDLING_UTLOPSDATO,
            aktivitet_egen_utlopsdato = PortefoljebrukerOpensearchModell.AKTIVITET_EGEN_UTLOPSDATO,
            aktivitet_gruppeaktivitet_utlopsdato = PortefoljebrukerOpensearchModell.AKTIVITET_GRUPPEAKTIVITET_UTLOPSDATO,
            aktivitet_ijobb_utlopsdato = PortefoljebrukerOpensearchModell.AKTIVITET_I_JOBB_UTLOPSDATO,
            aktivitet_mote_startdato = PortefoljebrukerOpensearchModell.AKTIVITET_MOTE_STARTDATO,
            aktivitet_mote_utlopsdato = PortefoljebrukerOpensearchModell.AKTIVITET_MOTE_UTLOPSDATO,
            aktivitet_sokeavtale_utlopsdato = PortefoljebrukerOpensearchModell.AKTIVITET_SOKE_AVTALE_UTLOPSDATO,
            aktivitet_start = PortefoljebrukerOpensearchModell.AKTIVITET_START,
            aktivitet_stilling_utlopsdato = PortefoljebrukerOpensearchModell.AKTIVITET_STILLING_UTLOPSDATO,
            aktivitet_tiltak_utlopsdato = PortefoljebrukerOpensearchModell.AKTIVITET_TILTAK_UTLOPSDATO,
            aktivitet_utdanningaktivitet_utlopsdato = PortefoljebrukerOpensearchModell.AKTIVITET_UTDANNINGAKTIVITET_UTLOPSDATO,
            aktiviteter = PortefoljebrukerOpensearchModell.AKTIVITETER,
            alleAktiviteter = PortefoljebrukerOpensearchModell.ALLE_AKTIVITETER,
            alle_aktiviteter_behandling_utlopsdato = PortefoljebrukerOpensearchModell.ALLE_AKTIVITETER_BEHANDLING_UTLOPSDATO,
            alle_aktiviteter_egen_utlopsdato = PortefoljebrukerOpensearchModell.ALLE_AKTIVITETER_EGEN_UTLOPSDATO,
            alle_aktiviteter_ijobb_utlopsdato = PortefoljebrukerOpensearchModell.ALLE_AKTIVITETER_IJOBB_UTLOPSDATO,
            alle_aktiviteter_mote_startdato = PortefoljebrukerOpensearchModell.ALLE_AKTIVITETER_MOTE_STARTDATO,
            alle_aktiviteter_mote_utlopsdato = PortefoljebrukerOpensearchModell.ALLE_AKTIVITETER_MOTE_UTLOPSDATO,
            alle_aktiviteter_sokeavtale_utlopsdato = PortefoljebrukerOpensearchModell.ALLE_AKTIVITETER_SOKE_AVTALE_UTLOPSDATO,
            alle_aktiviteter_stilling_utlopsdato = PortefoljebrukerOpensearchModell.ALLE_AKTIVITETER_STILLING_UTLOPSDATO,
            forrige_aktivitet_start = PortefoljebrukerOpensearchModell.FORRIGE_AKTIVITET_START,
            neste_aktivitet_start = PortefoljebrukerOpensearchModell.NESTE_AKTIVITET_START,
            nyesteutlopteaktivitet = PortefoljebrukerOpensearchModell.NYESTE_UTLOPTE_AKTIVITET,
            siste_endringer = PortefoljebrukerOpensearchModell.SISTE_ENDRINGER,
            tiltak = PortefoljebrukerOpensearchModell.TILTAK,

            // Ytelser
            aap_kelvin = PortefoljebrukerOpensearchModell.AAP_KELVIN,
            aap_kelvin_rettighetstype = PortefoljebrukerOpensearchModell.AAP_KELVIN_RETTIGHETSTYPE,
            aap_kelvin_tom_vedtaksdato = PortefoljebrukerOpensearchModell.AAP_KELVIN_TOM_VEDTAKSDATO,
            aapmaxtiduke = PortefoljebrukerOpensearchModell.AAP_MAXTID_UKE,
            aapordinerutlopsdato = PortefoljebrukerOpensearchModell.AAP_ORDINER_UTLOPSDATO,
            aapunntakukerigjen = PortefoljebrukerOpensearchModell.AAP_UNNTAK_UKER_IGJEN,
            dagputlopuke = PortefoljebrukerOpensearchModell.DAGP_UTLOP_UKE,
            enslige_forsorgere_overgangsstonad = PortefoljebrukerOpensearchModell.ENSLIGE_FORSORGERE_OVERGANGSSTONAD,
            permutlopuke = PortefoljebrukerOpensearchModell.PERM_UTLOP_UKE,
            rettighetsgruppekode = PortefoljebrukerOpensearchModell.RETTIGHETSGRUPPE_KODE,
            tiltakspenger = PortefoljebrukerOpensearchModell.TILTAKSPENGER,
            tiltakspenger_rettighet = PortefoljebrukerOpensearchModell.TILTAKSPENGER_RETTIGHET,
            tiltakspenger_vedtaksdato_tom = PortefoljebrukerOpensearchModell.TILTAKSPENGER_VEDTAKSDATO_TOM,
            utlopsdato = PortefoljebrukerOpensearchModell.UTLOPSDATO,
            ytelse = PortefoljebrukerOpensearchModell.YTELSE,

            // Dialog
            venterpasvarfrabruker = PortefoljebrukerOpensearchModell.VENTER_PA_SVAR_FRA_BRUKER,
            venterpasvarfranav = PortefoljebrukerOpensearchModell.VENTER_PA_SVAR_FRA_NAV,

            // Nav ansatt
            egen_ansatt = PortefoljebrukerOpensearchModell.EGEN_ANSATT,
            skjermet_til = PortefoljebrukerOpensearchModell.SKJERMET_TIL,

            // CV
            cv_eksistere = PortefoljebrukerOpensearchModell.CV_EKSISTERE,
            har_delt_cv = PortefoljebrukerOpensearchModell.HAR_DELT_CV,
            neste_cv_kan_deles_status = PortefoljebrukerOpensearchModell.NESTE_CV_KAN_DELES_STATUS,
            neste_svarfrist_stilling_fra_nav = PortefoljebrukerOpensearchModell.NESTE_SVARFRIST_STILLING_FRA_NAV,

            // Annet
            fargekategori = PortefoljebrukerOpensearchModell.FARGEKATEGORI,
            fargekategori_enhetId = PortefoljebrukerOpensearchModell.FARGEKATEGORI_ENHET_ID,
            formidlingsgruppekode = PortefoljebrukerOpensearchModell.FORMIDLINGSGRUPPE_KODE,
            huskelapp = PortefoljebrukerOpensearchModell.HUSKELAPP,
            tiltakshendelse = PortefoljebrukerOpensearchModell.TILTAKSHENDELSE,
            utgatt_varsel = PortefoljebrukerOpensearchModell.UTGATT_VARSEL,
        )

        object PortefoljebrukerOpensearchModell {
            // Personalia
            val AKTOR_ID: String = "1111111111111"
            val BARN_UNDER_18_AAR: List<BarnUnder18AarData> =
                listOf(BarnUnder18AarData(12, Adressebeskyttelse.UGRADERT.diskresjonskode))
            val BOSTED_SIST_OPPDATERT: LocalDate = LocalDate.parse("2025-11-06")
            val BYDELSNUMMER: String = "030101"
            val DISKRESJONSKODE: String = "6"
            val ER_DOED: Boolean = false
            val ETTERNAVN: String = "Nordmann"
            val FNR: String = "11111111111"
            val FODSELSDAG_I_MND: Int = 1
            val FODSELSDATO: String = "2025-01-01"
            val FOEDELAND: String = "NOR"
            val FOEDELAND_FULLT_NAVN: String = "NORGE"
            val FORNAVN: String = "Ola"
            val FULLT_NAVN: String = "Ola Nordmann"
            val HAR_FLERE_STATSBORGERSKAP: Boolean = false
            val HAR_UKJENT_BOSTED: Boolean = false
            val HOVED_STATSBORGERSKAP: Statsborgerskap = Statsborgerskap("NORGE", LocalDate.parse("1974-10-04"), null)
            val KJONN: String = "K"
            val KOMMUNENUMMER: String = "3324"
            val LANDGRUPPE: String = "3"
            val SIKKERHETSTILTAK: String = "TFUS"
            val SIKKERHETSTILTAK_BESKRIVELSE: String = "Telefonisk utestengelse"
            val SIKKERHETSTILTAK_GYLDIG_FRA: String = "2025-11-07"
            val SIKKERHETSTILTAK_GYLDIG_TIL: String = "2025-12-07"
            val TALESPRAAK_TOLK: String = "NN"
            val TEGNSPRAAK_TOLK: String = "EN"
            val TOLKBEHOV_SIST_OPPDATERT: LocalDate = LocalDate.parse("2025-11-07")
            val UTENLANDSK_ADRESSE: String = "FIN"

            // Oppfølging
            val ENHET_ID: String = "0914"
            val GJELDENDE_VEDTAK_14A: GjeldendeVedtak14a = GjeldendeVedtak14a(
                Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                Hovedmal.BEHOLDE_ARBEID,
                ZonedDateTime.parse("2025-11-07T12:00:00.000000+01:00")
            )
            val HOVEDMAAL_KODE: String = "BEHOLDEA"
            val ISERV_FRA_DATO: String = "2025-11-10T00:00:00.000000Z"
            val KVALIFISERINGSGRUPPE_KODE: String = "BFORM"
            val MANUELL_BRUKER: String = "MANUELL"
            val NY_FOR_VEILEDER: Boolean = true
            val OPPFOLGING: Boolean = true
            val OPPFOLGING_STARTDATO: String = "2025-10-10T00:00:00.000000Z"
            val TILDELT_TIDSPUNKT: LocalDateTime = LocalDateTime.parse("2025-10-10T10:00:00.000000")
            val TRENGER_VURDERING: Boolean = true
            val UTKAST_14_A_ANSVARLIG_VEILEDER: String = "Z999988"
            val UTKAST_14_A_STATUS: String = "UTKAST_OPPRETTET"
            val UTKAST_14_A_STATUS_ENDRET: String = "2025-10-11T12:00:00.000000Z"
            val VEILEDER_ID: String = "Z999999"

            // Arbeidssøker
            val BRUKERS_SITUASJON_SIST_ENDRET = LocalDate.parse("2025-10-10")
            val BRUKERS_SITUASJONER: List<String> = listOf(JobbSituasjonBeskrivelse.MIDLERTIDIG_JOBB.name)
            val PROFILERING_RESULTAT: Profileringsresultat = Profileringsresultat.ANTATT_GODE_MULIGHETER
            val UTDANNING: String = "VIDEREGAENDE_FAGBREV_SVENNEBREV"
            val UTDANNING_BESTATT: String = "JA"
            val UTDANNING_GODKJENT: String = "JA"
            val UTDANNING_OG_SITUASJON_SIST_ENDRET: LocalDate = LocalDate.parse("2024-07-18")

            // Arbeidsforhold
            val ER_SYKMELDT_MED_ARBEIDSGIVER: Boolean = false

            // Aktiviteter
            val AKTIVITET_BEHANDLING_UTLOPSDATO: String = "2025-10-10T10:00:00.000Z"
            val AKTIVITET_EGEN_UTLOPSDATO: String = "2025-10-10T10:00:00.000Z"
            val AKTIVITET_GRUPPEAKTIVITET_UTLOPSDATO: String = "2025-10-10T10:00:00.000Z"
            val AKTIVITET_I_JOBB_UTLOPSDATO: String = "2025-10-10T10:00:00.000Z"
            val AKTIVITET_MOTE_STARTDATO: String = "2025-10-10T10:00:00.000Z"
            val AKTIVITET_MOTE_UTLOPSDATO: String = "2025-10-10T10:00:00.000Z"
            val AKTIVITET_SOKE_AVTALE_UTLOPSDATO: String = "2025-10-10T10:00:00.000Z"
            val AKTIVITET_START: String = "2025-10-10T10:00:00.000Z"
            val AKTIVITET_STILLING_UTLOPSDATO: String = "2025-10-10T10:00:00.000Z"
            val AKTIVITET_TILTAK_UTLOPSDATO: String = "2025-10-10T10:00:00.000Z"
            val AKTIVITET_UTDANNINGAKTIVITET_UTLOPSDATO: String = "2025-10-10T10:00:00.000Z"
            val AKTIVITETER: Set<String> = setOf(
                "egen",
                "stilling",
                "stilling_fra_nav",
                "sokeavtale",
                "behandling",
                "ijobb",
                "mote",
                "tiltak",
                "gruppeaktivitet",
                "utdanningaktivitet",
            )
            val ALLE_AKTIVITETER: Set<String> = setOf(
                "egen",
                "stilling",
                "stilling_fra_nav",
                "sokeavtale",
                "behandling",
                "ijobb",
                "mote",
                "tiltak",
                "gruppeaktivitet",
                "utdanningaktivitet",
            )
            val ALLE_AKTIVITETER_BEHANDLING_UTLOPSDATO: String = "2025-10-10T10:00:00.000Z"
            val ALLE_AKTIVITETER_EGEN_UTLOPSDATO: String = "2025-10-10T10:00:00.000Z"
            val ALLE_AKTIVITETER_IJOBB_UTLOPSDATO: String = "2025-10-10T10:00:00.000Z"
            val ALLE_AKTIVITETER_MOTE_STARTDATO: String = "2025-10-10T10:00:00.000Z"
            val ALLE_AKTIVITETER_MOTE_UTLOPSDATO: String = "2025-10-10T10:00:00.000Z"
            val ALLE_AKTIVITETER_SOKE_AVTALE_UTLOPSDATO: String = "2025-10-10T10:00:00.000Z"
            val ALLE_AKTIVITETER_STILLING_UTLOPSDATO: String = "2025-10-10T10:00:00.000Z"
            val FORRIGE_AKTIVITET_START: String = "2025-10-10T10:00:00.000Z"
            val NESTE_AKTIVITET_START: String = "2025-10-10T10:00:00.000Z"
            val NYESTE_UTLOPTE_AKTIVITET: String = "2025-10-10T10:00:00.000Z"
            val SISTE_ENDRINGER: Map<String, Endring> = mapOf(
                "NY_STILLING" to Endring(
                    aktivtetId = "111110",
                    tidspunkt = "2025-10-10T10:00:00.000Z",
                    erSett = "N"
                ),
                "NY_EGEN" to Endring(
                    aktivtetId = "111111",
                    tidspunkt = "2025-10-10T10:00:00.000Z",
                    erSett = "N"
                ),
                "AVBRUTT_STILLING" to Endring(
                    aktivtetId = "111112",
                    tidspunkt = "2025-10-10T10:00:00.000Z",
                    erSett = "N"
                ),
                "FULLFORT_BEHANDLING" to Endring(
                    aktivtetId = "111113",
                    tidspunkt = "2025-10-10T10:00:00.000Z",
                    erSett = "N"
                ),
                "AVBRUTT_SOKEAVTALE" to Endring(
                    aktivtetId = "111114",
                    tidspunkt = "2025-10-10T10:00:00.000Z",
                    erSett = "N"
                ),
                "NY_IJOBB" to Endring(
                    aktivtetId = "111115",
                    tidspunkt = "2025-10-10T10:00:00.000Z",
                    erSett = "N"
                ),
                "AVBRUTT_IJOBB" to Endring(
                    aktivtetId = "111116",
                    tidspunkt = "2025-10-10T10:00:00.000Z",
                    erSett = "N"
                ),
                "FULLFORT_SOKEAVTALE" to Endring(
                    aktivtetId = "111117",
                    tidspunkt = "2025-10-10T10:00:00.000Z",
                    erSett = "N"
                ),
                "FULLFORT_IJOBB" to Endring(
                    aktivtetId = "111118",
                    tidspunkt = "2025-10-10T10:00:00.000Z",
                    erSett = "N"
                ),
                "FULLFORT_EGEN" to Endring(
                    aktivtetId = "111119",
                    tidspunkt = "2025-10-10T10:00:00.000Z",
                    erSett = "N"
                ),
                "FULLFORT_STILLING" to Endring(
                    aktivtetId = "111120",
                    tidspunkt = "2025-10-10T10:00:00.000Z",
                    erSett = "N"
                )
            )
            val TILTAK: Set<String> = setOf(
                "EKSPEBIST",
                "ENKELAMO",
                "ENKFAGYRKE",
                "GRUPPEAMO",
                "MIDLONTIL",
                "VARLONTIL",
                "VATIAROR",
            )

            // Ytelser
            val AAP_KELVIN: Boolean = true
            val AAP_KELVIN_RETTIGHETSTYPE: AapRettighetstype = AapRettighetstype.BISTANDSBEHOV
            val AAP_KELVIN_TOM_VEDTAKSDATO: LocalDate = LocalDate.parse("2028-01-01")
            val AAP_MAXTID_UKE: Int = 50
            val AAP_ORDINER_UTLOPSDATO: LocalDate = LocalDate.parse("2025-12-17")
            val AAP_UNNTAK_UKER_IGJEN: Int = 10
            val DAGP_UTLOP_UKE: Int = 100
            val ENSLIGE_FORSORGERE_OVERGANGSSTONAD: EnsligeForsorgereOvergangsstonad = EnsligeForsorgereOvergangsstonad(
                "Hovedperiode",
                false,
                LocalDate.parse("2025-02-10"),
                LocalDate.parse("2020-10-10")
            )
            val PERM_UTLOP_UKE: Int = 20
            val RETTIGHETSGRUPPE_KODE: String = "IYT"
            val TILTAKSPENGER: Boolean = true
            val TILTAKSPENGER_RETTIGHET: TiltakspengerRettighet = TiltakspengerRettighet.TILTAKSPENGER_OG_BARNETILLEGG
            val TILTAKSPENGER_VEDTAKSDATO_TOM: LocalDate = LocalDate.parse("2025-10-01")
            val UTLOPSDATO: String = "2025-10-10T10:00:00.000Z"
            val YTELSE: String = "DAGPENGER_MED_PERMITTERING"

            // Dialog
            val VENTER_PA_SVAR_FRA_BRUKER: String = "2025-10-10T10:00:00.000Z"
            val VENTER_PA_SVAR_FRA_NAV: String = "2025-10-10T10:00:00.000Z"

            // Nav ansatt
            val EGEN_ANSATT: Boolean = true
            val SKJERMET_TIL: LocalDateTime = LocalDateTime.parse("2020-01-01T10:10:10.000")

            // CV
            val CV_EKSISTERE: Boolean = true
            val HAR_DELT_CV: Boolean = true
            val NESTE_CV_KAN_DELES_STATUS: String = "JA"
            val NESTE_SVARFRIST_STILLING_FRA_NAV: LocalDate = LocalDate.parse("2023-01-01")

            // Annet
            val FARGEKATEGORI: String = "FARGEKATEGORI_D"
            val FARGEKATEGORI_ENHET_ID: String = "0220"
            val FORMIDLINGSGRUPPE_KODE: String = "ARBS"
            val HUSKELAPP = HuskelappForBruker(
                LocalDate.parse("2025-10-10"),
                "En kommentar",
                LocalDate.parse("2025-11-01"),
                "Z999999",
                "10d3abe4-9c23-4d0a-a676-0bd17128b028",
                "0220"
            )
            val TILTAKSHENDELSE: Tiltakshendelse = Tiltakshendelse(
                UUID.fromString("cffaf928-bdeb-4994-88b7-d3f3ae194470"),
                LocalDateTime.parse("2024-09-01T12:00:00.000"),
                "Utkast til påmelding",
                "/arbeidsmarkedstiltak/deltakelse/deltaker/cffaf928-bdeb-4994-88b7-d3f3ae194470",
                Tiltakstype.ARBFORB,
                Fnr.of("11111111111")
            )
            val UTGATT_VARSEL: Hendelse.HendelseInnhold = Hendelse.HendelseInnhold(
                beskrivelse = "Bruker har et utgått varsel",
                dato = ZonedDateTime.parse("2025-07-01T13:37:00.000+02:00"),
                lenke = URI.create("https://veilarbpersonflate.ansatt.dev.nav.no/aktivitetsplan").toURL(),
                detaljer = null
            )
        }
    }
}