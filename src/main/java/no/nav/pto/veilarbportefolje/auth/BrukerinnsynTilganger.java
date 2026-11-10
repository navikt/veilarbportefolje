package no.nav.pto.veilarbportefolje.auth;

import no.nav.pto.veilarbportefolje.persononinfo.domene.Adressebeskyttelse;

public record BrukerinnsynTilganger(
        boolean tilgangTilAdressebeskyttelseStrengtFortrolig,
        boolean tilgangTilAdressebeskyttelseFortrolig,
        boolean tilgangTilSkjerming
) {

    public boolean harAlle() {
        return tilgangTilAdressebeskyttelseStrengtFortrolig && tilgangTilAdressebeskyttelseFortrolig && tilgangTilSkjerming;
    }

    public boolean harTilgangTilKonfidensiellInfo(String diskresjonskode, boolean egenAnsatt) {
        return harTilgangTilAdressebeskyttelse(diskresjonskode) && (!egenAnsatt || tilgangTilSkjerming);
    }

    public boolean harTilgangTilAdressebeskyttelse(String diskresjonskode) {
        if (Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode.equals(diskresjonskode)
                || Adressebeskyttelse.STRENGT_FORTROLIG_UTLAND.diskresjonskode.equals(diskresjonskode)) {
            return tilgangTilAdressebeskyttelseStrengtFortrolig;
        }
        if (Adressebeskyttelse.FORTROLIG.diskresjonskode.equals(diskresjonskode)) {
            return tilgangTilAdressebeskyttelseFortrolig;
        }
        return true;
    }
}
