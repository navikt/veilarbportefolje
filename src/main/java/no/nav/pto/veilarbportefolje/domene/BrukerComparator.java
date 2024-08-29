package no.nav.pto.veilarbportefolje.domene;

import java.util.Comparator;

/**
 * Samanliknar brukarar på fødselsnummer slik at dei kan sorterast i ei bestemt rekkefølgje.
 * */
public class BrukerComparator implements Comparator<Bruker> {
    @Override
    public int compare(Bruker bruker1, Bruker bruker2) {
        return bruker1.getFnr().toString().compareTo(bruker2.getFnr().toString());
    }
}
