package no.nav.fo.provider.rest;

import no.nav.fo.domene.Bruker;

import java.util.List;

class PortefoljeViewModel {

    public final String enhet;
    public final int antallTotalt;
    public final int antallReturnert;
    public final int fraIndex;
    public final List<Bruker> brukere;

    PortefoljeViewModel(String enhet, List<Bruker> brukere, int antallTotalt, int fraIndex) {
        this.enhet = enhet;
        this.brukere = brukere;
        this.antallReturnert = brukere.size();
        this.antallTotalt = antallTotalt;
        this.fraIndex = fraIndex;
    }
}
