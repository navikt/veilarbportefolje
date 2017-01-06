package no.nav.fo.domene;


import java.util.List;

public class Portefolje {

    private List<Bruker> brukere;

    public Portefolje withBrukere(List<Bruker> brukere) {
        this.brukere = brukere;
        return this;
    }

    public List<Bruker> getBrukere() {
        return brukere;
    }
}
