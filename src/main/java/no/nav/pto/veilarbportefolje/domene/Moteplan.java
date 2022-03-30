package no.nav.pto.veilarbportefolje.domene;

import static no.nav.pto.veilarbportefolje.domene.Motedeltaker.skjermetDeltaker;

public record Moteplan (Motedeltaker deltaker, String dato, boolean avtaltMedNav) {
    public Moteplan getSkjermetMoteplan(){
        return new Moteplan(skjermetDeltaker, this.dato, this.avtaltMedNav);
    }
}
