package no.nav.pto.veilarbportefolje.domene;

import static no.nav.pto.veilarbportefolje.domene.Motedeltaker.skjermetDeltaker;

public record Moteplan (Motedeltaker deltaker, String dato, boolean avtaltMedNav) {
    public static Moteplan skjermMoteplan(Moteplan moteplan){
        return new Moteplan(skjermetDeltaker, moteplan.dato, moteplan.avtaltMedNav);
    }
}
