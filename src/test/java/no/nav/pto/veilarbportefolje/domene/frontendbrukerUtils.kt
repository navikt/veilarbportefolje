package no.nav.pto.veilarbportefolje.domene

import no.nav.pto.veilarbportefolje.domene.frontendmodell.*

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

    tolkebehov = Tolkebehov("", "",null),

    foedeland = null,
    hovedStatsborgerskap = StatsborgerskapForBruker(
        statsborgerskap = null,
        gyldigFra = null
    ),

    geografiskBosted = GeografiskBostedForBruker(
        bostedKommune = null,
        bostedKommuneUkjentEllerUtland = "Utland",
        bostedBydel = null,
        bostedSistOppdatert = null
    ),

    vedtak14a = Vedtak14aForBruker(
        gjeldendeVedtak14a = null,
        utkast14a = null
    ),

    oppfolgingStartdato = null,
    veilederId = null,
    tildeltTidspunkt = null,

    utdanningOgSituasjonSistEndret = null,

    moteMedNavIDag = null,
    moteStartTid = null,
    alleMoterStartTid = null,
    alleMoterSluttTid = null,

    aktiviteterAvtaltMedNav = AktiviteterAvtaltMedNav(
        nesteUtlopsdatoForAlleAktiviteter = null,
        nyesteUtlopteAktivitet = null,
        nesteUtlopsdatoForFiltrerteAktiviteter = null,
        aktivitetStart = null,
        nesteAktivitetStart = null,
        forrigeAktivitetStart = null
    ),

    nesteSvarfristCvStillingFraNav = null,
    tiltakshendelse = null,
    hendelse = null,

    sisteEndringAvBruker = null,

    ytelser = YtelserForBruker(
        ytelserArena = YtelserArena(
            innsatsgruppe = null,
            ytelse = null,
            utlopsdato = null,
            dagputlopUke = null,
            permutlopUke = null,
            aapmaxtidUke = null,
            aapUnntakUkerIgjen = null,
            aapordinerutlopsdato = null
        ),
        aap = null,
        tiltakspenger = null,
        ensligeForsorgereOvergangsstonad = null,
    ),
    meldingerVenterPaSvar = MeldingerVenterPaSvar(
        datoMeldingVenterPaNav = null,
        datoMeldingVenterPaBruker = null
    ),
    egenAnsatt = false,
    skjermetTil = null,

    huskelapp = null,
    fargekategori = null,
    fargekategoriEnhetId = null
)



