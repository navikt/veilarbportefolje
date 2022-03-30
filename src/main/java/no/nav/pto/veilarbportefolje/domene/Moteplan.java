package no.nav.pto.veilarbportefolje.domene;

public record Moteplan (Motedeltaker deltaker, String dato, boolean avtaltMedNav) {
    public Moteplan getSkjermetMoteplan(){
        return new Moteplan(new Motedeltaker("", "", null), this.dato, this.avtaltMedNav);
    }
}
