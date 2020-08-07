package no.nav.pto.veilarbportefolje.util;

import no.nav.pto.veilarbportefolje.domene.Bruker;
import no.nav.pto.veilarbportefolje.domene.Portefolje;

import java.util.List;

public class PortefoljeUtils {

    public static Portefolje buildPortefolje(int antall, List<Bruker> brukereSublist, String enhet, int fra) {

        return new Portefolje()
                .setEnhet(enhet)
                .setBrukere(brukereSublist)
                .setAntallTotalt(antall)
                .setAntallReturnert(brukereSublist.size())
                .setFraIndex(fra);
    }
}
