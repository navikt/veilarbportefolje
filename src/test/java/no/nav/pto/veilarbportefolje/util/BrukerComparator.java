package no.nav.pto.veilarbportefolje.util;

import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModell;

import java.util.Comparator;

/**
 * Samanliknar brukarar på fødselsnummer slik at dei kan sorterast i ei bestemt rekkefølgje.
 * */
public class BrukerComparator implements Comparator<PortefoljebrukerFrontendModell> {
    @Override
    public int compare(PortefoljebrukerFrontendModell bruker1, PortefoljebrukerFrontendModell bruker2) {
        return bruker1.getFnr().toString().compareTo(bruker2.getFnr().toString());
    }
}
