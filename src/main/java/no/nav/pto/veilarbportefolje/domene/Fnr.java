package no.nav.pto.veilarbportefolje.domene;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

@Value
@Getter(value = PRIVATE)
public class Fnr {
    String fnr;

    @JsonCreator
    public Fnr(String fnr) {
        if (fnr.matches("\\d{11}")) {
            this.fnr = fnr;
        } else {
            throw new IllegalArgumentException("Fødselsnummeret er ugyldig");
        }
    }

    public String getFnr() {
        return fnr;
    }

    public static Fnr of(String fnr) {
        return new Fnr(fnr);
    }

    @Override
    public String toString() {
        return fnr;
    }
}
