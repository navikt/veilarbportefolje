package no.nav.pto.veilarbportefolje.domene;

public enum Brukerstatus {
    TRENGER_OPPFOLGINGSVEDTAK,
    UFORDELTE_BRUKERE,
    INAKTIVE_BRUKERE,
    VENTER_PA_SVAR_FRA_NAV,
    VENTER_PA_SVAR_FRA_BRUKER,
    UTLOPTE_AKTIVITETER,
    IKKE_I_AVTALT_AKTIVITET,
    I_AVTALT_AKTIVITET,
    I_AKTIVITET,
    MIN_ARBEIDSLISTE, // OpenSearch lyttar ikkje på dette filteret (returnerer alle resultata), men vi må ha det i Brukerstatus så lenge det finst i lagra filter i veilarbfilter, elles krasjer kallet mot portefolje om nokon bruker eit filter som har MIN_ARBEIDSLISTE lagra.
    NYE_BRUKERE_FOR_VEILEDER,
    ER_SYKMELDT_MED_ARBEIDSGIVER,
    MOTER_IDAG,
    UNDER_VURDERING,
    MINE_HUSKELAPPER,
    TILTAKSHENDELSER,
    UTGATTE_VARSEL
}
