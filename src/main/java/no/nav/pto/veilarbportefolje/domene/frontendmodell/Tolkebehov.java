package no.nav.pto.veilarbportefolje.domene.frontendmodell;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;

public record Tolkebehov(
        String talespraaktolk,
        String tegnspraaktolk,
        LocalDate sistOppdatert
) {
    public static Tolkebehov of(String talespraaktolk, String tegnspraaktolk, LocalDate sistOppdatert) {
        if(talespraaktolk != null || tegnspraaktolk != null || sistOppdatert != null){
            return new Tolkebehov(talespraaktolk, tegnspraaktolk, sistOppdatert);
        } else {
            return new Tolkebehov("", "", null);
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Tolkebehov {
    }
}
