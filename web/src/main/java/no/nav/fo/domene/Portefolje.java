package no.nav.fo.domene;


import java.util.ArrayList;
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

    public List<Bruker> getBrukerFrom(int fra, int antall) {
        if(fra >= brukere.size()) { return new ArrayList<>(); }
        return brukere.subList(fra, fra+antall);
    }

    public void sortByLastName(String sortDirection) {
        if(sortDirection == "ascending") {
            brukere.sort(Bruker.SORT_BY_LAST_NAME_ASCENDING);
        } else if(sortDirection == "descending") {
            brukere.sort(Bruker.SORT_BY_LAST_NAME_DESCENDING);
        }
    }
}
