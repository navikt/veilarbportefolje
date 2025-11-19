package no.nav.pto.veilarbportefolje.domene

import no.nav.pto.veilarbportefolje.domene.frontendmodell.Etiketter
import no.nav.pto.veilarbportefolje.domene.frontendmodell.GeografiskBostedForBruker
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModell

var frontendbrukerDefaults: PortefoljebrukerFrontendModell = PortefoljebrukerFrontendModell(
    etiketter = Etiketter(
        erDoed = false,
        erSykmeldtMedArbeidsgiver = false,
        trengerOppfolgingsvedtak = false,
        nyForVeileder = false,
        nyForEnhet = false,
        harBehovForArbeidsevneVurdering = false,
        harSikkerhetstiltak = false,
        diskresjonskodeFortrolig = null,
        profileringResultat = null
    ),
    fnr = null,
    aktoerid = null,
    fornavn = null,
    etternavn = null,
    barnUnder18AarData = null,

    tolkebehov = null,

    foedeland = null,
    hovedStatsborgerskap = null,

    geografiskBosted = GeografiskBostedForBruker(
        bostedKommune = null,
        bostedKommuneUkjentEllerUtland = "Utland",
        bostedBydel = null,
        bostedSistOppdatert = null
    ),

    avvik14aVedtak = null,
    gjeldendeVedtak14a = null,
    oppfolgingStartdato = null,
    utkast14a = null,
    veilederId = null,
    tildeltTidspunkt = null,

    utdanningOgSituasjonSistEndret = null,

    nyesteUtlopteAktivitet = null,
    aktivitetStart = null,
    nesteAktivitetStart = null,
    forrigeAktivitetStart = null,

    moteStartTid = null,
    alleMoterStartTid = null,
    alleMoterSluttTid = null,

    aktiviteter = mutableMapOf(),
    nesteUtlopsdatoAktivitet = null,

    nesteSvarfristCvStillingFraNav = null,
    tiltakshendelse = null,
    hendelser = null,

    sisteEndringKategori = null,
    sisteEndringTidspunkt = null,
    sisteEndringAktivitetId = null,

    innsatsgruppe = null,
    ytelse = null,
    utlopsdato = null,
    dagputlopUke = null,
    permutlopUke = null,
    aapmaxtidUke = null,
    aapUnntakUkerIgjen = null,
    aapordinerutlopsdato = null,
    aapKelvin = null,
    tiltakspenger = null,
    ensligeForsorgereOvergangsstonad = null,

    venterPaSvarFraNAV = null,
    venterPaSvarFraBruker = null,

    egenAnsatt = false,
    skjermetTil = null,

    huskelapp = null,
    fargekategori = null,
    fargekategoriEnhetId = null
)



