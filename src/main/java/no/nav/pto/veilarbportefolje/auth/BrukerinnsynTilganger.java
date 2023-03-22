package no.nav.pto.veilarbportefolje.auth;

public record BrukerinnsynTilganger(
        boolean tilgangTilAdressebeskyttelseStrengtFortrolig,
        boolean tilgangTilAdressebeskyttelseFortrolig,
        boolean tilgangTilSkjerming
) {

    public boolean harAlle() {
        return tilgangTilAdressebeskyttelseStrengtFortrolig && tilgangTilAdressebeskyttelseFortrolig && tilgangTilSkjerming;
    }
}
