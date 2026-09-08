package no.nav.pto.veilarbportefolje.lagredefilter.minefilter

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.common.json.JsonUtils
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.JobbSituasjonBeskrivelse
import no.nav.pto.veilarbportefolje.domene.Kjonn
import no.nav.pto.veilarbportefolje.domene.ManuellBrukerStatus
import no.nav.pto.veilarbportefolje.domene.filtervalg.*
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper

/*
*** Migrerings-guide ****
* For enkle migreringer er det nok å gjøre endringer i koden, og så migrere i gcloud ved hjelp av ferdig definerte scripts.
* For mer komplekse migreringer bør man ha mappinger som et mellomsteg til man har migreret over til det nye formatet:
*
* ENDRINGER:
* For endringer i key/filternavn, må man legge til @JsonAlias("gameltNavn") over filternavnet i AktiveFiltervalg.
* For endringer i enums må man legge til @JsonAlias("GAMMEL_VERDI") eller over enumen i data classen.
* Legge til en test som sjekker at alt blir riktig.
*
* SLETTING AV VERDIER / HELE FILTRE:
* Oppdater databasen først. Så fjern filteret eller enumen fra koden.
*
* SPLITTING/ANDRE KOMPLEKSE TING:
* 1. I AktiveFiltervalg, endre classen til å kunne deserializere "gammle" filtre i databasen.
* Kan f eks være å endre typen fra en enum til en string (eller list<String>) så den kan deserialiseres.
* 2. Lag en mapper-funksjon som tar inn filteret i rekonstruerFiltervalgFraAktive.
* Lag custom logikk for det du ønsker å endre.
* 3. ekstraherAktiveFiltervalg -> mapper om nødvendig så riktig verdi blir sendt inn til AktiveFiltervalg
* 4. Lag tester for å verifiser at alt blir riktig.
* 5. RYDD OPP etter migreringen er ferdig.
*/

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class AktiveFiltervalg(
    val ferdigfilterListe: List<Brukerstatus> = emptyList(),
    val alder: List<String> = emptyList(),
    val kjonn: Kjonn? = null,
    val fodselsdagIMnd: List<String> = emptyList(),
    val formidlingsgruppe: List<Formidlingsgruppe> = emptyList(),
    val servicegruppe: List<Servicegruppe> = emptyList(),
    val rettighetsgruppe: List<Rettighetsgruppe> = emptyList(),
    val veiledere: List<String> = emptyList(),
    val aktiviteter: Map<String, AktivitetFiltervalg> = emptyMap(),
    val aktiviteterForenklet: List<String> = emptyList(),
    val tiltakstyper: List<String> = emptyList(),
    val manuellBrukerStatus: List<ManuellBrukerStatus> = emptyList(),
    val navnEllerFnrQuery: String = "",
    val registreringstype: List<JobbSituasjonBeskrivelse> = emptyList(),
    val utdanning: List<UtdanningSvar> = emptyList(),
    val utdanningBestatt: List<UtdanningBestattSvar> = emptyList(),
    val utdanningGodkjent: List<UtdanningGodkjentSvar> = emptyList(),
    val sisteEndringKategori: String? = null,
    val ulesteEndringer: String? = null,
    val cvJobbprofil: CVjobbprofil? = null,
    val landgruppe: List<String> = emptyList(),
    val foedeland: List<String> = emptyList(),
    val tolkebehov: List<String> = emptyList(),
    val tolkBehovSpraak: List<String> = emptyList(),
    val stillingFraNavFilter: List<StillingFraNAVFilter> = emptyList(),
    val barnUnder18Aar: List<BarnUnder18Aar> = emptyList(),
    val barnUnder18AarAlder: List<String> = emptyList(),
    val geografiskBosted: List<String> = emptyList(),
    val ensligeForsorgere: List<EnsligeForsorgere> = emptyList(),
    val fargekategorier: List<String> = emptyList(),
    val gjeldendeVedtak14a: List<String> = emptyList(),
    val innsatsgruppeGjeldendeVedtak14a: List<Innsatsgruppe> = emptyList(),
    val hovedmalGjeldendeVedtak14a: List<Hovedmal> = emptyList(),
    val ytelseAapArena: List<YtelseAapArena> = emptyList(),
    val ytelseAapKelvin: List<YtelseAapKelvin> = emptyList(),
    val ytelseTiltakspenger: List<YtelseTiltakspenger> = emptyList(),
    val ytelseTiltakspengerArena: List<YtelseTiltakspengerArena> = emptyList(),
    val ytelseDagpenger: List<YtelseDagpenger> = emptyList(),
    val ytelseDagpengerArena: List<YtelseDagpengerArena> = emptyList(),
    val ytelseUngdomsprogram: List<YtelseUngdomsprogram> = emptyList(),
    val visGeografiskBosted: List<String> = emptyList()
)

fun ekstraherAktiveFiltervalg(filtervalg: Filtervalg): AktiveFiltervalg =
    AktiveFiltervalg(
        ferdigfilterListe = filtervalg.ferdigfilterListe,
        alder = filtervalg.alder,
        kjonn = filtervalg.kjonn,
        fodselsdagIMnd = filtervalg.fodselsdagIMnd,
        formidlingsgruppe = filtervalg.formidlingsgruppe,
        servicegruppe = filtervalg.servicegruppe,
        rettighetsgruppe = filtervalg.rettighetsgruppe,
        veiledere = filtervalg.veiledere,
        aktiviteter = if (filtervalg.harAktiviteterAvansert()) filtervalg.aktiviteter else emptyMap(),
        aktiviteterForenklet = filtervalg.aktiviteterForenklet,
        tiltakstyper = filtervalg.tiltakstyper,
        manuellBrukerStatus = filtervalg.manuellBrukerStatus,
        navnEllerFnrQuery = filtervalg.navnEllerFnrQuery,
        registreringstype = filtervalg.registreringstype,
        utdanning = filtervalg.utdanning,
        utdanningBestatt = filtervalg.utdanningBestatt,
        utdanningGodkjent = filtervalg.utdanningGodkjent,
        sisteEndringKategori = filtervalg.sisteEndringKategori,
        ulesteEndringer = filtervalg.ulesteEndringer,
        cvJobbprofil = filtervalg.cvJobbprofil,
        landgruppe = filtervalg.landgruppe,
        foedeland = filtervalg.foedeland,
        tolkebehov = filtervalg.tolkebehov,
        tolkBehovSpraak = filtervalg.tolkBehovSpraak,
        stillingFraNavFilter = filtervalg.stillingFraNavFilter,
        barnUnder18Aar = filtervalg.barnUnder18Aar,
        barnUnder18AarAlder = filtervalg.barnUnder18AarAlder,
        geografiskBosted = filtervalg.geografiskBosted,
        ensligeForsorgere = filtervalg.ensligeForsorgere,
        fargekategorier = filtervalg.fargekategorier,
        gjeldendeVedtak14a = filtervalg.gjeldendeVedtak14a,
        innsatsgruppeGjeldendeVedtak14a = filtervalg.innsatsgruppeGjeldendeVedtak14a,
        hovedmalGjeldendeVedtak14a = filtervalg.hovedmalGjeldendeVedtak14a,
        ytelseAapArena = filtervalg.ytelseAapArena,
        ytelseAapKelvin = filtervalg.ytelseAapKelvin,
        ytelseTiltakspenger = filtervalg.ytelseTiltakspenger,
        ytelseTiltakspengerArena = filtervalg.ytelseTiltakspengerArena,
        ytelseDagpenger = filtervalg.ytelseDagpenger,
        ytelseDagpengerArena = filtervalg.ytelseDagpengerArena,
        ytelseUngdomsprogram = filtervalg.ytelseUngdomsprogram,
        visGeografiskBosted = filtervalg.visGeografiskBosted
    )

fun rekonstruerFiltervalgFraAktive(aktive: AktiveFiltervalg): Filtervalg =
    Filtervalg(
        ferdigfilterListe = aktive.ferdigfilterListe,
        alder = aktive.alder,
        kjonn = aktive.kjonn,
        fodselsdagIMnd = aktive.fodselsdagIMnd,
        formidlingsgruppe = aktive.formidlingsgruppe,
        servicegruppe = aktive.servicegruppe,
        rettighetsgruppe = aktive.rettighetsgruppe,
        veiledere = aktive.veiledere,
        aktiviteter = aktive.aktiviteter,
        aktiviteterForenklet = aktive.aktiviteterForenklet,
        tiltakstyper = aktive.tiltakstyper,
        manuellBrukerStatus = aktive.manuellBrukerStatus,
        navnEllerFnrQuery = aktive.navnEllerFnrQuery,
        registreringstype = aktive.registreringstype,
        utdanning = aktive.utdanning,
        utdanningBestatt = aktive.utdanningBestatt,
        utdanningGodkjent = aktive.utdanningGodkjent,
        sisteEndringKategori = aktive.sisteEndringKategori,
        ulesteEndringer = aktive.ulesteEndringer,
        cvJobbprofil = aktive.cvJobbprofil,
        landgruppe = aktive.landgruppe,
        foedeland = aktive.foedeland,
        tolkebehov = aktive.tolkebehov,
        tolkBehovSpraak = aktive.tolkBehovSpraak,
        stillingFraNavFilter = aktive.stillingFraNavFilter,
        barnUnder18Aar = aktive.barnUnder18Aar,
        barnUnder18AarAlder = aktive.barnUnder18AarAlder,
        geografiskBosted = aktive.geografiskBosted,
        ensligeForsorgere = aktive.ensligeForsorgere,
        fargekategorier = aktive.fargekategorier,
        gjeldendeVedtak14a = aktive.gjeldendeVedtak14a,
        innsatsgruppeGjeldendeVedtak14a = aktive.innsatsgruppeGjeldendeVedtak14a,
        hovedmalGjeldendeVedtak14a = aktive.hovedmalGjeldendeVedtak14a,
        ytelseAapArena = aktive.ytelseAapArena,
        ytelseAapKelvin = aktive.ytelseAapKelvin,
        ytelseTiltakspenger = aktive.ytelseTiltakspenger,
        ytelseTiltakspengerArena = aktive.ytelseTiltakspengerArena,
        ytelseDagpenger = aktive.ytelseDagpenger,
        ytelseDagpengerArena = aktive.ytelseDagpengerArena,
        ytelseUngdomsprogram = aktive.ytelseUngdomsprogram,
        visGeografiskBosted = aktive.visGeografiskBosted
    )


class FiltervalgRekonstruksjonException() :
    RuntimeException()

/**
 * Deserialiserer og rekonstruerer et [Filtervalg] fra lagret JSON.
 * Kaster [FiltervalgRekonstruksjonException] ved ugyldig enum-verdi, umappbar nøkkel
 * eller annen deserialiserings-/rekonstruksjonsfeil.
 */

// Må castes til JsonMapper for å få tilgang på rebuild, usikket hvorfor.
private val strictAktiveFiltervalgMapper = (JsonUtils.getMapper() as JsonMapper)
    .rebuild()
    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build()

fun rekonstruerFiltervalgFraJson(filterId: Int, aktiveFiltervalgJson: String?): Filtervalg {
    val aktive = try {
        strictAktiveFiltervalgMapper.readValue(aktiveFiltervalgJson, AktiveFiltervalg::class.java)
    } catch (e: Exception) {
        secureLog.error(
            "Kunne ikke rekonstruere filter (filterId=${filterId}) så hopper over lagret filter. " +
                    "Årsaken: Klarte ikke å deserialisere lagret AktiveFiltervalg-JSON",
            e
        )
        throw FiltervalgRekonstruksjonException()
    }
    return try {
        rekonstruerFiltervalgFraAktive(aktive)
    } catch (e: Exception) {
        secureLog.error(
            "Kunne ikke rekonstruere filter (filterId=${filterId}) så hopper over lagret filter. " +
                    "Årsaken: Klarte ikke å rekonstruere Filtervalg fra AktiveFiltervalg",
            e
        )
        throw FiltervalgRekonstruksjonException()
    }
}
