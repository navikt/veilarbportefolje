package no.nav.pto.veilarbportefolje.domene;

public record Motedeltaker(String fornavn, String etternavn, String fnr){
    public static Motedeltaker skjermetDeltaker = new Motedeltaker("", "", "");
}
